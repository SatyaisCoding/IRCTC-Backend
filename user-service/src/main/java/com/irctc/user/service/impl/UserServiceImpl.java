package com.irctc.user.service.impl;

import com.irctc.user.dto.ChangePasswordRequest;
import com.irctc.user.dto.UserResponse;
import com.irctc.user.entity.User;
import com.irctc.user.exception.UserException;
import com.irctc.user.mapper.UserMapper;
import com.irctc.user.repository.UserRepository;
import com.irctc.user.service.UserCacheService;
import com.irctc.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository    userRepository;
    private final UserMapper        userMapper;
    private final PasswordEncoder   passwordEncoder;
    private final UserCacheService  userCacheService;

    // ── GET Profile ────────────────────────────────────────────────────────
    // Cache-Aside: Redis first → DB on miss → store in Redis → return
    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserProfile(Long id) {

        // 1. Redis lookup (20ms)
        UserResponse cached = userCacheService.get(id);
        if (cached != null) {
            return cached;
        }

        // 2. Cache miss → fetch from PostgreSQL (200ms)
        log.info("[UserService] Cache miss for user:{} — fetching from DB", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserException("User not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        UserResponse response = userMapper.toResponse(user);

        // 3. Store in Redis for future lookups (TTL 24h)
        userCacheService.put(response);

        return response;
    }

    // ── UPDATE Profile ─────────────────────────────────────────────────────
    // Write to DB → evict stale cache → return fresh data
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
        UserResponse response = userMapper.toResponse(updatedUser);

        // Evict stale cache — next GET will re-populate with fresh data
        userCacheService.evict(id);
        log.info("[UserService] Profile updated for user:{} — cache evicted", id);

        return response;
    }

    // ── DELETE Profile ─────────────────────────────────────────────────────
    // Delete from DB → evict from Redis
    @Override
    @Transactional
    public void deleteProfile(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserException("User not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        userRepository.delete(user);

        // Evict from cache immediately
        userCacheService.evict(id);
        log.info("[UserService] Account deleted for user:{} — cache evicted", id);
    }

    // ── CHANGE Password ────────────────────────────────────────────────────
    @Override
    @Transactional
    public void changePassword(Long id, ChangePasswordRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserException("User not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new UserException("Old password does not match", "INVALID_PASSWORD", HttpStatus.UNAUTHORIZED);
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new UserException("New password cannot be the same as the old password", "SAME_PASSWORD", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
