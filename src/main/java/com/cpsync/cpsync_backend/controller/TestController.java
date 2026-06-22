package com.cpsync.cpsync_backend.controller;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
public class TestController {

    @GetMapping("/api/me")
    public Object me(OAuth2AuthenticationToken authentication) {
        OAuth2User user = authentication.getPrincipal();
        return user.getAttributes(); // dumps everything Google gave us
    }

    @GetMapping("/api/login-success")
    public String loginSuccess(@RequestParam Long userId) {
        return "Logged in. User ID in DB: " + userId;
    }

    @GetMapping("/api/whoami")
    public String whoAmI(org.springframework.security.core.Authentication authentication) {
        if (authentication == null) {
            return "Not authenticated";
        }
        return "Authenticated as user ID: " + authentication.getPrincipal();
    }
}