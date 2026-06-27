package com.cpsync.cpsync_backend.repository;

import com.cpsync.cpsync_backend.model.Platform;
import com.cpsync.cpsync_backend.model.UserPlatformPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserPlatformPreferenceRepository extends JpaRepository<UserPlatformPreference, Long> {
    List<UserPlatformPreference> findByUserId(Long userId);
    Optional<UserPlatformPreference> findByUserIdAndPlatform(Long userId, Platform platform);
    List<UserPlatformPreference> findByUserIdAndEnabledTrue(Long userId);

    @Query("SELECT p FROM UserPlatformPreference p JOIN FETCH p.user WHERE p.enabled = true")
    List<UserPlatformPreference> findAllEnabledWithUser();

    // Bulk delete all preferences for a user
    @Modifying
    @Query("DELETE FROM UserPlatformPreference p WHERE p.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}