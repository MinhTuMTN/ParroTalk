package com.parrotalk.backend.service;

import com.parrotalk.backend.entity.User;

import com.parrotalk.backend.constant.LessonStatus;
import com.parrotalk.backend.dto.CloudinaryDto;
import com.parrotalk.backend.entity.Lesson;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    /** Transcription segment service */
    private final TranscriptionSegmentService transcriptionSegmentService;

    /** Audio task producer */
    private final AudioTaskProducer audioTaskProducer;

    /**
     * Process upload.
     * 
     * @param file  Audio file
     * @param owner Uploaded user
     * @return Lesson ID
     */
    public UUID processUpload(MultipartFile file, String lessonTitle, User owner) {
        try {
            // Hashing file
            String fileHash = DigestUtils.md5DigestAsHex(file.getInputStream());

            // Find lesson with same file hash
            Optional<Lesson> existingLesson = lessonService.findByFileHash(fileHash);

            // If exist and not failed, return it
            if (existingLesson.isPresent()) {
                Lesson lesson = existingLesson.get();
                if (lesson.getStatus() != LessonStatus.FAILED) {
                    return lesson.getId();
                }
            }

            // Otherwise, upload new file to storage (using Cloudinary)
            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            CloudinaryDto cloudinaryDto = storageService.store(file, filename);

            // Save new lesson to database with PENDING status
            Lesson lesson = lessonService.createLesson(
                    cloudinaryDto.getUrl(),
                    fileHash,
                    owner,
                    cloudinaryDto.getDuration(),
                    lessonTitle);

            // Send task to RabbitMQ
            audioTaskProducer.sendTranscriptionTask(lesson.getId(), cloudinaryDto.getUrl());

            // Return lesson
            return lesson.getId();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate checksum for file", e);
        }
    }

    /**
     * Process YouTube URL.
     * 
     * @param request YouTube upload request
     * @param owner   Authenticated user
     * @return Lesson ID
     */
    public UUID processYoutube(com.parrotalk.backend.dto.YoutubeUploadRequest request, User owner) {
        String url = request.getUrl();
        String title = request.getTitle();

        // Use URL hash to find existing lesson
        String fileHash = DigestUtils.md5DigestAsHex(url.getBytes());
        Optional<Lesson> existingLesson = lessonService.findByFileHash(fileHash);

        if (existingLesson.isPresent()) {
            Lesson lesson = existingLesson.get();
            if (lesson.getStatus() != LessonStatus.FAILED) {
                return lesson.getId();
            }
        }

        // Create new lesson without audio file upload (just the URL)
        Lesson lesson = lessonService.createLesson(
                url, // Use YT URL as the fileUrl
                fileHash,
                owner,
                0, // Duration unknown initially
                title);

        // Send task to RabbitMQ for the worker to process the YT URL
        audioTaskProducer.sendTranscriptionTask(lesson.getId(), url);

        return lesson.getId();
    }

    /**
     * Retry lesson.
     * 
     * @param lessonId Lesson id
     * @return Lesson
     */
    @Transactional
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
        transcriptionSegmentService.deleteByLessonId(lessonId);

        // Send task to RabbitMQ
        audioTaskProducer.sendTranscriptionTask(lesson.getId(), lesson.getFileUrl());

        // Return lesson
        return lesson;
    }

    /**
     * Retry all processing lessons.
     * Retry all lessons that are in PROCESSING status and created before 1 hour.
     */
    @Transactional
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

            transcriptionSegmentService.deleteByLessonId(lesson.getId());
            audioTaskProducer.sendTranscriptionTask(lesson.getId(), lesson.getFileUrl());
        }
    }
}
