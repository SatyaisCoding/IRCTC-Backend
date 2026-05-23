package com.irctc.user.service.impl;

import com.irctc.user.dto.UserLoginRequest;
import com.irctc.user.dto.UserLoginResponse;
import com.irctc.user.dto.UserRegisterRequest;
import com.irctc.user.dto.UserResponse;
import com.irctc.user.entity.User;
import com.irctc.user.exception.UserException;
import com.irctc.user.repository.UserRepository;
import com.irctc.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserResponse registerUser(UserRegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new UserException("Username is already taken", "USERNAME_ALREADY_EXISTS");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new UserException("Email is already registered", "EMAIL_ALREADY_EXISTS");
        }

        // NOTE: In production, password should be hashed (e.g., using BCryptPasswordEncoder)
        User user = User.builder()
                .username(request.getUsername())
                .password(request.getPassword()) 
                .email(request.getEmail())
                .fullName(request.getFullName())
                .role("USER")
                .build();

        User savedUser = userRepository.save(user);
        return mapToResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserLoginResponse loginUser(UserLoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UserException("Invalid username or password", "INVALID_CREDENTIALS"));

        // NOTE: Standard plain-text password match for boilerplate/mock
        if (!user.getPassword().equals(request.getPassword())) {
            throw new UserException("Invalid username or password", "INVALID_CREDENTIALS");
        }

        // Mock JWT generation for readiness
        String mockToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." + 
                           Base64UrlEncode("{\"sub\":\"" + user.getUsername() + "\",\"role\":\"" + user.getRole() + "\"}") + 
                           ".MockSignatureValue";

        return UserLoginResponse.builder()
                .token(mockToken)
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserException("User not found with Username: " + username, "USER_NOT_FOUND"));
        return mapToResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserException("User not found with ID: " + id, "USER_NOT_FOUND"));
        return mapToResponse(user);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private String Base64UrlEncode(String input) {
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(input.getBytes());
    }
}
