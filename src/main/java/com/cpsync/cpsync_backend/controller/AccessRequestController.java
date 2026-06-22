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
        // Save to database – you can view them later via logs or a simple query
        accessRequestRepository.save(request);
        // Log the email so you can see it immediately in Render's logs
        System.out.println("New access request: " + request.getEmail());
        return "Your request has been received. We'll activate your account within 12 hours.";
    }
}