package com.irctc.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * Rate Limiter Key Resolvers.
 *
 * Two strategies:
 *  - userKeyResolver  → rate limit per authenticated user (X-User-Id header)
 *  - ipKeyResolver    → rate limit per IP address (for public/unauthenticated routes)
 *
 * Spring Cloud Gateway uses Redis under the hood with the Token Bucket algorithm:
 *  - replenishRate  = tokens added per second (steady-state throughput)
 *  - burstCapacity  = max tokens bucket can hold (peak burst allowed)
 */
@Configuration
public class RateLimiterConfig {

    /**
     * For protected routes — limits per authenticated user ID.
     * Falls back to IP if header is absent.
     */
    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just("user:" + userId);
            }
            // Fallback to IP for unauthenticated requests that slipped through
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just("ip:" + ip);
        };
    }

    /**
     * For public routes (login, register) — limits per IP address.
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            return Mono.just("ip:" + ip);
        };
    }
}
