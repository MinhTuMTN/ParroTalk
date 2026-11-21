package com.parrotalk.backend.ai;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.parrotalk.backend.entity.AiUsageEvent;
import com.parrotalk.backend.repository.AiUsageEventRepository;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * Buffers AI usage events in a bounded in-memory queue and persists them in batches
 * on a background thread, never joining (or failing) the caller's transaction.
 */
@Component
@Slf4j
public class AiUsageRecorder {

    static final int BATCH_SIZE = 50;

    private final AiUsageEventRepository repository;
    private final BlockingQueue<AiUsageEvent> queue;
    private final AtomicBoolean flushScheduled = new AtomicBoolean(false);
    private final ExecutorService flushExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ai-usage-flush");
        t.setDaemon(true);
        return t;
    });

    @Autowired
    public AiUsageRecorder(
            AiUsageEventRepository repository,
            @Value("${app.ai-usage.queue-capacity:10000}") int queueCapacity) {
        this.repository = repository;
        this.queue = new ArrayBlockingQueue<>(Math.max(1, queueCapacity));
    }

    /**
     * Enqueues an event. Must be called on the caller thread (userId captured by caller).
     * Never throws; drops the event when the queue is full.
     */
    public void record(UUID userId, String provider, String model, String feature,
            int inputTokens, int outputTokens, String status) {
        try {
            AiUsageEvent event = AiUsageEvent.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .provider(truncate(provider, 60))
                    .model(truncate(model, 120))
                    .feature(truncate(feature == null ? AiFeature.UNKNOWN : feature, 60))
                    .inputTokens(Math.max(0, inputTokens))
                    .outputTokens(Math.max(0, outputTokens))
                    .status(truncate(status == null ? "OK" : status, 20))
                    .createdAt(Instant.now())
                    .build();
            if (!queue.offer(event)) {
                log.warn("AI usage queue full; dropping usage event (model: {})", model);
                return;
            }
            if (queue.size() >= BATCH_SIZE && flushScheduled.compareAndSet(false, true)) {
                try {
                    flushExecutor.execute(() -> {
                        flushScheduled.set(false);
                        flush();
                    });
                } catch (RuntimeException e) {
                    flushScheduled.set(false);
                }
            }
        } catch (RuntimeException e) {
            log.warn("Failed to record AI usage event: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${app.ai-usage.flush-interval-ms:5000}")
    public void flush() {
        try {
            List<AiUsageEvent> batch = new ArrayList<>(BATCH_SIZE);
            while (queue.drainTo(batch, BATCH_SIZE) > 0) {
                persist(batch);
                batch = new ArrayList<>(BATCH_SIZE);
            }
        } catch (RuntimeException e) {
            log.warn("AI usage flush failed: {}", e.getMessage());
        }
    }

    private void persist(List<AiUsageEvent> batch) {
        try {
            repository.saveAll(batch);
        } catch (RuntimeException e) {
            log.warn("Failed to persist {} AI usage event(s); dropped: {}", batch.size(), e.getMessage());
        }
    }

    int pending() {
        return queue.size();
    }

    @PreDestroy
    public void shutdown() {
        flushExecutor.shutdown();
        flush();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "unknown";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
