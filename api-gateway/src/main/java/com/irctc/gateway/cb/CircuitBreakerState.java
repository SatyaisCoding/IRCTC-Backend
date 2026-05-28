package com.irctc.gateway.cb;

/**
 * States of the custom distributed Circuit Breaker.
 */
public enum CircuitBreakerState {
    CLOSED,
    OPEN,
    HALF_OPEN
}
