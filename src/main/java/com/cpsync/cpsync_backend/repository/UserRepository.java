package com.cpsync.cpsync_backend.repository;

import com.cpsync.cpsync_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleId(String googleId);
    java.util.List<User> findByActiveTrue();
}