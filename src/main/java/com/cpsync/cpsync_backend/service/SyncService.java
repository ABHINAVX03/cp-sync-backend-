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
     * Syncs new contests for a user.  Uses the unique constraint on
     * synced_events as an atomic lock – the row is created BEFORE the
     * Google Calendar call, so concurrent syncs never create duplicates.
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

            // 1) Placeholder – atomic claim via the unique constraint
            SyncedEvent placeholder = new SyncedEvent();
            placeholder.setUser(user);
            placeholder.setContestKey(key);
            placeholder.setGoogleEventId(null);   // not created yet

            try {
                syncedEventRepository.save(placeholder);
            } catch (DataIntegrityViolationException e) {
                // Another thread already claimed this contest – skip
                continue;
            }

            // 2) Now that we've claimed it, do the external call
            try {
                String googleEventId = calendarService.createContestEvent(user, contest);
                placeholder.setGoogleEventId(googleEventId);
                syncedEventRepository.save(placeholder);
                syncedCount++;
            } catch (GoogleCalendarService.CalendarSyncException | IllegalStateException e) {
                // Permanent failure – release the claim so we can retry later
                syncedEventRepository.delete(placeholder);
                failedCount++;
                log.error("[SyncService] Skipping contest {} for user {}: {}",
                        key, user.getId(), e.getMessage());
            } catch (Exception e) {
                syncedEventRepository.delete(placeholder);
                failedCount++;
                log.error("[SyncService] Unexpected error for contest {} user {}: {}",
                        key, user.getId(), e.getMessage());
            }
        }

        if (failedCount > 0) {
            log.warn("[SyncService] User {} — {} contests failed to sync", user.getId(), failedCount);
        }

        return syncedCount;
    }
}