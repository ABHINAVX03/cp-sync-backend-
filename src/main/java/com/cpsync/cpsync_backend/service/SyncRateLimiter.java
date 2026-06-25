package com.cpsync.cpsync_backend.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-user rate limiter using Bucket4j (in-memory token bucket).
 *
 * Why in-memory instead of Redis?
 * On Render free tier you don't have Redis. ConcurrentHashMap works fine
 * for a single-instance deployment, which is exactly what free tier gives you.
 * If you ever scale to multiple instances, swap the map for a Redis-backed
 * ProxyManager from bucket4j-redis.
 *
 * Policy: each user gets 1 manual sync per 5 minutes.
 * This prevents someone hammering POST /api/sync in a loop, which would
 * burn through the Google Calendar API quota fast.
 */
@Service
public class SyncRateLimiter {

    private static final int TOKENS_PER_WINDOW = 1;
    private static final Duration WINDOW = Duration.ofMinutes(5);

    // Lazily creates a bucket per user. ConcurrentHashMap is thread-safe for
    // concurrent reads/writes, and computeIfAbsent is atomic.
    private final ConcurrentHashMap<Long, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Returns true if the user is allowed to sync right now.
     * Consumes a token if allowed; otherwise returns false immediately (no blocking).
     */
    public boolean tryConsume(Long userId) {
        Bucket bucket = buckets.computeIfAbsent(userId, this::newBucket);
        return bucket.tryConsume(1);
    }

    /**
     * How many seconds until the user's next sync is allowed.
     * Useful for returning a Retry-After header to the frontend.
     */
    public long secondsUntilRefill(Long userId) {
        Bucket bucket = buckets.get(userId);
        if (bucket == null) return 0;
        long nanosAvailable = bucket.getAvailableTokens();
        if (nanosAvailable > 0) return 0;
        // Bucket4j doesn't expose next-refill time directly on the simple API,
        // so we return the full window as a safe upper bound.
        return WINDOW.getSeconds();
    }

    private Bucket newBucket(Long ignored) {
        Bandwidth limit = Bandwidth.classic(
                TOKENS_PER_WINDOW,
                Refill.intervally(TOKENS_PER_WINDOW, WINDOW)
        );
        return Bucket.builder().addLimit(limit).build();
    }
}