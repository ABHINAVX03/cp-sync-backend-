package com.cpsync.cpsync_backend.service.impl;

import com.cpsync.cpsync_backend.dto.ContestDto;
import com.cpsync.cpsync_backend.model.Platform;
import com.cpsync.cpsync_backend.service.ContestFetcher;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.cache.annotation.Cacheable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CodeforcesFetcher implements ContestFetcher {

    private static final String API_URL = "https://codeforces.com/api/contest.list?gym=false";
    private static final Logger log = LoggerFactory.getLogger(CodeforcesFetcher.class);
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public CodeforcesFetcher(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public Platform getPlatform() {
        return Platform.CODEFORCES;
    }

    @Override
    @Cacheable(value = "codeforcesContests", unless = "#result.isEmpty()")
    public List<ContestDto> fetchUpcomingContests() {
        try {
            String response = restClient.get()
                    .uri(API_URL)
                    .retrieve()
                    .body(String.class);

            CodeforcesApiResponse parsed = objectMapper.readValue(response, CodeforcesApiResponse.class);

            if (!"OK".equals(parsed.status)) {
                return Collections.emptyList();
            }

            return parsed.result.stream()
                    .filter(c -> "BEFORE".equals(c.phase)) // only contests that haven't started yet
                    .map(this::toContestDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("[{}] Fetch failed: {}", getPlatform(), e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private ContestDto toContestDto(CodeforcesContest c) {
        return new ContestDto(
                Platform.CODEFORCES.name(),
                String.valueOf(c.id),
                c.name,
                Instant.ofEpochSecond(c.startTimeSeconds),
                c.durationSeconds,
                "https://codeforces.com/contest/" + c.id
        );
    }

    // --- Internal DTOs matching Codeforces API JSON shape ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CodeforcesApiResponse {
        public String status;
        public List<CodeforcesContest> result;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CodeforcesContest {
        public long id;
        public String name;
        public String phase;
        public long durationSeconds;
        public long startTimeSeconds;
    }
}
