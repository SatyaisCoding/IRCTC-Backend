package com.irctc.user.service;

import com.irctc.user.dto.ChangePasswordRequest;
import com.irctc.user.dto.UserResponse;

public interface UserService {
    UserResponse getUserProfile(Long id);
    UserResponse updateUserProfile(Long id, String fullName, String email);
    void changePassword(Long id, ChangePasswordRequest request);
    void deleteProfile(Long id);
}
