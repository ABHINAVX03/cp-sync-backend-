package com.cpsync.cpsync_backend;

import com.cpsync.cpsync_backend.service.SyncRateLimiter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncRateLimiterTest {

    private final SyncRateLimiter rateLimiter = new SyncRateLimiter();

    @Test
    void firstRequest_allowed() {
        assertTrue(rateLimiter.tryConsume(1L));
    }

    @Test
    void secondRequestImmediately_blocked() {
        rateLimiter.tryConsume(1L);
        assertFalse(rateLimiter.tryConsume(1L));
    }

    @Test
    void differentUsers_independentLimits() {
        assertTrue(rateLimiter.tryConsume(1L));
        assertTrue(rateLimiter.tryConsume(2L));
    }

    @Test
    void secondsUntilRefill_afterConsumption_returnsPositive() {
        rateLimiter.tryConsume(10L);
        rateLimiter.tryConsume(10L);
        assertTrue(rateLimiter.secondsUntilRefill(10L) > 0);
    }
}
