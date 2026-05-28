package com.irctc.user.service.impl;

import com.irctc.user.dto.*;
import com.irctc.user.entity.RefreshToken;
import com.irctc.user.entity.User;
import com.irctc.user.exception.UserException;
import com.irctc.user.mapper.UserMapper;
import com.irctc.user.repository.RefreshTokenRepository;
import com.irctc.user.repository.UserRepository;
import com.irctc.user.security.JwtTokenProvider;
import com.irctc.user.security.UserPrincipal;
import com.irctc.user.kafka.KafkaProducerService;
import com.irctc.user.service.UserCacheService;
import com.irctc.user.kafka.event.OtpNotificationEvent;
import com.irctc.user.kafka.event.WelcomeNotificationEvent;
import com.irctc.user.service.AuthService;
import com.irctc.user.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpService otpService;
    private final KafkaProducerService kafkaProducerService;
    private final UserMapper userMapper;
    private final UserCacheService userCacheService;

    @Value("${app.jwt.refreshTokenExpirationMs}")
    private long refreshTokenExpirationMs;

    @Override
    @Transactional
    public UserResponse registerUser(UserRegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserException("Username is already taken", "USERNAME_ALREADY_EXISTS", HttpStatus.BAD_REQUEST);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserException("Email is already registered", "EMAIL_ALREADY_EXISTS", HttpStatus.BAD_REQUEST);
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        User savedUser = userRepository.save(user);

        // Generate and send Verification OTP asynchronously via Kafka
        String otp = otpService.generateOtp(savedUser.getEmail());
        kafkaProducerService.sendOtpNotification(
                new OtpNotificationEvent(savedUser.getEmail(), otp, "EMAIL_VERIFICATION")
        );

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    public void verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserException("User not found with email: " + request.getEmail(), "USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (user.isVerified()) {
            throw new UserException("Email is already verified", "ALREADY_VERIFIED", HttpStatus.BAD_REQUEST);
        }

        boolean isValid = otpService.validateOtp(request.getEmail(), request.getOtpCode());
        if (!isValid) {
            throw new UserException("Invalid or expired OTP code", "INVALID_OTP", HttpStatus.UNAUTHORIZED);
        }

        user.setVerified(true);
        User savedUser = userRepository.save(user);

        // Send Welcome email asynchronously via Kafka upon successful verification
        kafkaProducerService.sendWelcomeNotification(
                new WelcomeNotificationEvent(savedUser.getEmail(), savedUser.getFullName())
        );
    }

    @Override
    @Transactional
    public UserLoginResponse loginUser(UserLoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UserException("Invalid username or password", "INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED));

        if (!user.isVerified()) {
            // Re-send verification OTP asynchronously via Kafka
            String otp = otpService.generateOtp(user.getEmail());
            kafkaProducerService.sendOtpNotification(
                    new OtpNotificationEvent(user.getEmail(), otp, "EMAIL_VERIFICATION")
            );
            throw new UserException("Email address is not verified. A fresh OTP has been sent to your email.", "EMAIL_NOT_VERIFIED", HttpStatus.FORBIDDEN);
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String accessToken = tokenProvider.generateToken(authentication);
        
        // Generate new Refresh Token
        String refreshTokenString = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenString)
                .expiryDate(LocalDateTime.now().plusWeeks(1))
                .isRevoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        UserResponse userResponse = userMapper.toResponse(user);

        // Warm Redis cache on login — first GET /profile will be a cache HIT
        userCacheService.put(userResponse);
        log.info("[Auth] User [{}] logged in — profile cached in Redis (TTL 24h)", user.getUsername());

        return UserLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenString)
                .tokenType("Bearer")
                .user(userResponse)
                .build();
    }

    @Override
    @Transactional
    public UserLoginResponse refreshTokens(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new UserException("Invalid refresh token", "INVALID_REFRESH_TOKEN", HttpStatus.UNAUTHORIZED));

        User user = refreshToken.getUser();

        // Replay attack protection: If token is already revoked, it suggests token theft!
        if (refreshToken.isRevoked()) {
            log.warn("Replay attack detected! Revoking all refresh tokens for User: {}", user.getUsername());
            refreshTokenRepository.deleteByUser(user); // Force logout everywhere
            throw new UserException("Potential security breach. Please login again.", "SECURITY_BREACH", HttpStatus.UNAUTHORIZED);
        }

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new UserException("Refresh token expired. Please login again.", "TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED);
        }

        // ROTATION: Delete old, generate brand new refresh token
        refreshTokenRepository.delete(refreshToken);

        String newRefreshTokenString = UUID.randomUUID().toString();
        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(user)
                .token(newRefreshTokenString)
                .expiryDate(LocalDateTime.now().plusWeeks(1))
                .isRevoked(false)
                .build();
        refreshTokenRepository.save(newRefreshToken);

        // Generate new access token
        String newAccessToken = tokenProvider.generateTokenFromUsername(
                user.getUsername(),
                user.getRole(),
                user.getId()
        );

        return UserLoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenString)
                .tokenType("Bearer")
                .user(userMapper.toResponse(user))
                .build();
    }

    @Override
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        // 1. Blacklist access token
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            String jwt = accessToken.substring(7);
            tokenProvider.blacklistToken(jwt);
        }

        // 2. Delete refresh token from DB
        if (refreshToken != null) {
            refreshTokenRepository.findByToken(refreshToken)
                    .ifPresent(refreshTokenRepository::delete);
        }
        
        SecurityContextHolder.clearContext();
        log.info("User logged out successfully");
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserException("User not found with email: " + request.getEmail(), "USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        // Generate and send Password Reset OTP asynchronously via Kafka
        String otp = otpService.generateOtp(user.getEmail());
        kafkaProducerService.sendOtpNotification(
                new OtpNotificationEvent(user.getEmail(), otp, "PASSWORD_RESET")
        );
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserException("User not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        boolean isValid = otpService.validateOtp(request.getEmail(), request.getOtpCode());
        if (!isValid) {
            throw new UserException("Invalid or expired OTP code", "INVALID_OTP", HttpStatus.UNAUTHORIZED);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Force revoke old refresh tokens so they must login with new password everywhere
        refreshTokenRepository.deleteByUser(user);
        log.info("Password reset successful for user: {}", user.getUsername());
    }

    @Override
    @Transactional
    public UserLoginResponse googleOauthLogin(String email, String fullName) {
        // Track if this is a new user registration
        boolean[] isNewUser = {false};

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            isNewUser[0] = true;
            // Auto register Google users
            String generatedUsername = "google_" + email.split("@")[0] + "_" + UUID.randomUUID().toString().substring(0, 4);
            User newUser = User.builder()
                    .username(generatedUsername)
                    .password(passwordEncoder.encode("google-oauth-password-placeholder"))
                    .email(email)
                    .fullName(fullName)
                    .role("USER")
                    .isVerified(true) // Google emails are pre-verified
                    .build();
            return userRepository.save(newUser);
        });

        // If this is a brand new Google user, send a welcome email asynchronously via Kafka
        if (isNewUser[0]) {
            kafkaProducerService.sendWelcomeNotification(
                    new WelcomeNotificationEvent(email, fullName)
            );
            log.info("New Google user [{}] registered. Welcome event published to Kafka.", email);
        }

        String accessToken = tokenProvider.generateTokenFromUsername(
                user.getUsername(),
                user.getRole(),
                user.getId()
        );

        String refreshTokenString = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenString)
                .expiryDate(LocalDateTime.now().plusWeeks(1))
                .isRevoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return UserLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenString)
                .tokenType("Bearer")
                .user(userMapper.toResponse(user))
                .build();
    }
}
