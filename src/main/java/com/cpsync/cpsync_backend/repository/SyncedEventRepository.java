package com.cpsync.cpsync_backend.repository;

import com.cpsync.cpsync_backend.model.SyncedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SyncedEventRepository extends JpaRepository<SyncedEvent, Long> {
    Optional<SyncedEvent> findByUserIdAndContestKey(Long userId, String contestKey);
    boolean existsByUserIdAndContestKey(Long userId, String contestKey);

    @Query("SELECT s.contestKey FROM SyncedEvent s WHERE s.user.id = :userId")
    Set<String> findContestKeysByUserId(@Param("userId") Long userId);

    // Fetch all event IDs for cleanup before account deletion
    @Query("SELECT s.googleEventId FROM SyncedEvent s WHERE s.user.id = :userId")
    List<String> findGoogleEventIdsByUserId(@Param("userId") Long userId);

    // Bulk delete all events for a user (used during account deletion)
    @Modifying
    @Query("DELETE FROM SyncedEvent s WHERE s.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}