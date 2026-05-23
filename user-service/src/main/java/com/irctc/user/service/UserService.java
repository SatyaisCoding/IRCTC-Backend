package com.irctc.user.service;

import com.irctc.user.dto.UserLoginRequest;
import com.irctc.user.dto.UserLoginResponse;
import com.irctc.user.dto.UserRegisterRequest;
import com.irctc.user.dto.UserResponse;

public interface UserService {
    UserResponse registerUser(UserRegisterRequest request);
    UserLoginResponse loginUser(UserLoginRequest request);
    UserResponse getUserByUsername(String username);
    UserResponse getUserById(Long id);
}
