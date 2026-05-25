package com.irctc.user.kafka;

/**
 * Thrown by KafkaHealthValidator when the Kafka broker cannot be reached
 * before a notification event is published.
 */
public class KafkaBrokerUnavailableException extends RuntimeException {

    public KafkaBrokerUnavailableException(String message) {
        super(message);
    }

    public KafkaBrokerUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
