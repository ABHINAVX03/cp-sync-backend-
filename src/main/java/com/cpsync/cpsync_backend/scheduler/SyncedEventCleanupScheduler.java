package com.cpsync.cpsync_backend.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Deletes synced_events rows older than 90 days.
 *
 * Without this, the table grows forever. The findContestKeysByUserId()
 * query loads ALL rows for a user into memory on every sync — after 2 years
 * of use this becomes a multi-hundred-row Set per user.
 *
 * Runs at 4:00 AM UTC daily (1 hour after the main sync finishes).
 */
@Component
public class SyncedEventCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncedEventCleanupScheduler.class);

    private final JdbcTemplate jdbcTemplate;

    public SyncedEventCleanupScheduler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(cron = "0 0 4 * * *", zone = "UTC")
    public void cleanupOldSyncedEvents() {
        int deleted = jdbcTemplate.update(
                "DELETE FROM synced_events WHERE synced_at < NOW() - INTERVAL '90 days'"
        );
        log.info("[Cleanup] Deleted {} old synced_events rows", deleted);
    }
}