package com.cpsync.cpsync_backend.controller;

import com.cpsync.cpsync_backend.dto.response.UserProfileResponse;
import com.cpsync.cpsync_backend.model.AccessRequest;
import com.cpsync.cpsync_backend.repository.AccessRequestRepository;
import com.cpsync.cpsync_backend.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final String ADMIN_EMAIL = "guptaabhinav697@gmail.com";

    private final UserService userService;
    private final AccessRequestRepository accessRequestRepository;

    public AdminController(UserService userService,
                           AccessRequestRepository accessRequestRepository) {
        this.userService = userService;
        this.accessRequestRepository = accessRequestRepository;
    }

    private void requireAdmin(Authentication authentication) {
        // principal is the user ID (set by JwtAuthFilter)
        // we need to fetch the user's email to verify
        // We can get the email from the JWT directly (if we stored it) or fetch from DB.
        // The JWT contains the email as a claim, so let's extract it.
        // In JwtAuthFilter, we set the principal to userId, not email.
        // We can modify JwtAuthFilter to set the email as well (e.g., in credentials or details).
        // Simpler: fetch user by ID from userService and compare email.
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
}