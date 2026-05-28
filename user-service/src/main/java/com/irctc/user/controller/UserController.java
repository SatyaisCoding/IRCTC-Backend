package com.irctc.user.controller;

import com.irctc.user.dto.ApiResponse;
import com.irctc.user.dto.ChangePasswordRequest;
import com.irctc.user.dto.UserResponse;
import com.irctc.user.security.UserPrincipal;
import com.irctc.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        UserResponse response = userService.getUserProfile(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("User profile fetched successfully", response));
    }

    // ── PUT /api/v1/user/profile/update ───────────────────────────────────
    // Writes to DB + evicts Redis cache
    @PutMapping("/profile/update")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String email) {
        UserResponse response = userService.updateUserProfile(userPrincipal.getId(), fullName, email);
        return ResponseEntity.ok(ApiResponse.success("User profile updated successfully", response));
    }

    // ── DELETE /api/v1/user/profile ───────────────────────────────────────
    // Deletes account from DB + evicts Redis cache
    @DeleteMapping("/profile")
    public ResponseEntity<ApiResponse<String>> deleteProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        userService.deleteProfile(userPrincipal.getId());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Account deleted successfully"));
    }

    // ── POST /api/v1/user/change-password ─────────────────────────────────
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }
}
