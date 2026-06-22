package com.cpsync.cpsync_backend.service;

import com.cpsync.cpsync_backend.dto.response.UserProfileResponse;
import com.cpsync.cpsync_backend.model.Platform;
import com.cpsync.cpsync_backend.model.User;
import com.cpsync.cpsync_backend.model.UserPlatformPreference;
import com.cpsync.cpsync_backend.repository.UserPlatformPreferenceRepository;
import com.cpsync.cpsync_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserPlatformPreferenceRepository platformPreferenceRepository;
    private final TokenEncryptionService tokenEncryptionService;

    public UserService(UserRepository userRepository,
                       UserPlatformPreferenceRepository platformPreferenceRepository,
                       TokenEncryptionService tokenEncryptionService) {
        this.userRepository = userRepository;
        this.platformPreferenceRepository = platformPreferenceRepository;
        this.tokenEncryptionService = tokenEncryptionService;
    }

    public User upsertUserFromLogin(
            String googleId,
            String email,
            String name,
            String accessToken,
            String refreshToken,
            LocalDateTime tokenExpiry
    ) {
        Optional<User> existing = userRepository.findByGoogleId(googleId);
        User user = existing.orElseGet(User::new);

        user.setGoogleId(googleId);
        user.setEmail(email);
        user.setName(name);
        user.setAccessToken(tokenEncryptionService.encrypt(accessToken));
        user.setTokenExpiry(tokenExpiry);

        if (refreshToken != null) {
            user.setRefreshToken(tokenEncryptionService.encrypt(refreshToken));
        }

        if (existing.isEmpty()) {
            user.setActive(true);
        }

        return userRepository.save(user);
    }

    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        List<String> enabledPlatforms = platformPreferenceRepository
                .findByUserIdAndEnabledTrue(userId)
                .stream()
                .map(pref -> pref.getPlatform().name())
                .collect(Collectors.toList());

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.isActive(),
                enabledPlatforms
        );
    }

    public UserProfileResponse updatePlatforms(Long userId, List<String> platformNames) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        List<Platform> requestedPlatforms = platformNames.stream()
                .map(name -> Platform.valueOf(name.toUpperCase()))
                .collect(Collectors.toList());

        // Disable everything first, then enable only what was requested.
        // Simpler and safer than trying to diff add/remove.
        List<UserPlatformPreference> existingPrefs = platformPreferenceRepository.findByUserId(userId);

        for (UserPlatformPreference pref : existingPrefs) {
            pref.setEnabled(requestedPlatforms.contains(pref.getPlatform()));
        }
        platformPreferenceRepository.saveAll(existingPrefs);

        // Add any requested platforms that don't have a row yet
        for (Platform platform : requestedPlatforms) {
            boolean exists = existingPrefs.stream()
                    .anyMatch(p -> p.getPlatform() == platform);

            if (!exists) {
                UserPlatformPreference newPref = new UserPlatformPreference();
                newPref.setUser(user);
                newPref.setPlatform(platform);
                newPref.setEnabled(true);
                platformPreferenceRepository.save(newPref);
            }
        }

        return getProfile(userId);
    }

    public User setActive(Long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        user.setActive(active);
        return userRepository.save(user);
    }

    public String getEmailById(Long userId) {
        return userRepository.findById(userId)
                .map(User::getEmail)
                .orElse(null);
    }

    public List<UserProfileResponse> getAllUserProfiles() {
        return userRepository.findAll().stream()
                .map(user -> {
                    List<String> platforms = platformPreferenceRepository
                            .findByUserIdAndEnabledTrue(user.getId())
                            .stream()
                            .map(pref -> pref.getPlatform().name())
                            .collect(Collectors.toList());
                    return new UserProfileResponse(
                            user.getId(),
                            user.getEmail(),
                            user.getName(),
                            user.isActive(),
                            platforms
                    );
                })
                .collect(Collectors.toList());
    }
}