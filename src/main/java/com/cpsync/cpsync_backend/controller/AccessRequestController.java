package com.cpsync.cpsync_backend.controller;

import com.cpsync.cpsync_backend.model.AccessRequest;
import com.cpsync.cpsync_backend.repository.AccessRequestRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AccessRequestController {

    private final AccessRequestRepository accessRequestRepository;

    public AccessRequestController(AccessRequestRepository accessRequestRepository) {
        this.accessRequestRepository = accessRequestRepository;
    }

    @PostMapping("/request-access")
    public String requestAccess(@RequestBody AccessRequest request) {
        if (request.getEmail() == null || !request.getEmail().contains("@")) {
            return "Invalid email address.";
        }

        // Check if email already exists
        if (accessRequestRepository.existsByEmail(request.getEmail())) {
            return "You've already requested access. We'll activate your account soon.";
        }

        accessRequestRepository.save(request);
        System.out.println("New access request: " + request.getEmail());
        return "Your request has been received. We'll activate your account within 12 hours.";
    }
}