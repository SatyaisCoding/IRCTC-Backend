package com.irctc.notification.dto;

import java.time.LocalDateTime;

public class ApiResponse {
    private boolean success;
    private String message;
    private LocalDateTime timestamp;

    // Constructors
    public ApiResponse() {}

    public ApiResponse(boolean success, String message, LocalDateTime timestamp) {
        this.success = success;
        this.message = message;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    // Static factory methods
    public static ApiResponse success(String message) {
        return new ApiResponse(true, message, LocalDateTime.now());
    }

    public static ApiResponse error(String message) {
        return new ApiResponse(false, message, LocalDateTime.now());
    }
}
