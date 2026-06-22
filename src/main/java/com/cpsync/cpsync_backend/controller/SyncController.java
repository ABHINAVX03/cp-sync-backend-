package com.cpsync.cpsync_backend.controller;

import com.cpsync.cpsync_backend.model.User;
import com.cpsync.cpsync_backend.repository.UserRepository;
import com.cpsync.cpsync_backend.service.SyncService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final SyncService syncService;
    private final UserRepository userRepository;

    public SyncController(SyncService syncService, UserRepository userRepository) {
        this.syncService = syncService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public String triggerManualSync(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        int syncedCount = syncService.syncContestsForUser(user);

        return "Synced " + syncedCount + " new contest(s) to your calendar.";
    }
}