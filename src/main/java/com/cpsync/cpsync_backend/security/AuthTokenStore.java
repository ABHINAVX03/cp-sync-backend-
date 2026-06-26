package com.cpsync.cpsync_backend.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Stores one-time codes that the frontend exchanges for a JWT.
 * Codes expire in 60 seconds — long enough for the browser redirect,
 * short enough that a leaked URL is useless.
 *
 * Each code can be consumed exactly once (invalidated on retrieval).
 */
@Component
public class AuthTokenStore {

    private final Cache<String, String> store = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .maximumSize(10_000)
            .build();

    /**
     * Stores the JWT and returns a one-time code to put in the redirect URL.
     */
    public String generateCode(String jwt) {
        String code = UUID.randomUUID().toString();
        store.put(code, jwt);
        return code;
    }

    /**
     * Consumes a code — returns the JWT and invalidates the code immediately.
     * Returns empty if the code is unknown or expired.
     */
    public Optional<String> consumeCode(String code) {
        if (code == null) return Optional.empty();
        String jwt = store.getIfPresent(code);
        if (jwt != null) {
            store.invalidate(code); // one-time use
        }
        return Optional.ofNullable(jwt);
    }
}