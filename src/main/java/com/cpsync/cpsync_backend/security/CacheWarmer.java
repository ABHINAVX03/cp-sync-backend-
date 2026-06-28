package com.cpsync.cpsync_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Warms the contest caches on startup so the first dashboard load
 * doesn't time out while the fetchers hit external APIs.
 */
@Component
public class CacheWarmer {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmer.class);

    private final ContestAggregatorService aggregatorService;

    public CacheWarmer(ContestAggregatorService aggregatorService) {
        this.aggregatorService = aggregatorService;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void warmCaches() {
        log.info("[CacheWarmer] Pre‑fetching contests to warm caches…");
        try {
            int count = aggregatorService.fetchAllContests().size();
            log.info("[CacheWarmer] Cache warm complete — {} contests cached", count);
        } catch (Exception e) {
            log.warn("[CacheWarmer] Cache warm failed, first user request may be slow", e);
        }
    }
}