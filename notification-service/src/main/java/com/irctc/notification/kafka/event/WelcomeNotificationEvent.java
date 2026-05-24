package com.irctc.notification.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mirror of the WelcomeNotificationEvent published by user-service.
 * Used by notification-service Kafka consumer to deserialize the message.
 *
 * Note: This is an intentional duplicate — no shared library to avoid coupling.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WelcomeNotificationEvent {

    /** Recipient email address */
    private String toEmail;

    /** Full name of the newly registered user */
    private String fullName;
}
