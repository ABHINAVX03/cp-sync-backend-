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
import org.springframework.stereotype.Service;

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
     * Syncs all new (not-yet-synced) contests for a user's enabled platforms to their calendar.
     * Safe to call repeatedly — already-synced contests are skipped automatically.
     * Returns the count of newly synced contests.
     */
    public int syncContestsForUser(User user) {
        Set<Platform> enabledPlatforms = platformPreferenceRepository
                .findByUserIdAndEnabledTrue(user.getId())
                .stream()
                .map(UserPlatformPreference::getPlatform)
                .collect(Collectors.toSet());

        if (enabledPlatforms.isEmpty()) {
            return 0; // nothing to sync if user hasn't enabled any platforms
        }

        List<ContestDto> contests = aggregatorService.fetchContestsForPlatforms(enabledPlatforms);

        // One query for all of this user's already-synced contest keys, instead of
        // one existsBy... query per contest.
        Set<String> alreadySyncedKeys = syncedEventRepository.findContestKeysByUserId(user.getId());

        int syncedCount = 0;

        for (ContestDto contest : contests) {
            String contestKey = contest.getContestKey();

            if (alreadySyncedKeys.contains(contestKey)) {
                continue;
            }

            try {
                String googleEventId = calendarService.createContestEvent(user, contest);

                SyncedEvent syncedEvent = new SyncedEvent();
                syncedEvent.setUser(user);
                syncedEvent.setContestKey(contestKey);
                syncedEvent.setGoogleEventId(googleEventId);
                syncedEventRepository.save(syncedEvent);

                syncedCount++;

            } catch (Exception e) {
                // One contest failing to sync (e.g. transient Google API error) shouldn't
                // stop the rest of this user's contests from syncing.
                log.error("Failed to sync contest " + contestKey +
                        " for user " + user.getId() + ": " + e.getMessage());
            }
        }

        return syncedCount;
    }
}