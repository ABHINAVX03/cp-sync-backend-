package com.cpsync.cpsync_backend.service;

import com.cpsync.cpsync_backend.dto.ContestDto;
import com.cpsync.cpsync_backend.model.Platform;
import com.cpsync.cpsync_backend.model.SyncedEvent;
import com.cpsync.cpsync_backend.model.User;
import com.cpsync.cpsync_backend.model.UserPlatformPreference;
import com.cpsync.cpsync_backend.repository.SyncedEventRepository;
import com.cpsync.cpsync_backend.repository.UserPlatformPreferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * Syncs new contests for a user.  No transaction wraps the whole method –
     * each successful calendar call is persisted immediately so that partial
     * failures don't create duplicates.
     */
    public int syncContestsForUser(User user) {
        Set<Platform> enabledPlatforms = platformPreferenceRepository
                .findByUserIdAndEnabledTrue(user.getId())
                .stream()
                .map(UserPlatformPreference::getPlatform)
                .collect(Collectors.toSet());

        if (enabledPlatforms.isEmpty()) {
            return 0;
        }

        var contests = aggregatorService.fetchContestsForPlatforms(enabledPlatforms);
        Set<String> alreadySynced = syncedEventRepository.findContestKeysByUserId(user.getId());

        int syncedCount = 0;
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

                // Persist one at a time – prevents duplicates from partial failure
                saveEvent(event);
                syncedCount++;

            } catch (GoogleCalendarService.CalendarSyncException | IllegalStateException e) {
                // Calendar permanent failure or revoked access – log, continue
                failedCount++;
                log.error("[SyncService] Skipping contest {} for user {}: {}",
                        key, user.getId(), e.getMessage());
            } catch (Exception e) {
                // Catch unexpected errors to keep the loop going
                failedCount++;
                log.error("[SyncService] Unexpected error syncing contest {} for user {}: {}",
                        key, user.getId(), e.getMessage());
            }
        }

        if (failedCount > 0) {
            log.warn("[SyncService] User {} — {} contests failed to sync", user.getId(), failedCount);
        }

        return syncedCount;
    }

    @Transactional
    private void saveEvent(SyncedEvent event) {
        try {
            syncedEventRepository.save(event);
        } catch (DataIntegrityViolationException e) {
            // Race condition – the key was inserted between our check and now.
            // Log and move on; the Google event already exists but it's a duplicate
            // that the next cleanup will eventually remove.
            log.warn("[SyncService] Duplicate contest key {} for user {} ignored.",
                    event.getContestKey(), event.getUser().getId());
        }
    }
}