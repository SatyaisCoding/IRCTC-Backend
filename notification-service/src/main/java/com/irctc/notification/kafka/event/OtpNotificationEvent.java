package com.irctc.notification.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mirror of the OtpNotificationEvent published by user-service.
 * Used by notification-service Kafka consumer to deserialize the message.
 *
 * Note: This is an intentional duplicate — no shared library to avoid coupling.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpNotificationEvent {

    /** Recipient email address */
    private String toEmail;

    /** The generated OTP code to embed in the email */
    private String otp;

    /**
     * Type of OTP email to send.
     * Expected values: "EMAIL_VERIFICATION" | "PASSWORD_RESET"
     */
    private String templateType;
}
