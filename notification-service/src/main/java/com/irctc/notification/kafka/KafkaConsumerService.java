package com.irctc.notification.kafka;

import com.irctc.notification.dto.NotificationRequest;
import com.irctc.notification.kafka.event.OtpNotificationEvent;
import com.irctc.notification.kafka.event.WelcomeNotificationEvent;
import com.irctc.notification.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final MailService mailService;

    @KafkaListener(
            topics = "notification.email.otp",
            groupId = "notification-service-group",
            containerFactory = "otpKafkaListenerContainerFactory"
    )
    public void consumeOtpNotification(OtpNotificationEvent event) {
        log.info("[OTP Consumer] Processing → email: [{}] | type: [{}]",
                event.getToEmail(), event.getTemplateType());

        Map<String, String> templateModel = new HashMap<>();
        templateModel.put("otp", event.getOtp());

        mailService.sendEmail(NotificationRequest.builder()
                .toEmail(event.getToEmail())
                .templateType(event.getTemplateType())
                .templateModel(templateModel)
                .build());

        log.info("[OTP Consumer] ✅ Email dispatched for: [{}]", event.getToEmail());
    }

    // Invoked after all 3 retries are exhausted — record moved to DLT permanently
    @DltHandler
    @KafkaListener(
            topics = "notification.email.otp.DLT",
            groupId = "notification-service-dlt-group",
            containerFactory = "otpKafkaListenerContainerFactory"
    )
    public void handleOtpDlt(ConsumerRecord<String, OtpNotificationEvent> record) {
        log.error("[OTP DLT] Permanently failed | email: [{}] | type: [{}] | topic: {}",
                record.value() != null ? record.value().getToEmail() : "unknown",
                record.value() != null ? record.value().getTemplateType() : "unknown",
                record.topic());
        // TODO: alert via Slack / PagerDuty
    }

    @KafkaListener(
            topics = "notification.email.welcome",
            groupId = "notification-service-group",
            containerFactory = "welcomeKafkaListenerContainerFactory"
    )
    public void consumeWelcomeNotification(WelcomeNotificationEvent event) {
        log.info("[Welcome Consumer] Processing → email: [{}]", event.getToEmail());

        Map<String, String> templateModel = new HashMap<>();
        templateModel.put("fullName", event.getFullName());

        mailService.sendEmail(NotificationRequest.builder()
                .toEmail(event.getToEmail())
                .templateType("WELCOME")
                .templateModel(templateModel)
                .build());

        log.info("[Welcome Consumer] ✅ Email dispatched for: [{}]", event.getToEmail());
    }

    // Invoked after all 3 retries are exhausted — record moved to DLT permanently
    @DltHandler
    @KafkaListener(
            topics = "notification.email.welcome.DLT",
            groupId = "notification-service-dlt-group",
            containerFactory = "welcomeKafkaListenerContainerFactory"
    )
    public void handleWelcomeDlt(ConsumerRecord<String, WelcomeNotificationEvent> record) {
        log.error("[Welcome DLT] Permanently failed | email: [{}] | name: [{}] | topic: {}",
                record.value() != null ? record.value().getToEmail() : "unknown",
                record.value() != null ? record.value().getFullName() : "unknown",
                record.topic());
        // TODO: alert via Slack / PagerDuty
    }
}
