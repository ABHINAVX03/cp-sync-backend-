package com.cpsync.cpsync_backend.service;

import com.cpsync.cpsync_backend.model.User;
import com.cpsync.cpsync_backend.repository.UserRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

@Service
public class GoogleTokenRefreshService {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private final UserRepository userRepository;
    private final TokenEncryptionService tokenEncryptionService;
    private final String clientId;
    private final String clientSecret;

    public GoogleTokenRefreshService(
            RestClient.Builder restClientBuilder,
            UserRepository userRepository,
            TokenEncryptionService tokenEncryptionService,
            ObjectMapper objectMapper,
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String clientId,
            @Value("${spring.security.oauth2.client.registration.google.client-secret}") String clientSecret
    ) {
        this.restClient = restClientBuilder.build();
        this.userRepository = userRepository;
        this.tokenEncryptionService = tokenEncryptionService;
        this.objectMapper = objectMapper;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /**
     * Returns a valid, decrypted access token for this user — refreshing it first if expired.
     * Throws if the user has no refresh token (shouldn't happen if login flow worked correctly).
     */
    public String getValidAccessToken(User user) {
        boolean isExpired = user.getTokenExpiry() == null
                || user.getTokenExpiry().isBefore(LocalDateTime.now().plusMinutes(2)); // 2-min buffer

        if (!isExpired) {
            return tokenEncryptionService.decrypt(user.getAccessToken());
        }

        return refreshAccessToken(user);
    }

    private String refreshAccessToken(User user) {
        if (user.getRefreshToken() == null) {
            throw new IllegalStateException(
                    "User " + user.getId() + " has no refresh token — cannot refresh access token."
            );
        }

        String decryptedRefreshToken = tokenEncryptionService.decrypt(user.getRefreshToken());

        MultiValueMap<String, String> formParams = new LinkedMultiValueMap<>();
        formParams.add("client_id", clientId);
        formParams.add("client_secret", clientSecret);
        formParams.add("refresh_token", decryptedRefreshToken);
        formParams.add("grant_type", "refresh_token");

        try {
            String response = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formParams)
                    .retrieve()
                    .body(String.class);

            TokenRefreshResponse parsed = objectMapper.readValue(response, TokenRefreshResponse.class);

            user.setAccessToken(tokenEncryptionService.encrypt(parsed.accessToken));
            user.setTokenExpiry(LocalDateTime.now().plusSeconds(parsed.expiresIn));
            userRepository.save(user);

            return parsed.accessToken;

        } catch (org.springframework.web.client.HttpClientErrorException.BadRequest e) {
            user.setActive(false);
            userRepository.save(user);
            throw new IllegalStateException("User " + user.getId() + " revoked calendar access — account paused automatically.");
        } catch (Exception e) {
            throw new RuntimeException("Token refresh failed for user " + user.getId(), e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TokenRefreshResponse {
        @com.fasterxml.jackson.annotation.JsonProperty("access_token")
        public String accessToken;

        @com.fasterxml.jackson.annotation.JsonProperty("expires_in")
        public long expiresIn;
    }
}