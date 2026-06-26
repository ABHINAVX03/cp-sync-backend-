package com.cpsync.cpsync_backend.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Per-user rate limiter — 1 manual sync per 5 minutes.
 *
 * FIXED: replaced ConcurrentHashMap (unbounded) with Caffeine cache
 * (1-hour idle eviction). Prevents a memory leak where every user
 * who ever synced would accumulate an immortal Bucket object.
 */
@Service
public class SyncRateLimiter {

    private static final int TOKENS_PER_WINDOW = 1;
    private static final Duration WINDOW = Duration.ofMinutes(5);

    private final Cache<Long, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .maximumSize(50_000)
            .build();

    public boolean tryConsume(Long userId) {
        Bucket bucket = buckets.get(userId, id -> newBucket());
        return bucket.tryConsume(1);
    }

    public long secondsUntilRefill(Long userId) {
        Bucket bucket = buckets.getIfPresent(userId);
        if (bucket == null || bucket.getAvailableTokens() > 0) return 0;
        return WINDOW.getSeconds();
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(
                TOKENS_PER_WINDOW,
                Refill.intervally(TOKENS_PER_WINDOW, WINDOW)
        );
        return Bucket.builder().addLimit(limit).build();
    }
}