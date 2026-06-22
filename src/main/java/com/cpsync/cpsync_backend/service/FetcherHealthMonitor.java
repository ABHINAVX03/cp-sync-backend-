package com.cpsync.cpsync_backend.service;

import com.cpsync.cpsync_backend.model.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class FetcherHealthMonitor {

    private static final Logger log = LoggerFactory.getLogger(FetcherHealthMonitor.class);

    // Tracks the last known non-empty result count per platform
    private final ConcurrentHashMap<Platform, AtomicInteger> lastKnownGoodCount = new ConcurrentHashMap<>();

    // Tracks consecutive empty results per platform
    private final ConcurrentHashMap<Platform, AtomicInteger> consecutiveEmptyCount = new ConcurrentHashMap<>();

    public void recordResult(Platform platform, int resultCount) {
        if (resultCount > 0) {
            lastKnownGoodCount.put(platform, new AtomicInteger(resultCount));
            consecutiveEmptyCount.remove(platform);
            return;
        }

        AtomicInteger previousGood = lastKnownGoodCount.get(platform);
        AtomicInteger emptyStreak = consecutiveEmptyCount.computeIfAbsent(platform, p -> new AtomicInteger(0));
        int streak = emptyStreak.incrementAndGet();

        // Only alarm if this platform has historically had data before — avoids false alarms
        // on first-ever run, or genuinely contest-less periods for low-frequency platforms.
        if (previousGood != null) {
            log.warn("[FetcherHealthMonitor] {} returned EMPTY results ({} consecutive). " +
                            "Last known good count was {}. This may indicate the fetcher is broken " +
                            "(e.g. API/HTML structure changed).",
                    platform, streak, previousGood.get());
        }
    }
}