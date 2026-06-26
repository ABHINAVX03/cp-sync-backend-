package com.cpsync.cpsync_backend;

import com.cpsync.cpsync_backend.security.AuthTokenStore;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthTokenStoreTest {

    private final AuthTokenStore store = new AuthTokenStore();

    @Test
    void generateAndConsume_returnsJwt() {
        String jwt = "eyJfake.jwt.token";
        String code = store.generateCode(jwt);
        Optional<String> result = store.consumeCode(code);
        assertTrue(result.isPresent());
        assertEquals(jwt, result.get());
    }

    @Test
    void consumeCode_oneTimeUse_secondCallReturnsEmpty() {
        String code = store.generateCode("jwt");
        store.consumeCode(code);
        Optional<String> second = store.consumeCode(code);
        assertTrue(second.isEmpty());
    }

    @Test
    void consumeCode_unknownCode_returnsEmpty() {
        assertTrue(store.consumeCode("nonexistent-code").isEmpty());
    }

    @Test
    void consumeCode_nullCode_returnsEmpty() {
        assertTrue(store.consumeCode(null).isEmpty());
    }
}
