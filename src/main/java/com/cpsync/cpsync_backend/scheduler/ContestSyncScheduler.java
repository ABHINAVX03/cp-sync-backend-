package com.cpsync.cpsync_backend.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cpsync.cpsync_backend.model.User;
import com.cpsync.cpsync_backend.repository.UserRepository;
import com.cpsync.cpsync_backend.service.SyncService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class ContestSyncScheduler {
    private static final Logger log = LoggerFactory.getLogger(ContestSyncScheduler.class);
    private final UserRepository userRepository;
    private final SyncService syncService;
    private final ThreadPoolTaskExecutor ioTaskExecutor;

    public ContestSyncScheduler(UserRepository userRepository,
                                SyncService syncService,
                                ThreadPoolTaskExecutor ioTaskExecutor) {
        this.userRepository = userRepository;
        this.syncService = syncService;
        this.ioTaskExecutor = ioTaskExecutor;
    }

    /**
     * Runs once daily at 3:00 AM server time.
     * Syncs every active user in parallel (bounded pool — see app.sync.thread-pool-size).
     * One user's failure never stops the rest of the batch.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void runDailySync() {
        List<User> activeUsers = userRepository.findByActiveTrue();

        log.info("[ContestSyncScheduler] Starting daily sync for {} active user(s)", activeUsers.size());

        List<CompletableFuture<Integer>> futures = activeUsers.stream()
                .map(user -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return syncService.syncContestsForUser(user);
                    } catch (Exception e) {
                        log.error("[ContestSyncScheduler] Failed to sync user " + user.getId() + ": " + e.getMessage());
                        return -1;
                    }
                }, ioTaskExecutor))
                .toList();

        int totalSynced = 0;
        int failedUsers = 0;

        for (CompletableFuture<Integer> future : futures) {
            int result = future.join();
            if (result < 0) {
                failedUsers++;
            } else {
                totalSynced += result;
            }
        }

        log.info("[ContestSyncScheduler] Daily sync complete. Total new events: {}, failed users: {} / {}",
                totalSynced, failedUsers, activeUsers.size());
    }
}