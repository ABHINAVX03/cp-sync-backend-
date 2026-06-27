package com.cpsync.cpsync_backend.controller;

import com.cpsync.cpsync_backend.dto.ContestDto;
import com.cpsync.cpsync_backend.model.UserPlatformPreference;
import com.cpsync.cpsync_backend.repository.UserPlatformPreferenceRepository;
import com.cpsync.cpsync_backend.service.ContestAggregatorService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/contests")
public class ContestController {

    private final ContestAggregatorService aggregatorService;
    private final UserPlatformPreferenceRepository platformPreferenceRepository;

    public ContestController(ContestAggregatorService aggregatorService,
                             UserPlatformPreferenceRepository platformPreferenceRepository) {
        this.aggregatorService = aggregatorService;
        this.platformPreferenceRepository = platformPreferenceRepository;
    }

    /** Public endpoint – all contests across all platforms (unfiltered). */
    @GetMapping
    public List<ContestDto> getAllUpcomingContests() {
        return aggregatorService.fetchAllContests();
    }

    /** Authenticated endpoint – only contests for the user's enabled platforms. */
    @GetMapping("/mine")
    public List<ContestDto> getMyContests(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Set<com.cpsync.cpsync_backend.model.Platform> enabledPlatforms =
                platformPreferenceRepository.findByUserIdAndEnabledTrue(userId)
                        .stream()
                        .map(UserPlatformPreference::getPlatform)
                        .collect(Collectors.toSet());

        if (enabledPlatforms.isEmpty()) {
            return List.of();
        }
        return aggregatorService.fetchContestsForPlatforms(enabledPlatforms);
    }
}