package com.cpsync.cpsync_backend;

import com.cpsync.cpsync_backend.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    // 64-char secret = 512-bit key, valid for HS256
    private static final String SECRET =
            "test-secret-that-is-at-least-64-characters-long-for-hs256-algorithm!!";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, 3600_000L);
    }

    @Test
    void generateAndExtract_userId() {
        String token = jwtUtil.generateToken(42L, "user@example.com");
        assertEquals(42L, jwtUtil.extractUserId(token));
    }

    @Test
    void generateAndExtract_email() {
        String token = jwtUtil.generateToken(42L, "user@example.com");
        assertEquals("user@example.com", jwtUtil.extractEmail(token));
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        String token = jwtUtil.generateToken(1L, "a@b.com");
        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    void isTokenValid_tamperedToken_returnsFalse() {
        String token = jwtUtil.generateToken(1L, "a@b.com");
        String tampered = token.substring(0, token.length() - 1) + "X";
        assertFalse(jwtUtil.isTokenValid(tampered));
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() throws InterruptedException {
        JwtUtil shortLived = new JwtUtil(SECRET, 1L);
        String token = shortLived.generateToken(1L, "a@b.com");
        Thread.sleep(10);
        assertFalse(shortLived.isTokenValid(token));
    }

    @Test
    void isTokenValid_emptyString_returnsFalse() {
        assertFalse(jwtUtil.isTokenValid(""));
    }

    @Test
    void isTokenValid_null_returnsFalse() {
        assertFalse(jwtUtil.isTokenValid(null));
    }
}
