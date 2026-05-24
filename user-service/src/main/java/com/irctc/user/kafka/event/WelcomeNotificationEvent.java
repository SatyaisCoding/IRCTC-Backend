package com.irctc.user.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka event published by user-service when a new user registers via Google OAuth.
 * Triggers a welcome email from the notification-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WelcomeNotificationEvent {

    /** Recipient email address */
    private String toEmail;

    /** Full name of the newly registered user */
    private String fullName;
}
