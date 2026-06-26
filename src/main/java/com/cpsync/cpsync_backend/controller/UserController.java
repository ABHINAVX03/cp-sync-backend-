package com.cpsync.cpsync_backend.controller;

import com.cpsync.cpsync_backend.dto.request.UpdatePlatformsRequest;
import com.cpsync.cpsync_backend.dto.response.UserProfileResponse;
import com.cpsync.cpsync_backend.security.UserDenyList;
import com.cpsync.cpsync_backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final UserDenyList userDenyList;

    public UserController(UserService userService, UserDenyList userDenyList) {
        this.userService = userService;
        this.userDenyList = userDenyList;
    }

    @GetMapping("/profile")
    public UserProfileResponse getProfile(Authentication authentication) {
        return userService.getProfile(extractUserId(authentication));
    }

    @PutMapping("/platforms")
    public UserProfileResponse updatePlatforms(Authentication authentication,
                                               @Valid @RequestBody UpdatePlatformsRequest request) {
        return userService.updatePlatforms(extractUserId(authentication), request.getPlatforms());
    }

    @PutMapping("/pause")
    public UserProfileResponse pauseSync(Authentication authentication) {
        Long userId = extractUserId(authentication);
        userService.setActive(userId, false);
        userDenyList.deny(userId);          // FIXED: immediately block existing JWT
        return userService.getProfile(userId);
    }

    @PutMapping("/resume")
    public UserProfileResponse resumeSync(Authentication authentication) {
        Long userId = extractUserId(authentication);
        userService.setActive(userId, true);
        userDenyList.allow(userId);         // FIXED: re-allow immediately
        return userService.getProfile(userId);
    }

    private Long extractUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}