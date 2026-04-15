package com.parrotalk.backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parrotalk.backend.constant.LessonStatus;
import com.parrotalk.backend.constant.Role;
import com.parrotalk.backend.dto.LessonResponse;
import com.parrotalk.backend.entity.Lesson;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.repository.LessonRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Lesson Service.
 * 
 * @author MinhTuMTN
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LessonService {

    /** Lesson repository */
    private final LessonRepository lessonRepository;

    /** SSE service */
    private final SseService sseService;

    /**
     * Create a new lesson
     * 
     * @param fileUrl  audio file url
     * @param fileHash audio file hash
     * @param owner    owner of the lesson
     * @return Lesson
     */
    @Transactional
    public Lesson createLesson(String fileUrl, String fileHash, User owner) {
        Lesson lesson = Lesson.builder()
                .fileUrl(fileUrl)
                .fileHash(fileHash)
                .ownerId(owner.getId())
                .build();
        return lessonRepository.save(lesson);
    }

    /**
     * Find lesson by file hash.
     * 
     * @param fileHash File hash
     * @return Lesson
     */
    public Optional<Lesson> findByFileHash(String fileHash) {
        return lessonRepository.findByFileHash(fileHash);
    }

    /**
     * Get the lesson response.
     * Used by user get lesson status after upload.
     * 
     * @param lessonId Lesson id
     * @return Lesson response
     */
    public LessonResponse getLessonResponse(UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));
        return mapToResponse(lesson);
    }

    public List<LessonResponse> getAllLessons(User user) {
        List<Lesson> lessons;
        if (user.getRole() == Role.ADMIN) {
            lessons = lessonRepository.findAll();
        } else {
            // Logic for regular users: see their own lessons
            lessons = lessonRepository.findAllByOwnerId(user.getId());
        }

        return lessons.stream()
                .map(this::mapToResponse)
                .sorted((j1, j2) -> j2.getCreatedAt().compareTo(j1.getCreatedAt()))
                .collect(Collectors.toList());
    }

    private LessonResponse mapToResponse(Lesson lesson) {
        return LessonResponse.builder()
                .id(lesson.getId())
                .status(lesson.getStatus())
                .progress(lesson.getProgress())
                .currentStep(lesson.getCurrentStep())
                .fileUrl(lesson.getFileUrl())
                .createdAt(lesson.getCreatedAt())
                .mediaType(lesson.getMediaType().name())
                .sourceType(lesson.getSourceType().name())
                .build();
    }

    @Transactional
    public void updateProgress(UUID lessonId, int progress, String step, LessonStatus status) {
        log.info("Updating progress for lesson: {}, progress: {}, step: {}, status: {}", lessonId, progress, step,
                status);
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow(() -> new RuntimeException("Lesson not found"));
        boolean updated = false;

        if (status != null && lesson.getStatus() != status) {
            lesson.setStatus(status);
            updated = true;
        }
        if (lesson.getProgress() != progress) {
            lesson.setProgress(progress);
            updated = true;
        }
        if (step != null && !step.equals(lesson.getCurrentStep())) {
            lesson.setCurrentStep(step);
            updated = true;
        }

        if (updated) {
            lessonRepository.save(lesson);
            sseService.sendEvent(lessonId, Map.of(
                    "status", lesson.getStatus(),
                    "progress", lesson.getProgress(),
                    "step", lesson.getCurrentStep()));
        }
    }

    @Transactional
    public void deleteLesson(UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow(() -> new RuntimeException("Lesson not found"));
        lessonRepository.delete(lesson);
    }

    /**
     * Find lesson by id.
     * 
     * @param lessonId Lesson id
     * @return Lesson
     */
    public Optional<Lesson> findById(UUID lessonId) {
        return lessonRepository.findById(lessonId);
    }

    /**
     * Save lesson.
     * 
     * @param lesson Lesson
     */
    @Transactional
    public void save(Lesson lesson) {
        lessonRepository.save(lesson);
    }

    /**
     * Find all broken lessons.
     * 
     * @return List of broken lessons
     */
    public List<Lesson> findAllBrokenLessons() {
        // One hour before now
        LocalDateTime tenHourBefore = LocalDateTime.now().minusHours(10);
        // // Pageable with 50 items per page
        // Pageable pageable = PageRequest.of(0, 50);
        // // Find all lessons with status PROCESSING and created at before one hour
        // before
        // // now
        // return
        // lessonRepository.findAllByStatusAndCreatedAtBefore(LessonStatus.PROCESSING,
        // oneHourBefore, pageable)
        // .getContent();
        // return lessonRepository.findAll();
        return lessonRepository.findAllByStatusAndCreatedAtBefore(LessonStatus.DONE, tenHourBefore);
    }
}
