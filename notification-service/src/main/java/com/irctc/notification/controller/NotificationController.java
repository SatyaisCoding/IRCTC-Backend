package com.irctc.notification.controller;

import com.irctc.notification.dto.ApiResponse;
import com.irctc.notification.dto.NotificationRequest;
import com.irctc.notification.service.MailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final MailService mailService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse> sendNotification(@Valid @RequestBody NotificationRequest request) {
        mailService.sendEmail(request);
        return ResponseEntity.ok(ApiResponse.success("Template email processed and delivered successfully"));
    }
}
