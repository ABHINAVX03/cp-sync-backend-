package com.cpsync.cpsync_backend.controller;

import com.cpsync.cpsync_backend.dto.response.UserProfileResponse;
import com.cpsync.cpsync_backend.model.AccessRequest;
import com.cpsync.cpsync_backend.service.AdminService;
import com.cpsync.cpsync_backend.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Value("${app.admin-email}")
    private String adminEmail;

    private final UserService userService;
    private final AdminService adminService;

    public AdminController(UserService userService, AdminService adminService) {
        this.userService = userService;
        this.adminService = adminService;
    }

    private void requireAdmin(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String email = userService.getEmailById(userId);
        if (!adminEmail.equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
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
        return adminService.getAllAccessRequests();
    }

    @PostMapping("/approve/{id}")
    public ResponseEntity<Map<String, String>> approveRequest(@PathVariable Long id,
                                                              Authentication authentication) {
        requireAdmin(authentication);
        String message = adminService.approveById(id);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/manual-approve")
    public ResponseEntity<Map<String, String>> manualApprove(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        requireAdmin(authentication);
        String email = body.get("email");
        if (email == null || !email.contains("@")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid email"));
        }
        adminService.approveByEmail(email);
        return ResponseEntity.ok(Map.of("message", "Approved, email sent"));
    }
}