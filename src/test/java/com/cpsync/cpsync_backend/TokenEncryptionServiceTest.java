package com.cpsync.cpsync_backend;

import com.cpsync.cpsync_backend.service.TokenEncryptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest
@ActiveProfiles("test")
class TokenEncryptionServiceTest {

    @Autowired
    private TokenEncryptionService encryptionService;

    @Test
    void encryptDecrypt_roundTrip_returnsOriginalValue() {
        String original = "ya29.fake-access-token-value-1234567890";

        String encrypted = encryptionService.encrypt(original);
        String decrypted = encryptionService.decrypt(encrypted);

        assertNotEquals(original, encrypted); // make sure it's actually encrypted
        assertEquals(original, decrypted);    // and decrypts back correctly
    }

    @Test
    void encrypt_sameInputTwice_producesDifferentOutput() {
        String original = "same-token-value";

        String encrypted1 = encryptionService.encrypt(original);
        String encrypted2 = encryptionService.encrypt(original);

        // Different because IV is random each time — this is expected and good
        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    void encrypt_nullInput_returnsNull() {
        assertEquals(null, encryptionService.encrypt(null));
    }
}