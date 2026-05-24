package com.irctc.user.kafka;

import com.irctc.user.kafka.event.OtpNotificationEvent;
import com.irctc.user.kafka.event.WelcomeNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer service that publishes notification events to the broker.
 *
 * ┌─ Connection Safety ────────────────────────────────────────────────────┐
 * │                                                                         │
 * │  Before every send, KafkaHealthValidator.ensureConnected() is called.  │
 * │  This performs a lightweight broker probe (cached for 30 seconds)      │
 * │  and throws KafkaBrokerUnavailableException if the broker is down.     │
 * │                                                                         │
 * │  This prevents blindly firing kafkaTemplate.send() into a void and    │
 * │  only discovering the failure asynchronously in the CompletableFuture. │
 * │                                                                         │
 * └────────────────────────────────────────────────────────────────────────┘
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerServiceImpl implements KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaHealthValidator kafkaHealthValidator;

    @Value("${app.kafka.topics.otp-notification}")
    private String otpNotificationTopic;

    @Value("${app.kafka.topics.welcome-notification}")
    private String welcomeNotificationTopic;

    @Override
    public void sendOtpNotification(OtpNotificationEvent event) {
        // ── Step 1: Verify broker is reachable before attempting send ──────
        kafkaHealthValidator.ensureConnected();

        // ── Step 2: Publish event asynchronously ───────────────────────────
        log.info("[KafkaProducer] Publishing OTP event → topic: [{}] | email: [{}] | type: [{}]",
                otpNotificationTopic, event.getToEmail(), event.getTemplateType());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(otpNotificationTopic, event.getToEmail(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[KafkaProducer]  OTP event delivery FAILED for email [{}]: {}",
                        event.getToEmail(), ex.getMessage());
            } else {
                log.info("[KafkaProducer]  OTP event delivered | email: [{}] | " +
                         "partition: {} | offset: {}",
                        event.getToEmail(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    @Override
    public void sendWelcomeNotification(WelcomeNotificationEvent event) {
        // ── Step 1: Verify broker is reachable before attempting send ──────
        kafkaHealthValidator.ensureConnected();

        // ── Step 2: Publish event asynchronously ───────────────────────────
        log.info("[KafkaProducer] Publishing Welcome event → topic: [{}] | email: [{}]",
                welcomeNotificationTopic, event.getToEmail());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(welcomeNotificationTopic, event.getToEmail(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[KafkaProducer]  Welcome event delivery FAILED for email [{}]: {}",
                        event.getToEmail(), ex.getMessage());
            } else {
                log.info("[KafkaProducer]  Welcome event delivered | email: [{}] | " +
                         "partition: {} | offset: {}",
                        event.getToEmail(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
