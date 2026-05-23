package com.irctc.user.controller;

import com.irctc.user.dto.ApiResponse;
import com.irctc.user.dto.ChangePasswordRequest;
import com.irctc.user.dto.UserResponse;
import com.irctc.user.security.UserPrincipal;
import com.irctc.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getUserProfile(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        UserResponse response = userService.getUserProfile(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("User profile fetched successfully", response));
    }

    @PutMapping("/profile/update")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String email) {
        UserResponse response = userService.updateUserProfile(userPrincipal.getId(), fullName, email);
        return ResponseEntity.ok(ApiResponse.success("User profile updated successfully", response));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }
}
