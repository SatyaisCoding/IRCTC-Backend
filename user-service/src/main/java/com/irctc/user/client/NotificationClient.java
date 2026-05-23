package com.irctc.user.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "notification-service", url = "${app.services.notification-service-url}")
public interface NotificationClient {

    @PostMapping("/api/v1/notification/send")
    void sendNotification(@RequestBody NotificationRequest request);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class NotificationRequest {
        private String toEmail;
        private String templateType; // e.g. "EMAIL_VERIFICATION", "PASSWORD_RESET"
        private Map<String, String> templateModel; // e.g. { "otp": "123456" }
    }
}
