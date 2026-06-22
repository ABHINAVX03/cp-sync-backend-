package com.cpsync.cpsync_backend.controller;

import com.cpsync.cpsync_backend.dto.request.AccessRequest;
import com.cpsync.cpsync_backend.service.EmailService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AccessRequestController {

    private final EmailService emailService;

    public AccessRequestController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/request-access")
    public String requestAccess(@RequestBody AccessRequest request) {
        if (request.getEmail() == null || !request.getEmail().contains("@")) {
            return "Invalid email address.";
        }
        emailService.sendAccessRequestNotification(request.getEmail());
        return "Your request has been received. We'll activate your account within 12 hours.";
    }
}