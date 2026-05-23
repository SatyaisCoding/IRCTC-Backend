package com.irctc.search.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/search")
public class HealthCheckController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("serviceName", "search-service");
        healthInfo.put("timestamp", LocalDateTime.now());
        healthInfo.put("message", "Search Service is up and running successfully.");
        return ResponseEntity.ok(healthInfo);
    }
}
