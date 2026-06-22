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
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class GoogleCalendarService {

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

    public String createContestEvent(User user, ContestDto contest) {
        String accessToken = tokenRefreshService.getValidAccessToken(user);
        Calendar calendarService = buildCalendarClient(accessToken);

        Event event = new Event()
                .setSummary(contest.getPlatform() + ": " + contest.getName())
                .setDescription("Contest link: " + contest.getUrl())
                .setLocation(contest.getUrl());

        Instant startInstant = contest.getStartTime();
        Instant endInstant = startInstant.plusSeconds(contest.getDurationSeconds());

        event.setStart(toGoogleEventDateTime(startInstant));
        event.setEnd(toGoogleEventDateTime(endInstant));

        try {
            Event createdEvent = calendarService.events()
                    .insert("primary", event)
                    .execute();
            return createdEvent.getId();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create calendar event for user " + user.getId() +
                            ", contest " + contest.getContestKey(), e
            );
        }
    }

    private EventDateTime toGoogleEventDateTime(Instant instant) {
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
}