package com.cpsync.cpsync_backend.controller;

import com.cpsync.cpsync_backend.model.AccessRequest;
import com.cpsync.cpsync_backend.repository.AccessRequestRepository;
import com.cpsync.cpsync_backend.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api")
public class AccessRequestController {

    private final AccessRequestRepository accessRequestRepository;
    private final UserRepository userRepository;

    public AccessRequestController(AccessRequestRepository accessRequestRepository,
                                   UserRepository userRepository) {
        this.accessRequestRepository = accessRequestRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/request-access")
    @Transactional
    public String requestAccess(@RequestBody AccessRequest request) {
        if (request.getEmail() == null || !request.getEmail().contains("@")) {
            return "Invalid email address.";
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return "You're already an active user. Please sign in directly.";
        }

        if (accessRequestRepository.existsByEmail(request.getEmail())) {
            return "You've already requested access. We'll activate your account soon.";
        }

        try {
            accessRequestRepository.save(request);
        } catch (DataIntegrityViolationException e) {
            // Two concurrent requests for the same email — unique index caught it
            return "You've already requested access. We'll activate your account soon.";
        }

        return "Your request has been received. We'll activate your account within 12 hours.";
    }
}