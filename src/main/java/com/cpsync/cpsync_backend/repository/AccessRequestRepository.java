package com.cpsync.cpsync_backend.repository;

import com.cpsync.cpsync_backend.model.AccessRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccessRequestRepository extends JpaRepository<AccessRequest, Long> {
    boolean existsByEmail(String email);
    Optional<AccessRequest> findByEmail(String email);
}