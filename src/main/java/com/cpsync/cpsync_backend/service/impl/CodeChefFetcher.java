package com.cpsync.cpsync_backend.service.impl;

import com.cpsync.cpsync_backend.dto.ContestDto;
import com.cpsync.cpsync_backend.model.Platform;
import com.cpsync.cpsync_backend.service.ContestFetcher;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.cache.annotation.Cacheable;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CodeChefFetcher implements ContestFetcher {

    private static final String API_URL = "https://www.codechef.com/api/list/contests/all";

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CodeChefFetcher(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }
    @Override
    public Platform getPlatform() {
        return Platform.CODECHEF;
    }

    @Override
    @Cacheable(value="codechefContests", unless = "#result.isEmpty()")
    public List<ContestDto> fetchUpcomingContests() {
        try {
            String response = restClient.get()
                    .uri(API_URL)
                    .header("User-Agent", "Mozilla/5.0 (compatible; CP-Bot/1.0)")
                    .retrieve()
                    .body(String.class);

            CodeChefApiResponse parsed = objectMapper.readValue(response, CodeChefApiResponse.class);

            if (!"success".equals(parsed.status) || parsed.futureContests == null) {
                return Collections.emptyList();
            }

            return parsed.futureContests.stream()
                    .map(this::toContestDto)
                    .filter(c -> c != null)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private ContestDto toContestDto(CodeChefContest c) {
        try {
            Instant startTime = OffsetDateTime.parse(c.startDateIso).toInstant();
            Instant endTime = OffsetDateTime.parse(c.endDateIso).toInstant();

            long durationSeconds = endTime.getEpochSecond() - startTime.getEpochSecond();

            return new ContestDto(
                    Platform.CODECHEF.name(),
                    c.contestCode,
                    c.contestName,
                    startTime,
                    durationSeconds,
                    "https://www.codechef.com/" + c.contestCode
            );
        } catch (Exception e) {
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CodeChefApiResponse {
        public String status;

        @JsonProperty("future_contests")
        public List<CodeChefContest> futureContests;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CodeChefContest {
        @JsonProperty("contest_code")
        public String contestCode;

        @JsonProperty("contest_name")
        public String contestName;

        @JsonProperty("contest_start_date_iso")
        public String startDateIso;

        @JsonProperty("contest_end_date_iso")
        public String endDateIso;
    }
}