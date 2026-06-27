package com.cpsync.cpsync_backend.service;

import com.cpsync.cpsync_backend.dto.ContestDto;
import com.cpsync.cpsync_backend.model.User;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class GoogleCalendarService {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarService.class);

    @Value("${app.calendar.max-retries:3}")
    private int maxRetries;

    @Value("${app.calendar.backoff-base-ms:1000}")
    private long backoffBaseMs;

    private final GoogleTokenRefreshService tokenRefreshService;
    private static final NetHttpTransport HTTP_TRANSPORT;

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Google HTTP transport", e);
        }
    }

    public GoogleCalendarService(GoogleTokenRefreshService tokenRefreshService) {
        this.tokenRefreshService = tokenRefreshService;
    }

    /**
     * Creates a Google Calendar event for a contest, retrying with exponential
     * backoff on rate-limit (429) or transient server errors (5xx).
     */
    public String createContestEvent(User user, ContestDto contest) {
        String accessToken = tokenRefreshService.getValidAccessToken(user);
        Calendar calendarService = buildCalendarClient(accessToken);
        Event event = buildEvent(contest);

        Exception lastException = null;
        boolean tokenRefreshed = false;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                Event created = calendarService.events()
                        .insert("primary", event)
                        .execute();
                return created.getId();

            } catch (GoogleJsonResponseException e) {
                int status = e.getStatusCode();

                if (status == 401) {
                    if (!tokenRefreshed) {
                        log.warn("[Calendar] 401 for user {} contest {}, refreshing token once",
                                user.getId(), contest.getContestKey());
                        accessToken = tokenRefreshService.getValidAccessToken(user);
                        calendarService = buildCalendarClient(accessToken);
                        tokenRefreshed = true;
                        attempt--;
                        continue;
                    }
                    throw new CalendarSyncException(
                            "Auth permanently failed after token refresh for contest "
                                    + contest.getContestKey()
                                    + " (user " + user.getId() + " may have revoked access)", e);
                }

                if (status == 429 || status >= 500) {
                    lastException = e;
                    long waitMs = backoffBaseMs * (1L << attempt);
                    log.warn("[Calendar] HTTP {} for user {} attempt {}/{}, waiting {}ms",
                            status, user.getId(), attempt + 1, maxRetries + 1, waitMs);
                    sleep(waitMs);
                    continue;
                }

                throw new CalendarSyncException(
                        "Permanent calendar error (HTTP " + status + ") for contest "
                                + contest.getContestKey(), e);

            } catch (Exception e) {
                lastException = e;
                long waitMs = backoffBaseMs * (1L << attempt);
                log.warn("[Calendar] Network error for user {} attempt {}/{}: {}",
                        user.getId(), attempt + 1, maxRetries + 1, e.getMessage());
                sleep(waitMs);
            }
        }

        throw new CalendarSyncException(
                "Gave up after " + maxRetries + " retries for contest "
                        + contest.getContestKey(), lastException);
    }

    /**
     * Deletes a single calendar event by its Google event ID.
     * If the event is already gone (404) we treat it as success.
     */
    public void deleteEvent(User user, String eventId) {
        String accessToken = tokenRefreshService.getValidAccessToken(user);
        Calendar calendar = buildCalendarClient(accessToken);
        try {
            calendar.events().delete("primary", eventId).execute();
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                // already gone – that's fine
                log.debug("[Calendar] Event {} already deleted for user {}", eventId, user.getId());
            } else {
                throw new CalendarSyncException("Failed to delete event " + eventId, e);
            }
        } catch (Exception e) {
            throw new CalendarSyncException("Failed to delete event " + eventId, e);
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Event buildEvent(ContestDto contest) {
        Instant start = contest.getStartTime();
        Instant end = start.plusSeconds(contest.getDurationSeconds());

        return new Event()
                .setSummary(contest.getPlatform() + ": " + contest.getName())
                .setDescription("Contest link: " + contest.getUrl())
                .setLocation(contest.getUrl())
                .setStart(toEventDateTime(start))
                .setEnd(toEventDateTime(end));
    }

    private EventDateTime toEventDateTime(Instant instant) {
        return new EventDateTime()
                .setDateTime(new com.google.api.client.util.DateTime(instant.toEpochMilli()))
                .setTimeZone("UTC");
    }

    private Calendar buildCalendarClient(String accessToken) {
        Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod())
                .setAccessToken(accessToken);
        return new Calendar.Builder(HTTP_TRANSPORT, GsonFactory.getDefaultInstance(), credential)
                .setApplicationName("CPSync")
                .build();
    }

    private void sleep(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static class CalendarSyncException extends RuntimeException {
        public CalendarSyncException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}