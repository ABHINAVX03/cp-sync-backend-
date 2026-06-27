package com.cpsync.cpsync_backend.controller;

import com.cpsync.cpsync_backend.dto.ContestDto;
import com.cpsync.cpsync_backend.model.UserPlatformPreference;
import com.cpsync.cpsync_backend.repository.UserPlatformPreferenceRepository;
import com.cpsync.cpsync_backend.service.ContestAggregatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/contests")
public class ContestController {

    private static final Logger log = LoggerFactory.getLogger(ContestController.class);

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
        // This should never be null after SecurityConfig fix, but guard defensively
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("/api/contests/mine accessed without authentication");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        Long userId;
        try {
            userId = (Long) authentication.getPrincipal();
        } catch (ClassCastException e) {
            log.error("Principal is not a Long: {}", authentication.getPrincipal());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid authentication principal");
        }

        log.debug("Fetching contests for user {}", userId);

        Set<com.cpsync.cpsync_backend.model.Platform> enabledPlatforms;
        try {
            enabledPlatforms = platformPreferenceRepository.findByUserIdAndEnabledTrue(userId)
                    .stream()
                    .map(UserPlatformPreference::getPlatform)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Failed to load platform preferences for user {}", userId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to load platform preferences");
        }

        if (enabledPlatforms.isEmpty()) {
            return List.of();  // frontend shows empty state
        }

        try {
            return aggregatorService.fetchContestsForPlatforms(enabledPlatforms);
        } catch (Exception e) {
            log.error("Failed to fetch contests for user {} with platforms {}", userId, enabledPlatforms, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to fetch contests");
        }
    }
}