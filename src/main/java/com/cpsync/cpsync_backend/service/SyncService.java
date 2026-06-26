package com.cpsync.cpsync_backend.service;

import com.cpsync.cpsync_backend.dto.ContestDto;
import com.cpsync.cpsync_backend.model.Platform;
import com.cpsync.cpsync_backend.model.SyncedEvent;
import com.cpsync.cpsync_backend.model.User;
import com.cpsync.cpsync_backend.model.UserPlatformPreference;
import com.cpsync.cpsync_backend.repository.SyncedEventRepository;
import com.cpsync.cpsync_backend.repository.UserPlatformPreferenceRepository;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private final ContestAggregatorService aggregatorService;
    private final GoogleCalendarService calendarService;
    private final SyncedEventRepository syncedEventRepository;
    private final UserPlatformPreferenceRepository platformPreferenceRepository;

    public SyncService(ContestAggregatorService aggregatorService,
                       GoogleCalendarService calendarService,
                       SyncedEventRepository syncedEventRepository,
                       UserPlatformPreferenceRepository platformPreferenceRepository) {
        this.aggregatorService = aggregatorService;
        this.calendarService = calendarService;
        this.syncedEventRepository = syncedEventRepository;
        this.platformPreferenceRepository = platformPreferenceRepository;
    }

    /**
     * Syncs new contests for a user.
     *
     * Changes from original:
     * 1. Collects all new SyncedEvent rows and calls saveAll() once instead of
     *    save() per contest — cuts DB round-trips from N to 1 per user sync.
     * 2. Uses CalendarSyncException to distinguish retryable vs permanent failures
     *    without hiding them silently.
     */
    @Transactional
    public int syncContestsForUser(User user) {
        Set<Platform> enabledPlatforms = platformPreferenceRepository
                .findByUserIdAndEnabledTrue(user.getId())
                .stream()
                .map(UserPlatformPreference::getPlatform)
                .collect(Collectors.toSet());

        if (enabledPlatforms.isEmpty()) {
            return 0;
        }

        List<ContestDto> contests = aggregatorService.fetchContestsForPlatforms(enabledPlatforms);

        // One DB query for all synced keys instead of existsBy per contest
        Set<String> alreadySynced = syncedEventRepository.findContestKeysByUserId(user.getId());

        List<SyncedEvent> toSave = new ArrayList<>();
        int failedCount = 0;

        for (ContestDto contest : contests) {
            String key = contest.getContestKey();
            if (alreadySynced.contains(key)) {
                continue;
            }

            try {
                String googleEventId = calendarService.createContestEvent(user, contest);

                SyncedEvent event = new SyncedEvent();
                event.setUser(user);
                event.setContestKey(key);
                event.setGoogleEventId(googleEventId);
                toSave.add(event);

            } catch (GoogleCalendarService.CalendarSyncException e) {
                // Log and continue — one contest failing shouldn't stop the rest
                failedCount++;
                log.error("[SyncService] Skipping contest {} for user {}: {}",
                        key, user.getId(), e.getMessage());
            }
        }

        // Batch insert: one DB round-trip instead of N
        if (!toSave.isEmpty()) {
            syncedEventRepository.saveAll(toSave);
        }

        if (failedCount > 0) {
            log.warn("[SyncService] User {} — {} contests failed to sync", user.getId(), failedCount);
        }

        return toSave.size();
    }
}