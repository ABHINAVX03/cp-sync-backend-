package com.cpsync.cpsync_backend.controller;

import com.cpsync.cpsync_backend.dto.request.DeleteAccountRequest;
import com.cpsync.cpsync_backend.dto.request.UpdatePlatformsRequest;
import com.cpsync.cpsync_backend.dto.response.UserProfileResponse;
import com.cpsync.cpsync_backend.security.UserDenyList;
import com.cpsync.cpsync_backend.service.AccountDeletionService;
import com.cpsync.cpsync_backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final UserDenyList userDenyList;
    private final AccountDeletionService accountDeletionService;

    public UserController(UserService userService,
                          UserDenyList userDenyList,
                          AccountDeletionService accountDeletionService) {
        this.userService = userService;
        this.userDenyList = userDenyList;
        this.accountDeletionService = accountDeletionService;
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
        userDenyList.deny(userId);
        return userService.getProfile(userId);
    }

    @PutMapping("/resume")
    public UserProfileResponse resumeSync(Authentication authentication) {
        Long userId = extractUserId(authentication);
        userService.setActive(userId, true);
        userDenyList.allow(userId);
        return userService.getProfile(userId);
    }

    /**
     * Permanently deletes the account, all calendar events, and all associated data.
     * Requires email confirmation to prevent accidental deletion.
     */
    @DeleteMapping("/account")
    public ResponseEntity<Map<String, String>> deleteAccount(
            Authentication authentication,
            @Valid @RequestBody DeleteAccountRequest request) {
        Long userId = extractUserId(authentication);
        String actualEmail = userService.getEmailById(userId);

        if (!request.getEmail().equals(actualEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email does not match");
        }

        accountDeletionService.deleteAccount(userId);
        return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
    }

    private Long extractUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}