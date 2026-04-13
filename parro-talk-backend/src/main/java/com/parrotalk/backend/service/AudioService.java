package com.parrotalk.backend.service;

import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.constant.LessonStatus;
import com.parrotalk.backend.entity.Lesson;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Audio Service.
 * 
 * @author MinhTuMTN
 */
@Service
@RequiredArgsConstructor
public class AudioService {

    /** Storage service */
    private final StorageService storageService;

    /** Lesson service */
    private final LessonService lessonService;

    /** Audio task producer */
    private final AudioTaskProducer audioTaskProducer;

    /**
     * Process upload.
     * 
     * @param file  Audio file
     * @param owner Uploaded user
     * @return Lesson
     */
    public Lesson processUpload(MultipartFile file, User owner) {
        try {
            // Hashing file
            String fileHash = DigestUtils.md5DigestAsHex(file.getInputStream());

            // Find lesson with same file hash
            Optional<Lesson> existingLesson = lessonService.findByFileHash(fileHash);

            // If exist and not failed, return it
            if (existingLesson.isPresent()) {
                Lesson lesson = existingLesson.get();
                if (lesson.getStatus() != LessonStatus.FAILED) {
                    return lesson;
                }
            }

            // Otherwise, upload new file to storage (using Cloudinary)
            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            String fileUrl = storageService.store(file, filename);

            // Save new lesson to database with PENDING status
            Lesson lesson = lessonService.createLesson(fileUrl, fileHash, owner);

            // Send task to RabbitMQ
            audioTaskProducer.sendTranscriptionTask(lesson.getId(), fileUrl);

            // Return lesson
            return lesson;
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate checksum for file", e);
        }
    }

    /**
     * Retry lesson.
     * 
     * @param lessonId Lesson id
     * @return Lesson
     */
    public Lesson retryLesson(UUID lessonId) {
        // Find lesson by id
        Lesson lesson = lessonService.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));

        // Update lesson status to PENDING
        lesson.setStatus(LessonStatus.PENDING);
        lesson.setProgress(0);
        lesson.setCurrentStep("Uploading");
        lessonService.save(lesson);

        // Delete all relative TransactionSegment
        
        // Send task to RabbitMQ
        audioTaskProducer.sendTranscriptionTask(lesson.getId(), lesson.getFileUrl());

        // Return lesson
        return lesson;
    }

    /**
     * Retry all processing lessons.
     * Retry all lessons that are in PROCESSING status and created before 1 hour.
     */
    public void retryAllProcessingLesson() {
        // Find all broken lessons
        List<Lesson> lessons = lessonService.findAllBrokenLessons();

        // Retry each lesson
        for (Lesson lesson : lessons) {
            // Update lesson status to PENDING
            lesson.setStatus(LessonStatus.PENDING);
            lesson.setProgress(0);
            lesson.setCurrentStep("Uploading");
            lessonService.save(lesson);
            audioTaskProducer.sendTranscriptionTask(lesson.getId(), lesson.getFileUrl());
        }
    }
}
