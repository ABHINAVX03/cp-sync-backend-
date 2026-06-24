package com.cpsync.cpsync_backend.service;

import com.cpsync.cpsync_backend.dto.ContestDto;
import com.cpsync.cpsync_backend.model.Platform;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class ContestAggregatorService {

    private final List<ContestFetcher> fetchers;
    private final FetcherHealthMonitor healthMonitor;
    private final ThreadPoolTaskExecutor ioTaskExecutor;

    public ContestAggregatorService(List<ContestFetcher> fetchers,
                                    FetcherHealthMonitor healthMonitor,
                                    ThreadPoolTaskExecutor ioTaskExecutor) {
        this.fetchers = fetchers;
        this.healthMonitor = healthMonitor;
        this.ioTaskExecutor = ioTaskExecutor;
    }

    public List<ContestDto> fetchAllContests() {
        return fetchInParallel(fetchers);
    }

    public List<ContestDto> fetchContestsForPlatforms(Set<Platform> enabledPlatforms) {
        List<ContestFetcher> filtered = fetchers.stream()
                .filter(fetcher -> enabledPlatforms.contains(fetcher.getPlatform()))
                .toList();
        return fetchInParallel(filtered);
    }

    private List<ContestDto> fetchInParallel(List<ContestFetcher> targetFetchers) {
        List<CompletableFuture<List<ContestDto>>> futures = targetFetchers.stream()
                .map(fetcher -> CompletableFuture.supplyAsync(() -> {
                    List<ContestDto> results = fetcher.fetchUpcomingContests();
                    healthMonitor.recordResult(fetcher.getPlatform(), results.size());
                    return results;
                }, ioTaskExecutor))
                .toList();

        return futures.stream()
                .flatMap(f -> f.join().stream())
                .sorted(Comparator.comparing(ContestDto::getStartTime))
                .collect(Collectors.toList());
    }
}