package com.parrotalk.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import com.parrotalk.backend.client.ResendClient;
import com.parrotalk.backend.config.EmailProperties;
import com.parrotalk.backend.constant.EmailType;
import com.parrotalk.backend.dto.VerifyEmailMessage;
import com.parrotalk.backend.dto.resend.ResendEmailRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResendEmailService implements EmailService {

    private final ResendClient resendClient;
    private final EmailTemplateService templateService;
    private final EmailProperties emailProperties;

    @Override
    public void sendVerificationEmail(VerifyEmailMessage message) {
        log.info("Preparing verification email for: {}", message.email());
        
        Context context = new Context();
        context.setVariable("appName", emailProperties.getFrom());
        context.setVariable("userName", message.fullName());
        context.setVariable("verificationUrl", message.verificationUrl());
        context.setVariable("expirationMinutes", message.expirationMinutes());
        context.setVariable("supportEmail", emailProperties.getReplyTo());

        String html = templateService.processHtml(EmailType.VERIFY_EMAIL, context);
        
        String plainText = """
                Hello %s,

                Thank you for registering with %s.
                Verify your email address using this link:
                %s

                This verification link will expire in %d minutes.
                If you did not create this account, you can safely ignore this email.
                """.formatted(
                message.fullName(),
                emailProperties.getFrom(),
                message.verificationUrl(),
                message.expirationMinutes());

        ResendEmailRequest request = ResendEmailRequest.builder()
                .from(emailProperties.getFrom())
                .to(List.of(message.email()))
                .subject("[%s] Verify your email address".formatted("ParroTalk"))
                .html(html)
                .text(plainText)
                .replyTo(emailProperties.getReplyTo())
                .build();

        resendClient.sendEmail(request);
    }

    @Override
    public void sendPasswordResetEmail(String email, String resetUrl, String userName) {
        log.info("Preparing password reset email for: {}", email);
        
        Context context = new Context();
        context.setVariable("appName", emailProperties.getFrom());
        context.setVariable("userName", userName);
        context.setVariable("resetUrl", resetUrl);
        context.setVariable("supportEmail", emailProperties.getReplyTo());

        String html = templateService.processHtml(EmailType.RESET_PASSWORD, context);
        
        String plainText = """
                Hello %s,

                You requested to reset your password.
                Please click the link below to reset it:
                %s

                If you did not request this, please ignore this email.
                """.formatted(userName, resetUrl);

        ResendEmailRequest request = ResendEmailRequest.builder()
                .from(emailProperties.getFrom())
                .to(List.of(email))
                .subject("[%s] Password Reset Request".formatted("ParroTalk"))
                .html(html)
                .text(plainText)
                .replyTo(emailProperties.getReplyTo())
                .build();

        resendClient.sendEmail(request);
    }

    @Override
    public void sendWelcomeEmail(String email, String userName) {
        log.info("Preparing welcome email for: {}", email);
        
        Context context = new Context();
        context.setVariable("appName", emailProperties.getFrom());
        context.setVariable("userName", userName);
        context.setVariable("supportEmail", emailProperties.getReplyTo());

        String html = templateService.processHtml(EmailType.WELCOME, context);
        
        String plainText = """
                Hello %s,

                Welcome to %s! We are excited to have you on board.
                If you have any questions, feel free to reply to this email.
                """.formatted(userName, "ParroTalk");

        ResendEmailRequest request = ResendEmailRequest.builder()
                .from(emailProperties.getFrom())
                .to(List.of(email))
                .subject("Welcome to %s!".formatted("ParroTalk"))
                .html(html)
                .text(plainText)
                .replyTo(emailProperties.getReplyTo())
                .build();

        resendClient.sendEmail(request);
    }

    @Override
    public void sendEmailChangeNotification(String oldEmail, String newEmail, String userName) {
        log.info("Preparing email change notification for: {}", oldEmail);
        
        Context context = new Context();
        context.setVariable("appName", emailProperties.getFrom());
        context.setVariable("userName", userName);
        context.setVariable("newEmail", newEmail);
        context.setVariable("supportEmail", emailProperties.getReplyTo());

        String html = templateService.processHtml(EmailType.CHANGE_EMAIL, context);
        
        String plainText = """
                Hello %s,

                Your email address for %s has been changed to %s.
                If you did not request this change, please contact support immediately.
                """.formatted(userName, "ParroTalk", newEmail);

        ResendEmailRequest request = ResendEmailRequest.builder()
                .from(emailProperties.getFrom())
                .to(List.of(oldEmail))
                .subject("[%s] Email Address Changed".formatted("ParroTalk"))
                .html(html)
                .text(plainText)
                .replyTo(emailProperties.getReplyTo())
                .build();

        resendClient.sendEmail(request);
    }
}
