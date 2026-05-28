package com.irctc.user.service;

import com.irctc.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Manages user profile caching in Redis.
 *
 * Cache Strategy (Cache-Aside / Lazy Loading):
 * ┌──────────────────────────────────────────────────────────┐
 * │  GET profile                                             │
 * │   1. Check Redis  → HIT  → return immediately (20ms)    │
 * │                  → MISS → fetch PostgreSQL (200ms)       │
 * │                         → store in Redis (TTL 24h)       │
 * │                         → return                         │
 * │                                                          │
 * │  UPDATE / DELETE profile                                 │
 * │   1. Write to PostgreSQL                                 │
 * │   2. Evict / update Redis key immediately                │
 * └──────────────────────────────────────────────────────────┘
 *
 * Key format : user:profile:{userId}
 * TTL        : 24 hours
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCacheService {

    private static final String KEY_PREFIX = "user:profile:";
    private static final long   TTL_HOURS  = 24L;

    private final RedisTemplate<String, Object> redisTemplate;

    // ── Cache key helper ──────────────────────────────────────────────────

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }

    // ── Read ──────────────────────────────────────────────────────────────

    /**
     * Fetch a profile from Redis cache.
     * Returns null on a cache miss — caller must then fetch from DB.
     */
    public UserResponse get(Long userId) {
        try {
            Object cached = redisTemplate.opsForValue().get(key(userId));
            if (cached instanceof UserResponse profile) {
                log.debug("[UserCache] HIT  → user:profile:{}", userId);
                return profile;
            }
        } catch (Exception ex) {
            log.warn("[UserCache] Redis read failed for user:{} — falling back to DB. Error: {}",
                    userId, ex.getMessage());
        }
        log.debug("[UserCache] MISS → user:profile:{}", userId);
        return null;
    }

    // ── Write ─────────────────────────────────────────────────────────────

    /**
     * Store a user profile in Redis with a 24-hour TTL.
     * Called after login and after a DB fetch on cache miss.
     */
    public void put(UserResponse profile) {
        try {
            redisTemplate.opsForValue().set(key(profile.getId()), profile, TTL_HOURS, TimeUnit.HOURS);
            log.debug("[UserCache] STORE → user:profile:{} (TTL {}h)", profile.getId(), TTL_HOURS);
        } catch (Exception ex) {
            log.warn("[UserCache] Redis write failed for user:{} — continuing without cache. Error: {}",
                    profile.getId(), ex.getMessage());
        }
    }

    // ── Evict ─────────────────────────────────────────────────────────────

    /**
     * Remove a user profile from Redis.
     * Called on profile update and account deletion.
     */
    public void evict(Long userId) {
        try {
            redisTemplate.delete(key(userId));
            log.debug("[UserCache] EVICT → user:profile:{}", userId);
        } catch (Exception ex) {
            log.warn("[UserCache] Redis evict failed for user:{} — Error: {}", userId, ex.getMessage());
        }
    }
}
