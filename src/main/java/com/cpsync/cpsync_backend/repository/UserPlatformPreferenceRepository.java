package com.cpsync.cpsync_backend.repository;

import com.cpsync.cpsync_backend.model.Platform;
import com.cpsync.cpsync_backend.model.UserPlatformPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserPlatformPreferenceRepository extends JpaRepository<UserPlatformPreference, Long> {
    List<UserPlatformPreference> findByUserId(Long userId);
    Optional<UserPlatformPreference> findByUserIdAndPlatform(Long userId, Platform platform);
    List<UserPlatformPreference> findByUserIdAndEnabledTrue(Long userId);
}