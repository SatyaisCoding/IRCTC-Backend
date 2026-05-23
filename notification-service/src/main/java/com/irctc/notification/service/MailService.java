package com.irctc.notification.service;

import com.irctc.notification.dto.NotificationRequest;

public interface MailService {
    void sendEmail(NotificationRequest request);
}
