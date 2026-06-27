package com.cpsync.cpsync_backend.service;

import com.cpsync.cpsync_backend.model.User;
import com.cpsync.cpsync_backend.repository.SyncedEventRepository;
import com.cpsync.cpsync_backend.repository.UserPlatformPreferenceRepository;
import com.cpsync.cpsync_backend.repository.UserRepository;
import com.cpsync.cpsync_backend.security.UserDenyList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountDeletionService {

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionService.class);

    private final UserRepository userRepository;
    private final SyncedEventRepository syncedEventRepository;
    private final UserPlatformPreferenceRepository platformPreferenceRepository;
    private final GoogleCalendarService calendarService;
    private final UserDenyList userDenyList;

    public AccountDeletionService(UserRepository userRepository,
                                  SyncedEventRepository syncedEventRepository,
                                  UserPlatformPreferenceRepository platformPreferenceRepository,
                                  GoogleCalendarService calendarService,
                                  UserDenyList userDenyList) {
        this.userRepository = userRepository;
        this.syncedEventRepository = syncedEventRepository;
        this.platformPreferenceRepository = platformPreferenceRepository;
        this.calendarService = calendarService;
        this.userDenyList = userDenyList;
    }

    /**
     * Deletes the user account and all related data.
     * 1. Removes every Google Calendar event that CPSync created.
     * 2. Deletes synced_events, platform preferences, and the user row.
     * 3. Deny‑lists the user ID so any remaining JWTs are instantly invalid.
     */
    @Transactional
    public void deleteAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Step 1: Remove Google Calendar events
        List<String> eventIds = syncedEventRepository.findGoogleEventIdsByUserId(userId);
        int deletedFromCalendar = 0;
        for (String eventId : eventIds) {
            if (eventId != null && !eventId.isBlank()) {
                try {
                    calendarService.deleteEvent(user, eventId);
                    deletedFromCalendar++;
                } catch (GoogleCalendarService.CalendarSyncException e) {
                    log.warn("Failed to delete calendar event {} for user {}: {}", eventId, userId, e.getMessage());
                }
            }
        }
        log.info("Deleted {} out of {} calendar events for user {}", deletedFromCalendar, eventIds.size(), userId);

        // Step 2: Remove database records
        syncedEventRepository.deleteByUserId(userId);
        platformPreferenceRepository.deleteByUserId(userId);
        userRepository.delete(user);

        // Step 3: Invalidate any remaining JWTs
        userDenyList.deny(userId);
    }
}