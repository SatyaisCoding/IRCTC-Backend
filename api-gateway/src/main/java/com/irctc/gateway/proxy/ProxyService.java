package com.irctc.gateway.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.irctc.gateway.cb.CustomCircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Senior-grade non-blocking reactive HTTP forwarding proxy service using WebClient.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProxyService {

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, WebClient> clients = new ConcurrentHashMap<>();

    /**
     * Initializes a caching proxy WebClient for a microservice if it doesn't already exist.
     */
    public void createProxy(String serviceName, String serviceUrl) {
        clients.computeIfAbsent(serviceName, name -> {
            log.info("[ProxyService] Registering proxy WebClient for service: {} -> {}", serviceName, serviceUrl);
            return WebClient.builder()
                    .baseUrl(serviceUrl)
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB limit
                    .build();
        });
    }

    /**
     * Get or create a client dynamically based on the target base URL.
     */
    private WebClient getClient(String serviceUrl) {
        return clients.computeIfAbsent(serviceUrl, url -> {
            log.info("[ProxyService] Dynamic registration of WebClient for URL: {}", url);
            return WebClient.builder()
                    .baseUrl(url)
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                    .build();
        });
    }

    /**
     * Core forwarding proxy function.
     * Reactively executes downstream service HTTP request and intercepts results to feed Circuit Breaker.
     */
    @SuppressWarnings("unchecked")
    public Mono<ResponseEntity<byte[]>> forwardRequest(
            String serviceUrl, 
            String path, 
            HttpMethod method, 
            Object data, 
            HttpHeaders headers, 
            CustomCircuitBreaker cb) {
        
        WebClient client = getClient(serviceUrl);

        // ── 1. URI Resolution & Method-Specific Data Mapping ────────────────
        URI finalUri;
        try {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(serviceUrl).path(path);
            
            if ((method == HttpMethod.GET || method == HttpMethod.DELETE) && data != null) {
                // GET/DELETE -> Map data properties to URL Query Parameters
                Map<String, Object> params = Map.of();
                try {
                    if (data instanceof Map) {
                        params = (Map<String, Object>) data;
                    } else {
                        String jsonStr;
                        if (data instanceof byte[] bytes) {
                            jsonStr = new String(bytes, java.nio.charset.StandardCharsets.UTF_8).trim();
                        } else {
                            jsonStr = data.toString().trim();
                        }

                        if (jsonStr.startsWith("{")) {
                            params = objectMapper.readValue(jsonStr, Map.class);
                        }
                    }
                } catch (Exception parseEx) {
                    log.warn("[ProxyService] Ignored non-map/non-json raw query payload during GET/DELETE parameter parsing: {}", parseEx.getMessage());
                }

                params.forEach((key, val) -> {
                    if (val != null) {
                        uriBuilder.queryParam(key, val.toString());
                    }
                });
            }
            finalUri = uriBuilder.build().toUri();
        } catch (Exception e) {
            log.error("[ProxyService] Error mapping parameters for method {}: {}", method, e.getMessage());
            return Mono.error(new IllegalArgumentException("Failed to construct URI parameters: " + e.getMessage()));
        }

        log.debug("[ProxyService] Forwarding HTTP {} to URL: {} with parameters/body", method, finalUri);

        // ── 2. Request Customization ──────────────────────────────────────────
        WebClient.RequestBodySpec requestSpec = client.method(method)
                .uri(finalUri)
                .headers(h -> {
                    h.addAll(headers);
                    // Ensure host header matches downstream
                    h.remove(HttpHeaders.HOST);
                    // Prevent HTTP smuggling/deserialization mismatch on GET/DELETE requests
                    if (method == HttpMethod.GET || method == HttpMethod.DELETE) {
                        h.remove(HttpHeaders.CONTENT_LENGTH);
                    }
                });

        WebClient.RequestHeadersSpec<?> headersSpec = requestSpec;
        if ((method != HttpMethod.GET && method != HttpMethod.DELETE) && data != null) {
            headersSpec = requestSpec.bodyValue(data);
        }

        // ── 3. Reactive HTTP Exchange & Circuit Breaker Tracking ─────────────
        return headersSpec.retrieve()
                .toEntity(byte[].class)
                .doOnSuccess(response -> {
                    if (response != null && response.getStatusCode().is5xxServerError()) {
                        // Only 5xx Server errors count as circuit breaker failure triggers
                        cb.recordFailure(new RuntimeException("Service " + cb.getServiceName() + " returned 5xx status: " + response.getStatusCode().value()));
                    } else {
                        // Success or standard client error (4xx) means service is responsive
                        cb.recordSuccess();
                    }
                })
                .doOnError(err -> {
                    if (err instanceof org.springframework.web.reactive.function.client.WebClientResponseException wcre) {
                        // 4xx client errors (e.g. 400 Bad Request) represent successful API communications
                        // and are NOT circuit breaker failures. Only 5xx server errors count as failures.
                        if (wcre.getStatusCode().is5xxServerError()) {
                            log.error("[ProxyService] Downstream service: {} returned 5xx Server Error: {}", cb.getServiceName(), wcre.getMessage());
                            cb.recordFailure(wcre);
                        }
                    } else {
                        // Captures actual network exceptions (timeout, host unreachable, connection refused)
                        log.error("[ProxyService] Forward request encountered exception for service: {}. Error: {}", cb.getServiceName(), err.getMessage());
                        cb.recordFailure(err);
                    }
                });
    }
}
