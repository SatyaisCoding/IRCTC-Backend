package com.irctc.user.service.impl;

import com.irctc.user.dto.ChangePasswordRequest;
import com.irctc.user.dto.UserResponse;
import com.irctc.user.entity.User;
import com.irctc.user.exception.UserException;
import com.irctc.user.mapper.UserMapper;
import com.irctc.user.repository.UserRepository;
import com.irctc.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserProfile(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserException("User not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUserProfile(Long id, String fullName, String email) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserException("User not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (email != null && !email.equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new UserException("Email is already taken by another account", "EMAIL_ALREADY_EXISTS");
            }
            user.setEmail(email);
        }

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName);
        }

        User updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }

    @Override
    @Transactional
    public void changePassword(Long id, ChangePasswordRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserException("User not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new UserException("Old password does not match", "INVALID_PASSWORD", HttpStatus.UNAUTHORIZED);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
