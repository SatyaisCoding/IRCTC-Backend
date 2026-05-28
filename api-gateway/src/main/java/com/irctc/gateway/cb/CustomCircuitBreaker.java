package com.irctc.gateway.cb;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe, non-blocking Circuit Breaker with Option A: Distributed Redis + Local Cache.
 */
@Slf4j
public class CustomCircuitBreaker {

    @Getter
    private final String serviceName;
    private final ReactiveStringRedisTemplate redisTemplate;

    // Keys used in Redis
    private final String stateKey;
    private final String failuresKey;
    private final String nextAttemptKey;

    // In-memory local cache for fast non-blocking checks
    private volatile CircuitBreakerState state = CircuitBreakerState.CLOSED;
    private final AtomicInteger failures = new AtomicInteger(0);
    private volatile long nextAttemptTime = 0;

    private final int threshold = 5;
    private final long retryIntervalMs = 60000; // 60 seconds

    public CustomCircuitBreaker(String serviceName, ReactiveStringRedisTemplate redisTemplate) {
        this.serviceName = serviceName;
        this.redisTemplate = redisTemplate;
        this.stateKey = "gateway:cb:" + serviceName + ":state";
        this.failuresKey = "gateway:cb:" + serviceName + ":failures";
        this.nextAttemptKey = "gateway:cb:" + serviceName + ":nextAttempt";
    }

    /**
     * Non-blocking check if request is allowed.
     */
    public boolean allowRequest() {
        CircuitBreakerState currentState = this.state;

        if (currentState == CircuitBreakerState.CLOSED) {
            return true;
        }

        if (currentState == CircuitBreakerState.OPEN) {
            long now = System.currentTimeMillis();
            if (now >= nextAttemptTime) {
                // Time window passed -> Transition to HALF_OPEN to attempt recovery
                this.state = CircuitBreakerState.HALF_OPEN;
                log.info("[Circuit Breaker] Service {} is transitioning to HALF_OPEN for probing...", serviceName);
                
                // Persist state change to Redis asynchronously (non-blocking)
                redisTemplate.opsForValue().set(stateKey, CircuitBreakerState.HALF_OPEN.name()).subscribe();
                return true;
            }
            return false; // Fail fast!
        }

        // HALF_OPEN allows requests to flow to test the service
        return true;
    }

    /**
     * Record a successful invocation of the downstream service.
     */
    public void recordSuccess() {
        CircuitBreakerState oldState = this.state;
        
        // Reset failures locally
        this.failures.set(0);
        this.state = CircuitBreakerState.CLOSED;

        if (oldState == CircuitBreakerState.HALF_OPEN || oldState == CircuitBreakerState.OPEN) {
            log.info("[Circuit Breaker] closed for {}", serviceName);
        }

        // Update Redis state asynchronously (non-blocking)
        Mono.when(
            redisTemplate.opsForValue().set(stateKey, CircuitBreakerState.CLOSED.name()),
            redisTemplate.opsForValue().set(failuresKey, "0"),
            redisTemplate.opsForValue().delete(nextAttemptKey)
        ).subscribe(
            null,
            err -> log.error("[Circuit Breaker] Failed to update Redis success state for {}: {}", serviceName, err.getMessage())
        );
    }

    /**
     * Record a failure invocation of the downstream service (e.g., connection refused, 5xx error).
     */
    public void recordFailure(Throwable ex) {
        int currentFailures = this.failures.incrementAndGet();
        CircuitBreakerState currentState = this.state;

        if (currentState == CircuitBreakerState.CLOSED && currentFailures >= threshold) {
            // Trip the breaker!
            long now = System.currentTimeMillis();
            this.nextAttemptTime = now + retryIntervalMs;
            this.state = CircuitBreakerState.OPEN;

            log.error("[Circuit Breaker] OPENED for service: {} due to {} failures. Error: {}. Next attempt in 60s.", 
                    serviceName, currentFailures, ex != null ? ex.getMessage() : "unknown", retryIntervalMs / 1000);

            // Persist to Redis asynchronously
            Mono.when(
                redisTemplate.opsForValue().set(stateKey, CircuitBreakerState.OPEN.name()),
                redisTemplate.opsForValue().set(failuresKey, String.valueOf(currentFailures)),
                redisTemplate.opsForValue().set(nextAttemptKey, String.valueOf(this.nextAttemptTime))
            ).subscribe(
                null,
                err -> log.error("[Circuit Breaker] Failed to persist OPEN state to Redis for {}: {}", serviceName, err.getMessage())
            );
        } else if (currentState == CircuitBreakerState.HALF_OPEN) {
            // Any failure in HALF_OPEN trips it back to OPEN immediately!
            long now = System.currentTimeMillis();
            this.nextAttemptTime = now + retryIntervalMs;
            this.state = CircuitBreakerState.OPEN;

            log.error("[Circuit Breaker] Half-open probe failed. Re-opened circuit for service: {}. Error: {}", 
                    serviceName, ex != null ? ex.getMessage() : "unknown");

            // Persist to Redis asynchronously
            Mono.when(
                redisTemplate.opsForValue().set(stateKey, CircuitBreakerState.OPEN.name()),
                redisTemplate.opsForValue().set(failuresKey, String.valueOf(currentFailures)),
                redisTemplate.opsForValue().set(nextAttemptKey, String.valueOf(this.nextAttemptTime))
            ).subscribe(
                null,
                err -> log.error("[Circuit Breaker] Failed to persist HALF_OPEN failure state to Redis for {}: {}", serviceName, err.getMessage())
            );
        } else {
            // Just sync failures count to Redis for standard tracking
            redisTemplate.opsForValue().set(failuresKey, String.valueOf(currentFailures)).subscribe();
        }
    }

    /**
     * Returns the current state (from local cache).
     */
    public CircuitBreakerState getState() {
        return this.state;
    }

    /**
     * Get current failure count (from local cache).
     */
    public int getFailureCount() {
        return this.failures.get();
    }

    /**
     * Synchronize this instance's local cache with Redis values.
     * This is called by the background synchronization scheduler.
     */
    public void syncFromRedis(String stateStr, String failuresStr, String nextAttemptStr) {
        try {
            if (stateStr != null) {
                CircuitBreakerState redisState = CircuitBreakerState.valueOf(stateStr);
                if (this.state != redisState) {
                    log.debug("[Circuit Breaker Cache Sync] State updated for {}: {} -> {}", serviceName, this.state, redisState);
                    this.state = redisState;
                }
            }
            if (failuresStr != null) {
                this.failures.set(Integer.parseInt(failuresStr));
            }
            if (nextAttemptStr != null) {
                this.nextAttemptTime = Long.parseLong(nextAttemptStr);
            }
        } catch (Exception e) {
            log.error("[Circuit Breaker Cache Sync] Error syncing {} from Redis: {}", serviceName, e.getMessage());
        }
    }
}
