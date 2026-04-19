package com.parrotalk.backend.service;

import com.parrotalk.backend.config.RabbitMQConfig;
import com.parrotalk.backend.constant.LessonStatus;
import com.parrotalk.backend.entity.Lesson;
import com.parrotalk.backend.entity.TranscriptionSegment;
import com.parrotalk.backend.repository.TranscriptionSegmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudioResultConsumer {

    private final LessonService lessonService;
    private final TranscriptionSegmentRepository segmentRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    @RabbitListener(queues = RabbitMQConfig.RESULT_QUEUE)
    public void receiveResult(Message message) {
        try {
            JsonNode node = mapper.readTree(message.getBody());
            String lessonIdStr = node.get("lessonId").asString();
            UUID lessonId = UUID.fromString(lessonIdStr);
            String status = node.get("status").asString();

            log.info("Received message for lesson: {}, status: {}", lessonId, status);

            if ("PROGRESS".equalsIgnoreCase(status)) {
                int progress = node.get("progress").asInt();
                String step = node.get("message").asString();
                lessonService.updateProgress(lessonId, progress, step, LessonStatus.PROCESSING, 0);
                return;
            }

            if ("FAILED".equalsIgnoreCase(status)) {
                String errorInfo = node.has("error") ? node.get("error").asString() : "Unknown AI error";
                log.error(errorInfo);
                lessonService.updateProgress(lessonId, 0, "AI Error", LessonStatus.FAILED, 0);
                return;
            }

            JsonNode resultNode = node.get("result");
            if (resultNode != null && resultNode.has("segments")) {
                int totalSegments = saveRealResults(lessonId, resultNode);
                lessonService.updateProgress(lessonId, 100, "Completed", LessonStatus.DONE, totalSegments);
            }

        } catch (Exception e) {
            log.error("Failed to process result from RabbitMQ", e);
            throw new RuntimeException("Requeue message", e);
        }
    }

    private int saveRealResults(UUID lessonId, JsonNode resultNode) {
        log.info("Saving real results for lesson: {}", lessonId);

        Lesson lesson = lessonService.findById(lessonId).orElseThrow(() -> new RuntimeException("Lesson not found"));
        List<TranscriptionSegment> segments = new ArrayList<>();
        for (JsonNode seg : resultNode.get("segments")) {
            TranscriptionSegment segment = TranscriptionSegment.builder()
                    .lesson(lesson)
                    .text(seg.get("text").asString().trim())
                    .startTime(seg.get("start").asDouble())
                    .endTime(seg.get("end").asDouble())
                    .build();
            segments.add(segment);
        }
        segmentRepository.saveAll(segments);

        return segments.size();
    }
}
