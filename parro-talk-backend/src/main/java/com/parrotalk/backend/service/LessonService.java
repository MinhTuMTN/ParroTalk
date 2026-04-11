package com.parrotalk.backend.service;

import com.parrotalk.backend.entity.Role;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.dto.LessonResponse;
import com.parrotalk.backend.entity.Lesson;
import com.parrotalk.backend.entity.LessonStatus;
import com.parrotalk.backend.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonService {
    private final LessonRepository lessonRepository;
    private final SseService sseService;

    @Transactional
    public Lesson createLesson(String fileUrl, String fileHash, User owner) {
        Lesson lesson = Lesson.builder()
            .fileUrl(fileUrl)
            .fileHash(fileHash)
            .ownerId(owner.getId())
            .build();
        return lessonRepository.save(lesson);
    }

    public Optional<Lesson> findByFileHash(String fileHash) {
        return lessonRepository.findByFileHash(fileHash);
    }

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
                .youtubeUrl(lesson.getYoutubeUrl())
                .build();
    }

    @Transactional
    public void updateProgress(UUID lessonId, int progress, String step, LessonStatus status) {
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
                "step", lesson.getCurrentStep()
            ));
        }
    }

    @Transactional
    public void deleteLesson(UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow(() -> new RuntimeException("Lesson not found"));
        lessonRepository.delete(lesson);
    }
}
