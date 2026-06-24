package com.cpsync.cpsync_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class EmailService {

    private final RestClient restClient;

    public EmailService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }
    @Value("${sendgrid.api-key:}")
    private String apiKey;

    @Value("${sendgrid.from-email:noreply@cpsync.dev}")
    private String fromEmail;

    public void sendWelcomeEmail(String toEmail) {
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("SendGrid API key not configured");
            return;
        }

        String subject = "🎉 Access granted – welcome to CPSync!";
        String body = "Hello,\n\n"
                + "Your request for early access to CPSync has been approved!\n\n"
                + "You can now sign in with your Google account (" + toEmail + ") at:\n"
                + "https://cp-sync-frontend.vercel.app\n\n"
                + "CPSync automatically syncs coding contests from Codeforces, LeetCode, "
                + "CodeChef, and AtCoder directly to your Google Calendar.\n\n"
                + "To get started:\n"
                + "1. Sign in with Google.\n"
                + "2. Choose your platforms on the Profile page.\n"
                + "3. Click \"Sync to Calendar\" to push contests instantly.\n\n"
                + "If you have any questions, just reply to this email.\n\n"
                + "— The CPSync Team";

        try {
            restClient.post()
                    .uri("https://api.sendgrid.com/v3/mail/send")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "personalizations", new Object[]{
                                    Map.of("to", new Object[]{ Map.of("email", toEmail) })
                            },
                            "from", Map.of("email", fromEmail),
                            "subject", subject,
                            "content", new Object[]{
                                    Map.of("type", "text/plain", "value", body)
                            }
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }
    }
}