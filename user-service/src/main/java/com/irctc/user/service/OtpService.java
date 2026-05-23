package com.irctc.user.service;

public interface OtpService {
    String generateOtp(String email);
    boolean validateOtp(String email, String code);
}
