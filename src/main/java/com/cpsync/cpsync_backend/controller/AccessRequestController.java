package com.cpsync.cpsync_backend.controller;

import com.cpsync.cpsync_backend.model.AccessRequest;
import com.cpsync.cpsync_backend.repository.AccessRequestRepository;
import com.cpsync.cpsync_backend.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

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
    public String requestAccess(@RequestBody AccessRequest request) {
        if (request.getEmail() == null || !request.getEmail().contains("@")) {
            return "Invalid email address.";
        }

        // 1. Check if user already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return "You're already an active user. Please sign in directly.";
        }

        // 2. Check if a request already exists
        if (accessRequestRepository.existsByEmail(request.getEmail())) {
            return "You've already requested access. We'll activate your account soon.";
        }

        // 3. Save new request
        accessRequestRepository.save(request);
        System.out.println("New access request: " + request.getEmail());
        return "Your request has been received. We'll activate your account within 12 hours.";
    }
}