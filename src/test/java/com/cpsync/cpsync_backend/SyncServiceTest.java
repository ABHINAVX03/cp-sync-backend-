package com.cpsync.cpsync_backend;

import com.cpsync.cpsync_backend.dto.ContestDto;
import com.cpsync.cpsync_backend.model.Platform;
import com.cpsync.cpsync_backend.model.SyncedEvent;
import com.cpsync.cpsync_backend.model.User;
import com.cpsync.cpsync_backend.model.UserPlatformPreference;
import com.cpsync.cpsync_backend.repository.SyncedEventRepository;
import com.cpsync.cpsync_backend.repository.UserPlatformPreferenceRepository;
import com.cpsync.cpsync_backend.service.ContestAggregatorService;
import com.cpsync.cpsync_backend.service.GoogleCalendarService;
import com.cpsync.cpsync_backend.service.SyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

    @Mock
    ContestAggregatorService aggregatorService;

    @Mock
    GoogleCalendarService calendarService;

    @Mock
    SyncedEventRepository syncedEventRepository;

    @Mock
    UserPlatformPreferenceRepository platformPreferenceRepository;

    @InjectMocks
    SyncService syncService;

    private User user;
    private ContestDto contest;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        contest = new ContestDto(
                "CODEFORCES",
                "1234",
                "Test Round",
                Instant.now().plusSeconds(3600),
                7200,
                "https://codeforces.com/contest/1234"
        );
    }

    @Test
    void syncContestsForUser_newContest_createsEventAndSaves() {
        stubEnabledCodeforcesPlatform();
        when(aggregatorService.fetchContestsForPlatforms(anySet()))
                .thenReturn(List.of(contest));
        when(syncedEventRepository.findContestKeysByUserId(1L))
                .thenReturn(Set.of());
        when(calendarService.createContestEvent(eq(user), eq(contest)))
                .thenReturn("google-event-id-123");

        int result = syncService.syncContestsForUser(user);

        assertEquals(1, result);
        verify(syncedEventRepository).saveAll(argThat(list -> {
            List<SyncedEvent> events = (List<SyncedEvent>) list;
            return events.size() == 1
                    && events.get(0).getContestKey().equals("CODEFORCES_1234");
        }));
    }

    @Test
    void syncContestsForUser_alreadySynced_skipsContest() {
        stubEnabledCodeforcesPlatform();
        when(aggregatorService.fetchContestsForPlatforms(anySet()))
                .thenReturn(List.of(contest));
        when(syncedEventRepository.findContestKeysByUserId(1L))
                .thenReturn(Set.of("CODEFORCES_1234"));

        int result = syncService.syncContestsForUser(user);

        assertEquals(0, result);
        verify(calendarService, never()).createContestEvent(any(), any());
    }

    @Test
    void syncContestsForUser_calendarFails_continuesAndCountsFailure() {
        stubEnabledCodeforcesPlatform();
        ContestDto contest2 = new ContestDto(
                "CODEFORCES",
                "5678",
                "Another Round",
                Instant.now().plusSeconds(7200),
                7200,
                "https://codeforces.com/contest/5678"
        );

        when(aggregatorService.fetchContestsForPlatforms(anySet()))
                .thenReturn(List.of(contest, contest2));
        when(syncedEventRepository.findContestKeysByUserId(1L)).thenReturn(Set.of());
        when(calendarService.createContestEvent(eq(user), eq(contest)))
                .thenThrow(new GoogleCalendarService.CalendarSyncException("API down", null));
        when(calendarService.createContestEvent(eq(user), eq(contest2)))
                .thenReturn("google-event-id-5678");

        int result = syncService.syncContestsForUser(user);

        assertEquals(1, result);
    }

    @Test
    void syncContestsForUser_noEnabledPlatforms_returnsZero() {
        when(platformPreferenceRepository.findByUserIdAndEnabledTrue(1L))
                .thenReturn(List.of());

        int result = syncService.syncContestsForUser(user);

        assertEquals(0, result);
        verifyNoInteractions(aggregatorService);
    }

    private void stubEnabledCodeforcesPlatform() {
        UserPlatformPreference pref = new UserPlatformPreference();
        pref.setUser(user);
        pref.setPlatform(Platform.CODEFORCES);
        pref.setEnabled(true);

        when(platformPreferenceRepository.findByUserIdAndEnabledTrue(1L))
                .thenReturn(List.of(pref));
    }
}
