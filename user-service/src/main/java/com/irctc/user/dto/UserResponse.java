package com.irctc.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse implements Serializable {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String role;
    private boolean isVerified;
    private LocalDateTime createdAt;
}
