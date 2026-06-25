package com.cpsync.cpsync_backend.repository;

import com.cpsync.cpsync_backend.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleId(String googleId);

    // Old method — kept for any direct calls outside the scheduler
    List<User> findByActiveTrue();

    // New: paginated version for the batched scheduler.
    // Spring Data derives this automatically — no @Query needed.
    List<User> findByActiveTrueOrderById(Pageable pageable);
}