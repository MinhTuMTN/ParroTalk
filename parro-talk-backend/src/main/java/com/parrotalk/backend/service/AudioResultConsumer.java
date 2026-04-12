package com.parrotalk.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parrotalk.backend.config.RabbitMQConfig;
import com.parrotalk.backend.entity.LessonStatus;
import com.parrotalk.backend.entity.SegmentType;
import com.parrotalk.backend.entity.TranscriptionSegment;
import com.parrotalk.backend.repository.TranscriptionSegmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
            String lessonIdStr = node.get("lessonId").asText();
            UUID lessonId = UUID.fromString(lessonIdStr);
            String status = node.get("status").asText();

            log.info("Received message for lesson: {}, status: {}", lessonId, status);

            if ("PROGRESS".equalsIgnoreCase(status)) {
                int progress = node.get("progress").asInt();
                String step = node.get("message").asText();
                lessonService.updateProgress(lessonId, progress, step, LessonStatus.PROCESSING);
                return;
            }

            if ("FAILED".equalsIgnoreCase(status)) {
                String errorInfo = node.has("error") ? node.get("error").asText() : "Unknown AI error";
                lessonService.updateProgress(lessonId, 0, "AI Error: " + errorInfo, LessonStatus.FAILED);
                return;
            }

            JsonNode resultNode = node.get("result");
            if (resultNode != null) {
                saveRealResults(lessonId, resultNode);
                lessonService.updateProgress(lessonId, 100, "Completed", LessonStatus.DONE);
            }

        } catch (Exception e) {
            log.error("Failed to process result from RabbitMQ", e);
            throw new RuntimeException("Requeue message", e);
        }
    }

    private void saveRealResults(UUID lessonId, JsonNode resultNode) {
        log.info("Saving real results for lesson: {}", lessonId);
        if (!resultNode.has("segments")) {
            return;
        }

        List<TranscriptionSegment> segments = new ArrayList<>();
        for (JsonNode seg : resultNode.get("segments")) {
            TranscriptionSegment segment = TranscriptionSegment.builder()
                    .lessonId(lessonId)
                    .text(seg.get("text").asText().trim())
                    .startTime(seg.get("start").asDouble())
                    .endTime(seg.get("end").asDouble())
                    .type(SegmentType.SENTENCE)
                    .build();
            segments.add(segment);
        }
        segmentRepository.saveAll(segments);
    }
}
