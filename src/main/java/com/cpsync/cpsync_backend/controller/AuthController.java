package com.cpsync.cpsync_backend.controller;

import com.cpsync.cpsync_backend.security.AuthTokenStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Exchanges a one-time code (from the OAuth2 redirect) for a JWT.
 * This endpoint is public — the code itself is the credential.
 * Codes are valid for 60 seconds and can only be used once.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthTokenStore authTokenStore;

    public AuthController(AuthTokenStore authTokenStore) {
        this.authTokenStore = authTokenStore;
    }

    @PostMapping("/exchange")
    public ResponseEntity<Map<String, String>> exchange(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        return authTokenStore.consumeCode(code)
                .map(jwt -> ResponseEntity.ok(Map.of("token", jwt)))
                .orElseGet(() -> ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid or expired code")));
    }
}