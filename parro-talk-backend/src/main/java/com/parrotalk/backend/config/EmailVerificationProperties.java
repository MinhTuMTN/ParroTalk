package com.parrotalk.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Email verification-related configuration properties.
 * 
 * @author MinhTuMTN
 */
@Data
@Component
@ConfigurationProperties(prefix = "application.email-verification")
public class EmailVerificationProperties {

    /** Token expiration time in minutes */
    private long tokenExpirationMinutes = 30;

    /** Resend cooldown in seconds */
    private long resendCooldownSeconds = 60;

    /** Metadata retention in minutes */
    private long metadataRetentionMinutes = 1440;
}
