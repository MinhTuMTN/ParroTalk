package com.parrotalk.backend.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.parrotalk.backend.repository.AiUsageEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Daily job that purges AI usage events older than the configured retention. */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiUsageCleanupScheduler {

    private final AiUsageEventRepository repository;

    @Value("${app.ai-usage.retention-days:30}")
    private int retentionDays = 30;

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public int cleanupOldEvents() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
        int deleted = repository.deleteByCreatedAtBefore(cutoff);
        log.info("AI usage cleanup completed — removed {} event(s) older than {} day(s)", deleted, retentionDays);
        return deleted;
    }
}
