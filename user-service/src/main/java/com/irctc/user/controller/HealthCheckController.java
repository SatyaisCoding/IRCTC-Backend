package com.irctc.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthCheckController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getIndex() {
        Map<String, Object> welcomeInfo = new HashMap<>();
        welcomeInfo.put("status", "RUNNING");
        welcomeInfo.put("message", "Welcome to IRCTC User Service Microservice");
        welcomeInfo.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(welcomeInfo);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("serviceName", "user-service");
        healthInfo.put("timestamp", LocalDateTime.now());
        healthInfo.put("message", "User Service is up and running successfully.");
        return ResponseEntity.ok(healthInfo);
    }
}
