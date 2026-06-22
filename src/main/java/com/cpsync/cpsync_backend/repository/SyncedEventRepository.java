package com.cpsync.cpsync_backend.repository;

import com.cpsync.cpsync_backend.model.SyncedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SyncedEventRepository extends JpaRepository<SyncedEvent, Long> {
    Optional<SyncedEvent> findByUserIdAndContestKey(Long userId, String contestKey);
    boolean existsByUserIdAndContestKey(Long userId, String contestKey);
}