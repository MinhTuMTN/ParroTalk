package com.parrotalk.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.parrotalk.backend.entity.Lesson;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SseService {
    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Connect to SSE
     * 
     * @param jobId Job ID
     * @return SseEmitter
     */
    public SseEmitter connect(UUID jobId) {
        log.info("New SSE connection for job: {}", jobId);
        SseEmitter emitter = new SseEmitter(3600000L); // 1 hour timeout
        emitters.put(jobId, emitter);

        emitter.onCompletion(() -> {
            log.info("SSE connection completed for job: {}", jobId);
            emitters.remove(jobId);
        });
        emitter.onTimeout(() -> {
            log.warn("SSE connection timeout for job: {}", jobId);
            emitters.remove(jobId);
        });
        emitter.onError(e -> {
            log.error("SSE connection error for job: {}: {}", jobId, e.getMessage());
            emitters.remove(jobId);
        });

        // Send a ping immediately
        try {
            emitter.send(SseEmitter.event().data("connected"));
        } catch (IOException e) {
            log.error("Failed to send initial ping: {}", e.getMessage());
        }

        return emitter;
    }

    /**
     * Send SSE event
     * 
     * @param jobId Job ID
     * @param event Event data
     */
    public void sendEvent(UUID jobId, Object event) {
        SseEmitter emitter = emitters.get(jobId);
        if (emitter != null) {
            try {
                log.info("Sending SSE event to {}: {}", jobId, event);
                emitter.send(SseEmitter.event().data(event));
            } catch (IOException e) {
                log.error("Failed to send event to {}: {}", jobId, e.getMessage());
                emitters.remove(jobId);
            }
        } else {
            log.warn("No emitter found for job: {}", jobId);
        }
    }

    /**
     * Send SSE event for lesson update
     * 
     * @param lesson Lesson
     */
    public void sendUpdatedLessonSSE(Lesson lesson) {
        Map<String, Object> message = Map.of(
                "status", lesson.getStatus(),
                "progress", lesson.getProgress(),
                "step", lesson.getCurrentStep());
        sendEvent(lesson.getId(), message);
    }
}
