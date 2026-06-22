package com.cpsync.cpsync_backend.repository;

import com.cpsync.cpsync_backend.model.AccessRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessRequestRepository extends JpaRepository<AccessRequest, Long> {
}