package com.cpsync.cpsync_backend.service;

import com.cpsync.cpsync_backend.dto.ContestDto;
import com.cpsync.cpsync_backend.model.Platform;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ContestAggregatorService {

    private final List<ContestFetcher> fetchers;
    private final FetcherHealthMonitor healthMonitor;

    public ContestAggregatorService(List<ContestFetcher> fetchers, FetcherHealthMonitor healthMonitor) {
        this.fetchers = fetchers;
        this.healthMonitor = healthMonitor;
    }

    public List<ContestDto> fetchAllContests() {
        return fetchers.stream()
                .flatMap(this::fetchAndMonitor)
                .sorted(Comparator.comparing(ContestDto::getStartTime))
                .collect(Collectors.toList());
    }

    public List<ContestDto> fetchContestsForPlatforms(Set<Platform> enabledPlatforms) {
        return fetchers.stream()
                .filter(fetcher -> enabledPlatforms.contains(fetcher.getPlatform()))
                .flatMap(this::fetchAndMonitor)
                .sorted(Comparator.comparing(ContestDto::getStartTime))
                .collect(Collectors.toList());
    }

    private java.util.stream.Stream<ContestDto> fetchAndMonitor(ContestFetcher fetcher) {
        List<ContestDto> results = fetcher.fetchUpcomingContests();
        healthMonitor.recordResult(fetcher.getPlatform(), results.size());
        return results.stream();
    }
}