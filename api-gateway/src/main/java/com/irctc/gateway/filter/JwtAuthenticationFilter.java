package com.irctc.gateway.filter;

import com.irctc.gateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Global JWT Authentication Filter.
 *
 * Runs on EVERY incoming request before routing.
 *
 * Flow:
 *  1. Check if path is public (auth endpoints) → skip validation
 *  2. Extract "Authorization: Bearer <token>" header
 *  3. Validate JWT signature + expiry via JwtUtil
 *  4. Inject X-User-Id and X-User-Role headers into the downstream request
 *  5. Reject with 401 if token is missing or invalid
 *
 * Downstream services (user-service, etc.) trust these headers
 * and do NOT need to re-validate the JWT.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    // ── Public paths — no JWT required ────────────────────────────────────
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/verify-otp",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/refresh",
            "/api/v1/auth/oauth2",
            "/api/v1/gateway/health",
            "/actuator/health",
            "/actuator/info"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // ── Step 1: Skip public routes ────────────────────────────────────
        if (isPublicPath(path)) {
            log.debug("[Auth Filter] Public route, skipping JWT check: {}", path);
            return chain.filter(exchange);
        }

        // ── Step 2: Extract Bearer token ──────────────────────────────────
        String token = extractToken(exchange.getRequest());
        if (token == null) {
            log.warn("[Auth Filter] Missing Authorization header for: {}", path);
            return rejectWith(exchange, HttpStatus.UNAUTHORIZED, "Authorization header is missing or malformed");
        }

        // ── Step 3: Validate token ────────────────────────────────────────
        if (!jwtUtil.validateToken(token)) {
            log.warn("[Auth Filter] Invalid or expired JWT for path: {}", path);
            return rejectWith(exchange, HttpStatus.UNAUTHORIZED, "Token is invalid or expired");
        }

        // ── Step 4: Extract claims and forward as headers ─────────────────
        Long   userId   = jwtUtil.getUserId(token);
        String username = jwtUtil.getUsername(token);
        String role     = jwtUtil.getRole(token);

        log.debug("[Auth Filter] Authenticated user:{} role:{} → {}", userId, role, path);

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id",   String.valueOf(userId))
                .header("X-Username",  username)
                .header("X-User-Role", role)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    // ── Run first, before all other filters ──────────────────────────────
    @Override
    public int getOrder() {
        return -1;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private Mono<Void> rejectWith(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");

        String body = """
                {"success":false,"message":"%s","errorCode":"AUTHENTICATION_FAILED"}
                """.formatted(message).trim();

        var buffer = response.bufferFactory().wrap(body.getBytes());
        return response.writeWith(Mono.just(buffer));
    }
}
