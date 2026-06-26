package com.cpsync.cpsync_backend.repository;

import com.cpsync.cpsync_backend.model.Platform;
import com.cpsync.cpsync_backend.model.UserPlatformPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface UserPlatformPreferenceRepository extends JpaRepository<UserPlatformPreference, Long> {
    List<UserPlatformPreference> findByUserId(Long userId);
    Optional<UserPlatformPreference> findByUserIdAndPlatform(Long userId, Platform platform);
    List<UserPlatformPreference> findByUserIdAndEnabledTrue(Long userId);

    /**
     * Loads all enabled preferences with their users in a single JOIN query.
     * Used by getAllUserProfiles() to avoid N+1.
     */
    @Query("SELECT p FROM UserPlatformPreference p JOIN FETCH p.user WHERE p.enabled = true")
    List<UserPlatformPreference> findAllEnabledWithUser();
}
