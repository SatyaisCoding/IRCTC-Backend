package com.irctc.gateway.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Global WebFlux Error Handler.
 * Intercepts any unmapped routes (404 NOT_FOUND), connection errors, or general failures,
 * returning a standardized, beautiful JSON response.
 */
@Component
@Order(-2) // High precedence — runs before standard Spring Boot error handlers
@RequiredArgsConstructor
@Slf4j
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String errorCode = "INTERNAL_SERVER_ERROR";
        String message = ex.getMessage();

        // Check if the error is a ResponseStatusException (e.g. 404 NOT_FOUND)
        if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            if (status == HttpStatus.NOT_FOUND) {
                errorCode = "NOT_FOUND";
                message = "Endpoint not found: " + exchange.getRequest().getURI().getPath();
            }
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String jsonResponse = """
                {"success":false,"message":"%s","errorCode":"%s"}
                """.formatted(message, errorCode).trim();

        log.error("[Gateway Error] Intercepted exception [{}]: {} for path: {} -> Status: {}", 
                ex.getClass().getSimpleName(), ex.getMessage(), exchange.getRequest().getURI().getPath(), status);

        DataBuffer buffer = response.bufferFactory().wrap(jsonResponse.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
