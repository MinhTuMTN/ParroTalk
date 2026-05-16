package com.parrotalk.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.parrotalk.backend.constant.LessonStatus;
import com.parrotalk.backend.entity.Lesson;
import com.parrotalk.backend.entity.TranscriptionSegment;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.util.SentenceUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;

/**
 * Process audio result from AI.
 * 
 * @author MinhTuMTN
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AudioProcessingService {

    /** Lesson service */
    private final LessonService lessonService;

    /** Transcription segment service */
    private final TranscriptionSegmentService segmentService;

    /**
     * Process audio result from AI
     * 
     * @param node Result node from AI
     */
    public void process(JsonNode node) {
        try {
            String lessonId = node.get("lessonId").asString();
            Lesson lesson = lessonService.findLessonForUpdate(UUID.fromString(lessonId));

            String status = node.get("status").asString();
            // Handle progress status
            if ("PROGRESS".equalsIgnoreCase(status)) {
                // Update lesson progress percentage and current step
                int progress = node.get("progress").asInt();
                String step = node.get("message").asString();
                lessonService.updateProgress(lesson, progress, step, LessonStatus.PROCESSING, 0);
                return;
            }

            // Handle failed status
            if ("FAILED".equalsIgnoreCase(status)) {
                // Get error info and update lesson status to FAILED
                String errorInfo = node.has("error") ? node.get("error").asString() : "Unknown AI error";
                log.error(errorInfo);
                lessonService.updateProgress(lesson, 0, "AI Error", LessonStatus.FAILED, 0);
                return;
            }

            // Handle completed status
            // Save result from AI and update lesson status to DONE
            JsonNode resultNode = node.get("result");
            if (resultNode != null && resultNode.has("segments")) {
                int totalSegments = saveAllTranscriptionSegment(lesson, resultNode);
                if (node.has("duration")) {
                    lesson.setDuration(node.get("duration").asInt());
                }
                lessonService.updateProgress(lesson, 100, "Completed", LessonStatus.DONE, totalSegments);
            }

        } catch (Exception e) {
            log.error("Failed to process result from RabbitMQ", e);
            throw new ParroTalkException("Requeue message", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Save all transcription segment from AI result.
     * 
     * @param lesson     Lesson
     * @param resultNode Result node data
     * @return Number of segments saved
     */
    private int saveAllTranscriptionSegment(Lesson lesson, JsonNode resultNode) {
        log.info("Saving all transcription segment for lesson: {}", lesson.getId());

        int order = 1;
        List<TranscriptionSegment> segments = new ArrayList<>();
        for (JsonNode seg : resultNode.get("segments")) {
            TranscriptionSegment segment = TranscriptionSegment.builder()
                    .lesson(lesson)
                    .text(seg.get("text").asString().trim())
                    .startTime(seg.get("start").asDouble())
                    .endTime(seg.get("end").asDouble())
                    .displayOrder(order++)
                    .difficulty(SentenceUtil.getSentenceDifficulty(seg.get("text").asString()))
                    .build();
            segments.add(segment);
        }
        return segmentService.saveAllTranscriptionSegment(segments);
    }
}
