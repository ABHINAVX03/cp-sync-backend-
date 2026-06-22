package com.cpsync.cpsync_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.admin-email}")
    private String adminEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendAccessRequestNotification(String userEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(adminEmail);
        message.setSubject("CPSync Access Request");
        message.setText("A user has requested access to CPSync:\n\n"
                + "Email: " + userEmail + "\n\n"
                + "Add them as a test user in Google Console: "
                + "https://console.cloud.google.com/apis/credentials/consent");
        mailSender.send(message);
    }
}