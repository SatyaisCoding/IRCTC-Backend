package com.irctc.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

/**
 * Stateless JWT utility used by the gateway to:
 *  - Validate the token's signature and expiry
 *  - Extract userId and role claims to forward as headers to downstream services
 *
 * The gateway does NOT issue tokens — that is user-service's responsibility.
 * The same JWT_SECRET must be shared between user-service and api-gateway.
 */
@Slf4j
@Component
public class JwtUtil {

    private final SecretKey signingKey;

    public JwtUtil(@Value("${app.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    // ── Validate ──────────────────────────────────────────────────────────

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException ex) {
            log.warn("[JwtUtil] Invalid token: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("[JwtUtil] Token validation error: {}", ex.getMessage());
        }
        return false;
    }

    // ── Extract Claims ────────────────────────────────────────────────────

    public Long getUserId(String token) {
        Object id = parseClaims(token).get("userId");
        if (id instanceof Integer) return ((Integer) id).longValue();
        if (id instanceof Long)    return (Long) id;
        return Long.parseLong(id.toString());
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String getRole(String token) {
        return (String) parseClaims(token).get("role");
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
