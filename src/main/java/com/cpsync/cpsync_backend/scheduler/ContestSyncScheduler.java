package com.cpsync.cpsync_backend.scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cpsync.cpsync_backend.model.User;
import com.cpsync.cpsync_backend.repository.UserRepository;
import com.cpsync.cpsync_backend.service.SyncService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ContestSyncScheduler {
    private static final Logger log = LoggerFactory.getLogger(ContestSyncScheduler.class);
    private final UserRepository userRepository;
    private final SyncService syncService;

    public ContestSyncScheduler(UserRepository userRepository, SyncService syncService) {
        this.userRepository = userRepository;
        this.syncService = syncService;
    }

    /**
     * Runs once daily at 3:00 AM server time.
     * Loops over every active user and syncs new contests to their calendar.
     * One user's failure never stops the rest of the batch.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void runDailySync() {
        List<User> activeUsers = userRepository.findByActiveTrue();

        log.info("[ContestSyncScheduler] Starting daily sync for " + activeUsers.size() + " active user(s)");

        int totalSynced = 0;
        int failedUsers = 0;

        for (User user : activeUsers) {
            try {
                int synced = syncService.syncContestsForUser(user);
                totalSynced += synced;
            } catch (Exception e) {
                failedUsers++;
                log.error("[ContestSyncScheduler] Failed to sync user " + user.getId() + ": " + e.getMessage());
            }
        }

        log.info("[ContestSyncScheduler] Daily sync complete. Total new events: " + totalSynced +
                ", failed users: " + failedUsers + " / " + activeUsers.size());
    }
}