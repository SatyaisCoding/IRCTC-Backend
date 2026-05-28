package com.irctc.user.controller;

import com.irctc.user.dto.ApiResponse;
import com.irctc.user.dto.ChangePasswordRequest;
import com.irctc.user.dto.UserResponse;
import com.irctc.user.dto.UserProfileUpdateRequest;
import com.irctc.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ── GET /api/v1/user/profile ───────────────────────────────────────────
    // Redis-first lookup: cache HIT (20ms) or DB fallback + cache store (200ms)
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getUserProfile(
            @RequestHeader("X-User-Id") Long userId) {
        UserResponse response = userService.getUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("User profile fetched successfully", response));
    }

    // ── PUT /api/v1/user/profile/update ───────────────────────────────────
    // Writes to DB + evicts Redis cache
    @PutMapping("/profile/update")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserProfile(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        UserResponse response = userService.updateUserProfile(userId, request.getFullName(), request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("User profile updated successfully", response));
    }

    // ── DELETE /api/v1/user/profile ───────────────────────────────────────
    // Deletes account from DB + evicts Redis cache
    @DeleteMapping("/profile")
    public ResponseEntity<ApiResponse<String>> deleteProfile(
            @RequestHeader("X-User-Id") Long userId) {
        userService.deleteProfile(userId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Account deleted successfully"));
    }

    // ── POST /api/v1/user/change-password ─────────────────────────────────
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }
}
