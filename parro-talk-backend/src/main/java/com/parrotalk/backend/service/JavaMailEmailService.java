package com.parrotalk.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.parrotalk.backend.config.ApplicationMailProperties;
import com.parrotalk.backend.dto.VerifyEmailMessage;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JavaMailEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final ApplicationMailProperties mailProperties;

    @Value("${spring.mail.username:no-reply@parrotalk.local}")
    private String fromAddress;

    @Override
    public void sendVerificationEmail(VerifyEmailMessage message) {
        try {
            Context context = new Context();
            context.setVariable("appName", mailProperties.getAppName());
            context.setVariable("userName", message.fullName());
            context.setVariable("verificationUrl", message.verificationUrl());
            context.setVariable("expirationMinutes", message.expirationMinutes());
            context.setVariable("supportEmail", mailProperties.getSupportEmail());

            String html = templateEngine.process("email/verify-email", context);
            String plainText = """
                    Hello %s,

                    Thank you for registering with %s.
                    Verify your email address using this link:
                    %s

                    This verification link will expire in %d minutes.
                    If you did not create this account, you can safely ignore this email.
                    """.formatted(
                    message.fullName(),
                    mailProperties.getAppName(),
                    message.verificationUrl(),
                    message.expirationMinutes());

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(message.email());
            helper.setSubject("[%s] Verify your email address".formatted(mailProperties.getAppName()));
            helper.setText(plainText, html);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send verification email", e);
        }
    }
}
