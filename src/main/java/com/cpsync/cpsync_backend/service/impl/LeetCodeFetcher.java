package com.cpsync.cpsync_backend.service.impl;

import com.cpsync.cpsync_backend.dto.ContestDto;
import com.cpsync.cpsync_backend.model.Platform;
import com.cpsync.cpsync_backend.service.ContestFetcher;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.cache.annotation.Cacheable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LeetCodeFetcher implements ContestFetcher {

    private static final String GRAPHQL_URL = "https://leetcode.com/graphql";
    private static final Logger log = LoggerFactory.getLogger(LeetCodeFetcher.class);

    private static final String QUERY = """
        query {
          allContests {
            title
            startTime
            duration
            titleSlug
          }
        }
        """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public LeetCodeFetcher(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }
    @Override
    public Platform getPlatform() {
        return Platform.LEETCODE;
    }

    @Override
    @Cacheable(value = "leetcodeContests", unless = "#result.isEmpty()")
    public List<ContestDto> fetchUpcomingContests() {
        try {
            String response = restClient.post()
                    .uri(GRAPHQL_URL)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .header("Referer", "https://leetcode.com")
                    .body(Map.of("query", QUERY))
                    .retrieve()
                    .body(String.class);

            LeetCodeGraphQLResponse parsed = objectMapper.readValue(response, LeetCodeGraphQLResponse.class);

            if (parsed.data == null || parsed.data.allContests == null) {
                return Collections.emptyList();
            }

            long nowEpochSeconds = Instant.now().getEpochSecond();

            return parsed.data.allContests.stream()
                    .filter(c -> c.startTime >= nowEpochSeconds) // allContests includes past ones too
                    .map(this::toContestDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("[{}] Fetch failed: {}", getPlatform(), e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private ContestDto toContestDto(LeetCodeContest c) {
        return new ContestDto(
                Platform.LEETCODE.name(),
                c.titleSlug,
                c.title,
                Instant.ofEpochSecond(c.startTime),
                c.duration,
                "https://leetcode.com/contest/" + c.titleSlug
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LeetCodeGraphQLResponse {
        public LeetCodeData data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LeetCodeData {
        public List<LeetCodeContest> allContests;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LeetCodeContest {
        public String title;
        public String titleSlug;
        public long startTime;
        public long duration;
    }
}
