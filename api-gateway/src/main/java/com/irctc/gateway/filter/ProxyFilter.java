package com.irctc.gateway.filter;

import com.irctc.gateway.cb.CircuitBreakerRegistry;
import com.irctc.gateway.cb.CustomCircuitBreaker;
import com.irctc.gateway.proxy.ProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global Routing Filter that intercepts matched route execution,
 * applies our Custom Distributed Circuit Breaker, and forwards requests via ProxyService.
 *
 * Runs BEFORE the default NettyRoutingFilter (order = 9999).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProxyFilter implements GlobalFilter, Ordered {

    private final ProxyService proxyService;
    private final CircuitBreakerRegistry cbRegistry;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route == null) {
            // No matched route -> Allow pipeline to proceed (will trigger 404 handler)
            return chain.filter(exchange);
        }

        String serviceName = getServiceNameFromRoute(route);
        String serviceUrl = route.getUri().toString();

        // Register WebClient proxy if not already done (lazy creation / proxy setup)
        proxyService.createProxy(serviceName, serviceUrl);

        CustomCircuitBreaker cb = cbRegistry.getBreaker(serviceName);

        // ── 1. Circuit Breaker State Check ──────────────────────────────────
        if (!cb.allowRequest()) {
            log.warn("[ProxyFilter] Blocked request to service: {} | Circuit is OPEN (Fail Fast)", serviceName);
            
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");

            String body = """
                    {"success":false,"message":"Service is down. Circuit breaker is OPEN for %s","errorCode":"SERVICE_UNAVAILABLE"}
                    """.formatted(serviceName);

            var buffer = response.bufferFactory().wrap(body.getBytes());
            return response.writeWith(Mono.just(buffer));
        }

        // ── 2. Request Capturing & Non-blocking Forwarding ──────────────────
        HttpMethod method = exchange.getRequest().getMethod();
        String path = exchange.getRequest().getURI().getRawPath();
        HttpHeaders headers = exchange.getRequest().getHeaders();

        // Extract raw request body bytes in a non-blocking manner
        return DataBufferUtils.join(exchange.getRequest().getBody())
            .map(dataBuffer -> {
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                DataBufferUtils.release(dataBuffer);
                return bytes;
            })
            .defaultIfEmpty(new byte[0])
            .flatMap(bodyBytes -> {
                Object data = bodyBytes.length > 0 ? bodyBytes : null;

                // Execute reactive forwarding
                return proxyService.forwardRequest(serviceUrl, path, method, data, headers, cb)
                    .flatMap(responseEntity -> {
                        ServerHttpResponse clientResponse = exchange.getResponse();
                        
                        // Set status code and copy headers from downstream
                        clientResponse.setStatusCode(responseEntity.getStatusCode());
                        clientResponse.getHeaders().addAll(responseEntity.getHeaders());

                        // Write response body payload back to client
                        byte[] respBody = responseEntity.getBody() != null ? responseEntity.getBody() : new byte[0];
                        var buffer = clientResponse.bufferFactory().wrap(respBody);
                        
                        return clientResponse.writeWith(Mono.just(buffer));
                    });
            })
            .onErrorResume(throwable -> {
                if (throwable instanceof org.springframework.web.reactive.function.client.WebClientResponseException wcre) {
                    // Downstream returned a valid HTTP error (4xx client validation or 5xx server exception)
                    ServerHttpResponse clientResponse = exchange.getResponse();
                    clientResponse.setStatusCode(wcre.getStatusCode());
                    clientResponse.getHeaders().addAll(wcre.getHeaders());

                    // Record success or failure in circuit breaker based on status code
                    if (wcre.getStatusCode().is5xxServerError()) {
                        log.error("[ProxyFilter] Downstream service: {} returned 5xx Server Error: {}", serviceName, wcre.getMessage());
                        cb.recordFailure(wcre);
                    } else {
                        // 4xx errors represent client validations (like username already taken), indicating the service is alive!
                        cb.recordSuccess();
                    }

                    byte[] errorBody = wcre.getResponseBodyAsByteArray();
                    var buffer = clientResponse.bufferFactory().wrap(errorBody);
                    return clientResponse.writeWith(Mono.just(buffer));
                }

                // Log full stack trace for real connection/network exceptions (e.g. connection refused, network timeouts)
                log.error("[ProxyFilter] Error forwarding request to service: {} | Reason: {}", serviceName, throwable.getMessage(), throwable);
                cb.recordFailure(throwable);
                
                ServerHttpResponse clientResponse = exchange.getResponse();
                clientResponse.setStatusCode(HttpStatus.BAD_GATEWAY);
                clientResponse.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");

                String body = """
                        {"success":false,"message":"Downstream request failed for service: %s","errorCode":"BAD_GATEWAY"}
                        """.formatted(serviceName);
                
                var buffer = clientResponse.bufferFactory().wrap(body.getBytes());
                return clientResponse.writeWith(Mono.just(buffer));
            });
    }

    /**
     * Map Route ID to unified downstream service name.
     */
    private String getServiceNameFromRoute(Route route) {
        String routeId = route.getId();
        if (routeId.startsWith("user-service")) {
            return "user-service";
        }
        if (routeId.startsWith("notification-service")) {
            return "notification-service";
        }
        if (routeId.startsWith("search-service")) {
            return "search-service";
        }
        if (routeId.startsWith("booking-service")) {
            return "booking-service";
        }
        if (routeId.startsWith("payment-service")) {
            return "payment-service";
        }
        return routeId;
    }

    /**
     * Run just before NettyRoutingFilter (which runs at Ordered.LOWEST_PRECEDENCE / order = 10000).
     * Order 9999 allows us to intercept the request and complete the exchange ourselves.
     */
    @Override
    public int getOrder() {
        return 9999;
    }
}
