package com.irctc.gateway.cb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry holding and synchronizing custom distributed circuit breakers.
 */
@Component
@Slf4j
@EnableScheduling
public class CircuitBreakerRegistry {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ConcurrentHashMap<String, CustomCircuitBreaker> breakers = new ConcurrentHashMap<>();

    public CircuitBreakerRegistry(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        
        // Pre-initialize known microservices
        getBreaker("user-service");
        getBreaker("notification-service");
        getBreaker("search-service");
        getBreaker("booking-service");
        getBreaker("payment-service");
    }

    /**
     * Get or create a circuit breaker for a service.
     */
    public CustomCircuitBreaker getBreaker(String serviceName) {
        if (serviceName == null || serviceName.isBlank()) {
            return null;
        }
        return breakers.computeIfAbsent(serviceName, name -> {
            log.info("[CircuitBreakerRegistry] Created custom distributed circuit breaker for service: {}", name);
            return new CustomCircuitBreaker(name, redisTemplate);
        });
    }

    /**
     * Returns all registered circuit breakers.
     */
    public Collection<CustomCircuitBreaker> getAllBreakers() {
        return breakers.values();
    }

    /**
     * Background task running every 5 seconds to synchronize local memory cache with Redis state.
     * Keeps multiple gateway instances synchronized with zero latency cost on HTTP forwarding.
     */
    @Scheduled(fixedRate = 5000)
    public void syncBreakersWithRedis() {
        if (breakers.isEmpty()) return;

        log.trace("[CircuitBreakerRegistry] Initiating non-blocking cache sync with Redis...");
        
        for (CustomCircuitBreaker cb : breakers.values()) {
            String serviceName = cb.getServiceName();
            String stateKey = "gateway:cb:" + serviceName + ":state";
            String failuresKey = "gateway:cb:" + serviceName + ":failures";
            String nextAttemptKey = "gateway:cb:" + serviceName + ":nextAttempt";

            Mono.zip(
                redisTemplate.opsForValue().get(stateKey).defaultIfEmpty("CLOSED"),
                redisTemplate.opsForValue().get(failuresKey).defaultIfEmpty("0"),
                redisTemplate.opsForValue().get(nextAttemptKey).defaultIfEmpty("0")
            ).subscribe(
                tuple -> cb.syncFromRedis(tuple.getT1(), tuple.getT2(), tuple.getT3()),
                err -> log.error("[CircuitBreakerRegistry] Failed to sync Redis state for {}: {}", serviceName, err.getMessage())
            );
        }
    }
}
