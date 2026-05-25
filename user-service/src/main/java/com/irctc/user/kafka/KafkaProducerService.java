package com.irctc.user.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.otp-notification}")
    private String otpTopic;

    @Value("${app.kafka.topics.welcome-notification}")
    private String welcomeTopic;

    // Java 21 record events to avoid boilerplate
    public record OtpNotificationEvent(String email, String otp, String ttlMinutes) {}
    public record WelcomeNotificationEvent(String email, String firstName) {}

    public void sendOtpNotification(OtpNotificationEvent event) {
        checkKafkaConnection(otpTopic);
        log.info("Sending OTP verification event to topic: {}", otpTopic);
        try {
            // Synchronously block to guarantee ordered sequential delivery
            kafkaTemplate.send(otpTopic, event.email(), event)
                    .get(5, TimeUnit.SECONDS);
            log.info("Successfully published OTP event for: {}", event.email());
        } catch (Exception ex) {
            log.error("Failed to publish OTP event to Kafka: {}", ex.getMessage());
            throw new IllegalStateException("Kafka publishing failed", ex);
        }
    }

    public void sendWelcomeNotification(WelcomeNotificationEvent event) {
        checkKafkaConnection(welcomeTopic);
        log.info("Sending Welcome event to topic: {}", welcomeTopic);
        try {
            // Synchronously block to guarantee ordered sequential delivery
            kafkaTemplate.send(welcomeTopic, event.email(), event)
                    .get(5, TimeUnit.SECONDS);
            log.info("Successfully published Welcome event for: {}", event.email());
        } catch (Exception ex) {
            log.error("Failed to publish Welcome event to Kafka: {}", ex.getMessage());
            throw new IllegalStateException("Kafka publishing failed", ex);
        }
    }

    private void checkKafkaConnection(String topic) {
        try {
            // Fetch partitions with metadata lookup to guarantee broker connectivity
            var metadata = kafkaTemplate.partitionsFor(topic);
            if (metadata == null || metadata.isEmpty()) {
                throw new IllegalStateException("No active partitions for topic: " + topic);
            }
        } catch (Exception ex) {
            log.error("Pre-send Kafka connection verification failed for topic: {}. Broker is offline.", topic);
            throw new IllegalStateException("Unable to connect to Kafka broker", ex);
        }
    }
}
