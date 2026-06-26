package com.cpsync.cpsync_backend.controller;

import com.cpsync.cpsync_backend.model.User;
import com.cpsync.cpsync_backend.service.SyncRateLimiter;
import com.cpsync.cpsync_backend.service.SyncService;
import com.cpsync.cpsync_backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final SyncService syncService;
    private final UserService userService;
    private final SyncRateLimiter rateLimiter;

    public SyncController(SyncService syncService,
                          UserService userService,
                          SyncRateLimiter rateLimiter) {
        this.syncService = syncService;
        this.userService = userService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> triggerManualSync(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        if (!rateLimiter.tryConsume(userId)) {
            long retryAfter = rateLimiter.secondsUntilRefill(userId);
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(retryAfter))
                    .body(Map.of(
                            "error", "You can only sync once every 5 minutes. Please wait " + retryAfter + "s.",
                            "retryAfterSeconds", retryAfter
                    ));
        }

        User user = userService.getUserById(userId);
        int syncedCount = syncService.syncContestsForUser(user);

        return ResponseEntity.ok(Map.of("status", "ok", "syncedCount", syncedCount));
    }
}
