package com.irctc.user.mapper;

import com.irctc.user.dto.UserRegisterRequest;
import com.irctc.user.dto.UserResponse;
import com.irctc.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .isVerified(user.isVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public User toEntity(UserRegisterRequest request) {
        if (request == null) {
            return null;
        }

        return User.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .role("USER") // Default role
                .isVerified(false) // Verified via OTP later
                .build();
    }
}
