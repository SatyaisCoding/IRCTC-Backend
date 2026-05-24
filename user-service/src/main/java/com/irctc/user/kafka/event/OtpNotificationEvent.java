package com.irctc.user.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka event published by user-service to notify the notification-service
 * to send an OTP email (EMAIL_VERIFICATION or PASSWORD_RESET).
 */
@Data
@Builder
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
