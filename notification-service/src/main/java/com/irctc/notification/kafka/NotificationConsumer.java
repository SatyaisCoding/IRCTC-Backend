package com.irctc.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.irctc.notification.dto.NotificationRequest;
import com.irctc.notification.kafka.event.OtpNotificationEvent;
import com.irctc.notification.kafka.event.WelcomeNotificationEvent;
import com.irctc.notification.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NotificationConsumer.class);

    private final MailService mailService;
    private final ObjectMapper objectMapper;

    // ==========================================
    // Core Topic Consumers
    // ==========================================

    @KafkaListener(topics = "notification.email.otp")
    public void consumeOtpEvent(String message) {
        log.info("Received OtpNotificationEvent from Kafka: {}", message);
        try {
            OtpNotificationEvent event = objectMapper.readValue(message, OtpNotificationEvent.class);
            
            NotificationRequest request = NotificationRequest.builder()
                    .toEmail(event.getToEmail())
                    .templateType(event.getTemplateType() != null ? event.getTemplateType() : "EMAIL_VERIFICATION")
                    .templateModel(Map.of("otp", event.getOtp()))
                    .build();
                    
            mailService.sendEmail(request);
        } catch (Exception ex) {
            log.error("Failed to parse or process OTP event: {}", ex.getMessage());
            throw new IllegalStateException("OTP processing failed", ex);
        }
    }

    @KafkaListener(topics = "notification.email.welcome")
    public void consumeWelcomeEvent(String message) {
        log.info("Received WelcomeNotificationEvent from Kafka: {}", message);
        try {
            WelcomeNotificationEvent event = objectMapper.readValue(message, WelcomeNotificationEvent.class);
            
            NotificationRequest request = NotificationRequest.builder()
                    .toEmail(event.getToEmail())
                    .templateType("WELCOME")
                    .templateModel(Map.of("firstName", event.getFullName()))
                    .build();
                    
            mailService.sendEmail(request);
        } catch (Exception ex) {
            log.error("Failed to parse or process Welcome event: {}", ex.getMessage());
            throw new IllegalStateException("Welcome processing failed", ex);
        }
    }

    // ==========================================
    // Dead Letter Queue (DLQ) Consumers
    // ==========================================

    @KafkaListener(topics = "notification.email.otp-dlt")
    public void consumeOtpDltEvent(String message) {
        log.warn("==================================================================");
        log.warn("SMTP EMAIL SENDING FAILED (DLQ INTERCEPTED)");
        log.warn("REASON: Local exponential retries exhausted. Routed to DLQ.");
        log.warn("FALLBACK MODE: Printing dynamically resolved message below.");
        try {
            OtpNotificationEvent event = objectMapper.readValue(message, OtpNotificationEvent.class);
            log.warn(">>> RECIPIENT: {}", event.getToEmail());
            log.warn(">>> SUBJECT: IRCTC - Email Verification OTP");
            log.warn(">>> BODY OTP: {}", event.getOtp());
        } catch (Exception ex) {
            log.error(">>> RAW DLQ MSG (FAILED TO PARSE): {}", message);
        }
        log.warn("==================================================================");
    }

    @KafkaListener(topics = "notification.email.welcome-dlt")
    public void consumeWelcomeDltEvent(String message) {
        log.warn("==================================================================");
        log.warn("SMTP EMAIL SENDING FAILED (DLQ INTERCEPTED)");
        log.warn("REASON: Local exponential retries exhausted. Routed to DLQ.");
        log.warn("FALLBACK MODE: Printing dynamically resolved message below.");
        try {
            WelcomeNotificationEvent event = objectMapper.readValue(message, WelcomeNotificationEvent.class);
            log.warn(">>> RECIPIENT: {}", event.getToEmail());
            log.warn(">>> SUBJECT: Welcome to IRCTC!");
            log.warn(">>> BODY: Welcome {} to IRCTC microservices portal!", event.getFullName());
        } catch (Exception ex) {
            log.error(">>> RAW DLQ MSG (FAILED TO PARSE): {}", message);
        }
        log.warn("==================================================================");
    }
}
