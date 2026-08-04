package com.parrotalk.backend.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Daily job that removes expired password-reset tokens from the database.
 *
 * @author MinhTuMTN
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordResetTokenCleanupScheduler {

    private final PasswordResetService passwordResetService;

    /**
     * Runs every day at 03:00 AM server time.
     * Deletes tokens whose {@code expired_at} is more than 24 hours in the past.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpiredTokens() {
        log.info("Starting scheduled password-reset token cleanup");
        int deleted = passwordResetService.cleanupExpiredTokens();
        log.info("Scheduled cleanup completed — removed {} token(s)", deleted);
    }
}
