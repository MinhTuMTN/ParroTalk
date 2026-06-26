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
}
