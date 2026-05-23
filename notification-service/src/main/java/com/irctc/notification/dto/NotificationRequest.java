package com.irctc.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    @NotBlank(message = "Recipient email (toEmail) cannot be blank")
    @Email(message = "Recipient email must be a valid email address")
    private String toEmail;

    @NotBlank(message = "Template type cannot be blank")
    private String templateType; // e.g., "EMAIL_VERIFICATION", "PASSWORD_RESET"

    @NotNull(message = "Template model parameters cannot be null")
    private Map<String, String> templateModel; // e.g., { "otp": "123456" }
}
