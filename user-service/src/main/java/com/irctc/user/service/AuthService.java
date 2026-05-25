package com.irctc.user.service;

import com.irctc.user.dto.*;

public interface AuthService {
    UserResponse registerUser(UserRegisterRequest request);
    void verifyOtp(VerifyOtpRequest request);
    UserLoginResponse loginUser(UserLoginRequest request);
    UserLoginResponse refreshTokens(RefreshTokenRequest request);
    void logout(String accessToken, String refreshToken);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
    UserLoginResponse googleOauthLogin(String email, String fullName);
}
