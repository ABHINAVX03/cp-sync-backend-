package com.cpsync.cpsync_backend.security;

import com.cpsync.cpsync_backend.model.User;
import com.cpsync.cpsync_backend.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final AuthTokenStore authTokenStore;

    @Value("${app.frontend.url:https://cp-sync-frontend.vercel.app/auth/callback}")
    private String frontendCallbackUrl;

    public OAuth2LoginSuccessHandler(OAuth2AuthorizedClientService authorizedClientService,
                                     UserService userService,
                                     JwtUtil jwtUtil,
                                     AuthTokenStore authTokenStore) {
        this.authorizedClientService = authorizedClientService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.authTokenStore = authTokenStore;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = oauthToken.getPrincipal();

        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName()
        );

        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
        OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();

        String googleId = oauth2User.getAttribute("sub");
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        LocalDateTime tokenExpiry = accessToken.getExpiresAt() != null
                ? LocalDateTime.ofInstant(accessToken.getExpiresAt(), ZoneOffset.UTC)
                : null;

        User savedUser = userService.upsertUserFromLogin(
                googleId, email, name,
                accessToken.getTokenValue(),
                refreshToken != null ? refreshToken.getTokenValue() : null,
                tokenExpiry
        );

        String jwt = jwtUtil.generateToken(savedUser.getId(), savedUser.getEmail());

        // FIXED: redirect with a short-lived one-time CODE, not the JWT itself.
        // The frontend exchanges this code for the JWT via POST /api/auth/exchange.
        // The JWT never appears in browser history, logs, or Referer headers.
        String code = authTokenStore.generateCode(jwt);
        response.sendRedirect(frontendCallbackUrl + "?code=" + code);
    }
}