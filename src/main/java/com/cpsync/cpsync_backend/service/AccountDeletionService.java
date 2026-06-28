package com.cpsync.cpsync_backend.service;

import com.cpsync.cpsync_backend.model.User;
import com.cpsync.cpsync_backend.repository.SyncedEventRepository;
import com.cpsync.cpsync_backend.repository.UserPlatformPreferenceRepository;
import com.cpsync.cpsync_backend.repository.UserRepository;
import com.cpsync.cpsync_backend.security.UserDenyList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class AccountDeletionService {

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionService.class);

    private final UserRepository userRepository;
    private final SyncedEventRepository syncedEventRepository;
    private final UserPlatformPreferenceRepository platformPreferenceRepository;
    private final GoogleCalendarService calendarService;
    private final UserDenyList userDenyList;
    private final ThreadPoolTaskExecutor ioTaskExecutor;   // same executor used for fetchers

    public AccountDeletionService(UserRepository userRepository,
                                  SyncedEventRepository syncedEventRepository,
                                  UserPlatformPreferenceRepository platformPreferenceRepository,
                                  GoogleCalendarService calendarService,
                                  UserDenyList userDenyList,
                                  ThreadPoolTaskExecutor ioTaskExecutor) {
        this.userRepository = userRepository;
        this.syncedEventRepository = syncedEventRepository;
        this.platformPreferenceRepository = platformPreferenceRepository;
        this.calendarService = calendarService;
        this.userDenyList = userDenyList;
        this.ioTaskExecutor = ioTaskExecutor;
    }

    /**
     * Deletes the user account and all related data.
     * 1. Removes every Google Calendar event CPSync created (parallel).
     * 2. Deletes all DB records in one short transaction.
     * 3. Deny‑lists the user ID.
     */
    public void deleteAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Step 1: Remove Google Calendar events (parallel, outside any DB txn)
        List<String> eventIds = syncedEventRepository.findGoogleEventIdsByUserId(userId);

        List<CompletableFuture<Void>> deletions = eventIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(id -> CompletableFuture.runAsync(() -> {
                    try {
                        calendarService.deleteEvent(user, id);
                    } catch (GoogleCalendarService.CalendarSyncException e) {
                        log.warn("Failed to delete calendar event {} for user {}: {}", id, userId, e.getMessage());
                    }
                }, ioTaskExecutor))
                .toList();

        // Wait for all deletions to finish (with a generous timeout)
        CompletableFuture.allOf(deletions.toArray(new CompletableFuture[0])).join();

        log.info("Calendar cleanup finished for user {}", userId);

        // Step 2: Atomic DB cleanup
        deleteUserRecords(userId, user);

        // Step 3: Invalidate any remaining JWTs
        userDenyList.deny(userId);
    }

    @Transactional
    void deleteUserRecords(Long userId, User user) {
        syncedEventRepository.deleteByUserId(userId);
        platformPreferenceRepository.deleteByUserId(userId);
        userRepository.delete(user);
    }
}