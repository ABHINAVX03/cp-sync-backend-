package com.cpsync.cpsync_backend.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * In-memory deny list for deactivated users.
 *
 * When a user's sync is paused, their userId is added here.
 * JwtAuthFilter checks this list — even if their JWT is still valid,
 * a deactivated user cannot access protected endpoints.
 *
 * TTL matches the JWT expiry (7 days). On resume, they are removed
 * from the list and their existing JWT works again immediately.
 *
 * Scale note: for multi-instance deployments, swap this for a Redis SET.
 */
@Component
public class UserDenyList {

    // Key = userId, value = ignored (we only care about presence)
    private final Cache<Long, Boolean> denied = Caffeine.newBuilder()
            .expireAfterWrite(7, TimeUnit.DAYS)
            .maximumSize(100_000)
            .build();

    public void deny(Long userId) {
        denied.put(userId, Boolean.TRUE);
    }

    public void allow(Long userId) {
        denied.invalidate(userId);
    }

    public boolean isDenied(Long userId) {
        return denied.getIfPresent(userId) != null;
    }
}