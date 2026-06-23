package com.cpsync.cpsync_backend.controller;

import com.cpsync.cpsync_backend.dto.response.UserProfileResponse;
import com.cpsync.cpsync_backend.model.AccessRequest;
import com.cpsync.cpsync_backend.repository.AccessRequestRepository;
import com.cpsync.cpsync_backend.service.EmailService;
import com.cpsync.cpsync_backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final String ADMIN_EMAIL = "guptaabhinav697@gmail.com";

    private final UserService userService;
    private final AccessRequestRepository accessRequestRepository;
    private final EmailService emailService;

    public AdminController(UserService userService,
                           AccessRequestRepository accessRequestRepository,
                           EmailService emailService) {
        this.userService = userService;
        this.accessRequestRepository = accessRequestRepository;
        this.emailService = emailService;
    }

    private void requireAdmin(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String email = userService.getEmailById(userId);
        if (!ADMIN_EMAIL.equals(email)) {
            throw new RuntimeException("Access denied");
        }
    }

    @GetMapping("/users")
    public List<UserProfileResponse> getAllUsers(Authentication authentication) {
        requireAdmin(authentication);
        return userService.getAllUserProfiles();
    }

    @GetMapping("/access-requests")
    public List<AccessRequest> getAccessRequests(Authentication authentication) {
        requireAdmin(authentication);
        return accessRequestRepository.findAll();
    }

    @PostMapping("/approve/{id}")
    public ResponseEntity<?> approveRequest(@PathVariable Long id, Authentication authentication) {
        requireAdmin(authentication);

        AccessRequest request = accessRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (request.getApprovedAt() != null) {
            return ResponseEntity.ok(Map.of("message", "Already approved"));
        }

        // Mark as approved
        request.setApprovedAt(LocalDateTime.now());
        accessRequestRepository.save(request);

        // Send welcome email
        emailService.sendWelcomeEmail(request.getEmail());

        return ResponseEntity.ok(Map.of("message", "Approved, email sent to " + request.getEmail()));
    }

    @PostMapping("/manual-approve")
    public ResponseEntity<?> manualApprove(@RequestBody Map<String, String> body, Authentication authentication) {
        requireAdmin(authentication);
        String email = body.get("email");
        if (email == null || !email.contains("@")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid email"));
        }

        // Check if already in access_requests
        AccessRequest request = accessRequestRepository.findByEmail(email)
                .orElseGet(() -> {
                    AccessRequest newReq = new AccessRequest();
                    newReq.setEmail(email);
                    return newReq;
                });
        request.setApprovedAt(LocalDateTime.now());
        accessRequestRepository.save(request);

        emailService.sendWelcomeEmail(email);
        return ResponseEntity.ok(Map.of("message", "Approved, email sent"));
    }
}