package com.parrotalk.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Password-reset configuration properties.
 *
 * @author MinhTuMTN
 */
@Data
@Component
@ConfigurationProperties(prefix = "application.password-reset")
public class PasswordResetProperties {

    /** How many minutes a reset token stays valid. */
    private long tokenExpirationMinutes = 30;

    /** Maximum forgot-password requests per hour (per IP or per email). */
    private int maxRequestsPerHour = 5;
}
