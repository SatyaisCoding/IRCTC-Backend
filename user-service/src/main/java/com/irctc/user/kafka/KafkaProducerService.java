package com.irctc.user.kafka;

import com.irctc.user.kafka.event.OtpNotificationEvent;
import com.irctc.user.kafka.event.WelcomeNotificationEvent;

public interface KafkaProducerService {

    void sendOtpNotification(OtpNotificationEvent event);

    void sendWelcomeNotification(WelcomeNotificationEvent event);
}
