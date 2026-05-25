package com.irctc.user.controller;

import com.irctc.user.dto.*;
import com.irctc.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> registerUser(@Valid @RequestBody UserRegisterRequest request) {
        UserResponse response = authService.registerUser(request);
        return new ResponseEntity<>(
                ApiResponse.success("Registration successful. Please verify your email using the OTP sent.", response),
                HttpStatus.CREATED
        );
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<String>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Email address verified successfully. You can now login."));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserLoginResponse>> loginUser(@Valid @RequestBody UserLoginRequest request) {
        UserLoginResponse response = authService.loginUser(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<UserLoginResponse>> refreshTokens(@Valid @RequestBody RefreshTokenRequest request) {
        UserLoginResponse response = authService.refreshTokens(request);
        return ResponseEntity.ok(ApiResponse.success("Tokens refreshed successfully", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestHeader("Authorization") String accessToken,
            @Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(accessToken, request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Reset password OTP code sent to your email."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully. You can now login."));
    }

    @PostMapping("/oauth2/google")
    public ResponseEntity<ApiResponse<UserLoginResponse>> googleOauthLogin(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String fullName = payload.get("fullName");
        
        if (email == null || fullName == null) {
            return new ResponseEntity<>(
                    ApiResponse.error("Invalid Google OAuth parameters"),
                    HttpStatus.BAD_REQUEST
            );
        }
        
        UserLoginResponse response = authService.googleOauthLogin(email, fullName);
        return ResponseEntity.ok(ApiResponse.success("Google Login successful", response));
    }
}
