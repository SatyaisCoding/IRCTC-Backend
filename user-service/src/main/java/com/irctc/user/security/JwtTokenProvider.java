package com.irctc.user.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenExpirationMs;
    private final StringRedisTemplate stringRedisTemplate;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.accessTokenExpirationMs}") long accessTokenExpirationMs,
            StringRedisTemplate stringRedisTemplate) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .claim("role", userPrincipal.getAuthorities().iterator().next().getAuthority())
                .claim("userId", userPrincipal.getId())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String generateTokenFromUsername(String username, String role, Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            // 1. Check if token is blacklisted in Redis (e.g., from a logout action)
            Boolean isBlacklisted = stringRedisTemplate.hasKey("blacklist:" + authToken);
            if (Boolean.TRUE.equals(isBlacklisted)) {
                log.warn("Rejected blacklisted access token: {}", authToken.substring(0, 10) + "...");
                return false;
            }

            // 2. Parse and validate claims signature and expiration
            Jwts.parser().verifyWith(key).build().parseSignedClaims(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        }
        return false;
    }

    public void blacklistToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            long remainingTimeMs = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (remainingTimeMs > 0) {
                stringRedisTemplate.opsForValue().set(
                        "blacklist:" + token,
                        "true",
                        remainingTimeMs,
                        TimeUnit.MILLISECONDS
                );
                log.info("Blacklisted token ending in: {}ms", remainingTimeMs);
            }
        } catch (Exception ex) {
            log.error("Could not blacklist token: {}", ex.getMessage());
        }
    }
}
