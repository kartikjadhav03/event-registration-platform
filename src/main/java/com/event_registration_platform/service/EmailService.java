
package com.event_registration_platform.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String token) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(toEmail);
        message.setSubject("Verify your email");

        message.setText(
                "Click to verify your email: " +
                        "http://localhost:8080/api/auth/verify?token=" + token
        );

        mailSender.send(message);
    }

    public void sendPasswordResetEmail(String toEmail, String token) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(toEmail);
        message.setSubject("Reset your password");

        message.setText(
                "Use this token to reset your password " +
                        "(valid for 15 minutes): " + token
        );

        mailSender.send(message);
    }
}
