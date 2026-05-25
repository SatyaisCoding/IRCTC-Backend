package com.irctc.user.service.impl;

import com.irctc.user.entity.OtpVerification;
import com.irctc.user.repository.OtpVerificationRepository;
import com.irctc.user.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpService {

    private final StringRedisTemplate redisTemplate;
    private final OtpVerificationRepository otpRepository;
    private final Random random = new Random();

    @Override
    @Transactional
    public String generateOtp(String email) {
        String otp = String.format("%06d", random.nextInt(900000) + 100000); // Guarantees 6 digits
        log.info("Generated OTP for {}", email);

        // 1. Try to save in Redis
        try {
            redisTemplate.opsForValue().set(
                    "otp:" + email,
                    otp,
                    5,
                    TimeUnit.MINUTES
            );
            log.info("Saved OTP for {} to Redis", email);
        } catch (Exception ex) {
            log.warn("Redis is not available. Saving OTP to Database: {}", ex.getMessage());
        }

        // 2. Always write to DB for audit trail / fallback resilience
        otpRepository.deleteByEmail(email); // Clean old ones
        OtpVerification verification = OtpVerification.builder()
                .email(email)
                .otpCode(otp)
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .build();
        otpRepository.save(verification);

        return otp;
    }

    @Override
    @Transactional
    public boolean validateOtp(String email, String code) {
        // 1. Try to read from Redis
        try {
            String cachedOtp = redisTemplate.opsForValue().get("otp:" + email);
            if (cachedOtp != null) {
                if (cachedOtp.equals(code)) {
                    redisTemplate.delete("otp:" + email);
                    otpRepository.deleteByEmail(email); // Sync DB
                    return true;
                }
                return false;
            }
        } catch (Exception ex) {
            log.warn("Redis lookup failed, falling back to database check: {}", ex.getMessage());
        }

        // 2. Fallback DB check
        return otpRepository.findFirstByEmailOrderByExpiryTimeDesc(email)
                .map(verification -> {
                    if (!verification.isExpired() && verification.getOtpCode().equals(code)) {
                        otpRepository.deleteByEmail(email);
                        return true;
                    }
                    return false;
                }).orElse(false);
    }
}
