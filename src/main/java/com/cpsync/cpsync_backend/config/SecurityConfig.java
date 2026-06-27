package com.cpsync.cpsync_backend.config;

import com.cpsync.cpsync_backend.security.JwtAuthFilter;
import com.cpsync.cpsync_backend.security.OAuth2LoginSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.servlet.CookieSameSiteSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.savedrequest.CookieRequestCache;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final JwtAuthFilter jwtAuthFilter;

    @Value("${app.cors.allowed-origins:https://cp-sync-frontend.vercel.app,http://localhost:3000}")
    private String allowedOrigins;

    public SecurityConfig(OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler, JwtAuthFilter jwtAuthFilter) {
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .requestCache(cache -> cache.requestCache(new CookieRequestCache()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/oauth2/**", "/login/**").permitAll()
                        // Only the exact /api/contests (GET) is public – the /mine variant requires auth
                        .requestMatchers("/api/contests").permitAll()
                        .requestMatchers("/api/request-access").permitAll()
                        .requestMatchers("/api/auth/exchange").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestResolver(
                                        new CustomAuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization")
                                )
                        )
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler((req, res, exp) -> {
                            logger.error("OAuth2 Login Failed: {}", exp.getMessage(), exp);
                            String errMsg = exp.getMessage() != null ? exp.getMessage() : "Unknown OAuth2 error";
                            res.sendRedirect(req.getContextPath() + "/login?error=" + URLEncoder.encode(errMsg, StandardCharsets.UTF_8));
                        })
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CookieSameSiteSupplier cookieSameSiteSupplier() {
        return CookieSameSiteSupplier.ofNone().whenHasName("JSESSIONID");
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        config.setExposedHeaders(List.of("Retry-After"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}