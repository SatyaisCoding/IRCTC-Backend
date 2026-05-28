package com.irctc.gateway.controller;

import com.irctc.gateway.cb.CircuitBreakerRegistry;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller providing deep insights into Gateway Health, Redis Status, and Custom Circuit Breakers.
 */
@RestController
@RequiredArgsConstructor
public class GatewayHealthController {

    private final CircuitBreakerRegistry cbRegistry;
    private final ReactiveStringRedisTemplate redisTemplate;

    @GetMapping("/api/v1/gateway/health")
    public Mono<GatewayHealthResponse> getGatewayHealth() {
        // Reactive Redis Ping check
        return redisTemplate.getConnectionFactory().getReactiveConnection().ping()
                .map(ping -> "UP")
                .onErrorReturn("DOWN")
                .map(redisStatus -> {
                    List<ServiceBreakerStatus> cbStatuses = cbRegistry.getAllBreakers().stream()
                            .map(cb -> ServiceBreakerStatus.builder()
                                    .serviceName(cb.getServiceName())
                                    .state(cb.getState().name())
                                    .failureCount(cb.getFailureCount())
                                    .build())
                            .collect(Collectors.toList());

                    return GatewayHealthResponse.builder()
                            .success(true)
                            .gatewayStatus("UP")
                            .redisStatus(redisStatus)
                            .circuitBreakers(cbStatuses)
                            .build();
                });
    }

    @Getter
    @Builder
    public static class GatewayHealthResponse {
        private final boolean success;
        private final String gatewayStatus;
        private final String redisStatus;
        private final List<ServiceBreakerStatus> circuitBreakers;
    }

    @Getter
    @Builder
    public static class ServiceBreakerStatus {
        private final String serviceName;
        private final String state;
        private final int failureCount;
    }
}
