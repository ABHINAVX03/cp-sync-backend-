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
     *
     * 401 handling: refresh the token exactly once. If it is still 401 after
     * the refresh, the user has revoked calendar access — throw immediately
     * rather than looping forever.
     *
     * Returns the Google event ID on success.
     * Throws CalendarSyncException (unchecked) on permanent failure so
     * SyncService can skip this contest and continue with the rest.
     */
    public String createContestEvent(User user, ContestDto contest) {
        String accessToken = tokenRefreshService.getValidAccessToken(user);
        Calendar calendarService = buildCalendarClient(accessToken);
        Event event = buildEvent(contest);

        Exception lastException = null;
        // Track whether we have already done a token refresh this call.
        // Ensures we never refresh more than once, preventing an infinite loop
        // if the refreshed token is also invalid (e.g. user revoked access).
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
                        // Refresh exactly once, then retry the same attempt slot.
                        // attempt-- cancels the upcoming attempt++ so we do not
                        // waste a retry slot on what was really a stale token.
                        log.warn("[Calendar] 401 for user {} contest {}, refreshing token once",
                                user.getId(), contest.getContestKey());
                        accessToken = tokenRefreshService.getValidAccessToken(user);
                        calendarService = buildCalendarClient(accessToken);
                        tokenRefreshed = true;
                        attempt--;
                        continue;
                    }
                    // Already refreshed once and still 401 — permanent auth failure.
                    // User has likely revoked calendar access.
                    throw new CalendarSyncException(
                            "Auth permanently failed after token refresh for contest "
                                    + contest.getContestKey()
                                    + " (user " + user.getId() + " may have revoked access)", e);
                }

                if (status == 429 || status >= 500) {
                    lastException = e;
                    long waitMs = backoffBaseMs * (1L << attempt); // 1s, 2s, 4s
                    log.warn("[Calendar] HTTP {} for user {} attempt {}/{}, waiting {}ms",
                            status, user.getId(), attempt + 1, maxRetries + 1, waitMs);
                    sleep(waitMs);
                    continue;
                }

                // 400, 403, 404 etc. are permanent — no point retrying
                throw new CalendarSyncException(
                        "Permanent calendar error (HTTP " + status + ") for contest "
                                + contest.getContestKey(), e);

            } catch (Exception e) {
                // Network timeout, I/O error — retry with backoff
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

    // Unchecked so SyncService can catch it cleanly without polluting every call site
    public static class CalendarSyncException extends RuntimeException {
        public CalendarSyncException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}