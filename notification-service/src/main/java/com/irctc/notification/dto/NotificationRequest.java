package com.irctc.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public class NotificationRequest {

    @NotBlank(message = "Recipient email (toEmail) cannot be blank")
    @Email(message = "Recipient email must be a valid email address")
    private String toEmail;

    @NotBlank(message = "Template type cannot be blank")
    private String templateType; // e.g., "EMAIL_VERIFICATION", "PASSWORD_RESET"

    @NotNull(message = "Template model parameters cannot be null")
    private Map<String, String> templateModel; // e.g., { "otp": "123456" }

    // Constructors
    public NotificationRequest() {}

    public NotificationRequest(String toEmail, String templateType, Map<String, String> templateModel) {
        this.toEmail = toEmail;
        this.templateType = templateType;
        this.templateModel = templateModel;
    }

    // Getters and Setters
    public String getToEmail() {
        return toEmail;
    }

    public void setToEmail(String toEmail) {
        this.toEmail = toEmail;
    }

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public Map<String, String> getTemplateModel() {
        return templateModel;
    }

    public void setTemplateModel(Map<String, String> templateModel) {
        this.templateModel = templateModel;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String toEmail;
        private String templateType;
        private Map<String, String> templateModel;

        public Builder toEmail(String toEmail) {
            this.toEmail = toEmail;
            return this;
        }

        public Builder templateType(String templateType) {
            this.templateType = templateType;
            return this;
        }

        public Builder templateModel(Map<String, String> templateModel) {
            this.templateModel = templateModel;
            return this;
        }

        public NotificationRequest build() {
            return new NotificationRequest(toEmail, templateType, templateModel);
        }
    }
}
