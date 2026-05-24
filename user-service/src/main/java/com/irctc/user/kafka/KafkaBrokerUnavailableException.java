package com.irctc.user.kafka;

/**
 * Thrown by KafkaHealthValidator when the Kafka broker cannot be reached
 * before a notification event is published.
 *
 * This is a RuntimeException so it propagates up through the service layer
 * without requiring callers to explicitly declare it in throws clauses.
 */
public class KafkaBrokerUnavailableException extends RuntimeException {

    public KafkaBrokerUnavailableException(String message) {
        super(message);
    }

    public KafkaBrokerUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
