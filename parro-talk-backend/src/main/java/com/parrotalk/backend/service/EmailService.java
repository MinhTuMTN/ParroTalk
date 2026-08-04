package com.parrotalk.backend.service;

import com.parrotalk.backend.dto.VerifyEmailMessage;

/**
 * Service interface for email operations.
 * 
 * @author MinhTuMTN
 */
public interface EmailService {

    /**
     * Sends an email with verification information.
     * 
     * @param message The verification email message containing user details and
     *                verification token
     */
    void sendVerificationEmail(VerifyEmailMessage message);

    /**
     * Sends a password reset email.
     */
    void sendPasswordResetEmail(String email, String resetUrl, String userName);

    /**
     * Sends a welcome email to a new user.
     */
    void sendWelcomeEmail(String email, String userName);

    /**
     * Sends a notification when user changes their email.
     */
    void sendEmailChangeNotification(String oldEmail, String newEmail, String userName);
}
