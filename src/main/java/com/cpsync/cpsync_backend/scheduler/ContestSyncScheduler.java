package com.cpsync.cpsync_backend.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cpsync.cpsync_backend.model.User;
import com.cpsync.cpsync_backend.repository.UserRepository;
import com.cpsync.cpsync_backend.service.SyncService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ContestSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(ContestSyncScheduler.class);

    // On Render free (512MB RAM), 50 users × ~2MB per user object + calendar call
    // stays safely under 200MB. Tune via env var if you upgrade tiers.
    @Value("${app.sync.batch-size:50}")
    private int batchSize;

    // How long to pause between batches. Gives GC time to breathe and avoids
    // spiking Neon's connection queue.
    @Value("${app.sync.batch-pause-ms:500}")
    private long batchPauseMs;

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
     * Runs daily at 3:00 AM server time.
     *
     * Key change from the old version: we no longer call findByActiveTrue() and
     * load every user into memory at once. Instead we page through users in
     * batches of `batchSize`, process each batch in parallel (bounded pool),
     * then sleep before the next batch. This keeps RAM flat regardless of user count.
     *
     * On Render free tier with 512MB:
     *   - 50 users/batch × ~500KB per sync = ~25MB per batch peak
     *   - Well within limits even at 2k total users (40 batches × 25MB sequential)
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void runDailySync() {
        log.info("[Scheduler] Starting daily sync (batch-size={}, pause={}ms)",
                batchSize, batchPauseMs);

        AtomicInteger totalSynced  = new AtomicInteger(0);
        AtomicInteger totalFailed  = new AtomicInteger(0);
        AtomicInteger totalSkipped = new AtomicInteger(0);
        int page = 0;

        while (true) {
            Pageable pageable = PageRequest.of(page, batchSize);
            List<User> batch = userRepository.findByActiveTrueOrderById(pageable);

            if (batch.isEmpty()) {
                break; // no more users
            }

            log.info("[Scheduler] Processing batch {} ({} users)", page + 1, batch.size());

            List<CompletableFuture<Integer>> futures = batch.stream()
                    .map(user -> CompletableFuture.supplyAsync(() -> {
                        try {
                            return syncService.syncContestsForUser(user);
                        } catch (Exception e) {
                            log.error("[Scheduler] Failed for user {}: {}", user.getId(), e.getMessage());
                            return -1;
                        }
                    }, ioTaskExecutor))
                    .toList();

            for (CompletableFuture<Integer> f : futures) {
                int result = f.join();
                if (result < 0)  totalFailed.incrementAndGet();
                else if (result == 0) totalSkipped.incrementAndGet();
                else totalSynced.addAndGet(result);
            }

            // If this was a partial batch we've reached the end
            if (batch.size() < batchSize) {
                break;
            }

            page++;

            // Sleep between batches — gives Neon's connection pool time to recycle
            // and stops us from hammering the Google Calendar API all at once.
            try {
                TimeUnit.MILLISECONDS.sleep(batchPauseMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[Scheduler] Interrupted between batches, stopping early");
                break;
            }
        }

        log.info("[Scheduler] Done. new_events={} skipped={} failed_users={}",
                totalSynced.get(), totalSkipped.get(), totalFailed.get());
    }
}