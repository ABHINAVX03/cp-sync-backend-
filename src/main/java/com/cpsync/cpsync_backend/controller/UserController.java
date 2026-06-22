package com.cpsync.cpsync_backend.controller;

import com.cpsync.cpsync_backend.dto.request.UpdatePlatformsRequest;
import com.cpsync.cpsync_backend.dto.response.UserProfileResponse;
import com.cpsync.cpsync_backend.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public UserProfileResponse getProfile(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return userService.getProfile(userId);
    }

    @PutMapping("/platforms")
    public UserProfileResponse updatePlatforms(Authentication authentication,
                                               @RequestBody UpdatePlatformsRequest request) {
        Long userId = extractUserId(authentication);
        return userService.updatePlatforms(userId, request.getPlatforms());
    }

    @PutMapping("/pause")
    public UserProfileResponse pauseSync(Authentication authentication) {
        Long userId = extractUserId(authentication);
        userService.setActive(userId, false);
        return userService.getProfile(userId);
    }

    @PutMapping("/resume")
    public UserProfileResponse resumeSync(Authentication authentication) {
        Long userId = extractUserId(authentication);
        userService.setActive(userId, true);
        return userService.getProfile(userId);
    }

    private Long extractUserId(Authentication authentication) {
        // Matches what JwtAuthFilter sets as the principal
        return (Long) authentication.getPrincipal();
    }
}