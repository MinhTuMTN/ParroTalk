package com.parrotalk.backend.service;

import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.Lesson;
import com.parrotalk.backend.entity.LessonStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AudioService {

    private final StorageService storageService;
    private final LessonService lessonService;
    private final TranscriptionProcessingService processingService;

    public Lesson processUpload(MultipartFile file, User owner) {
        try {
            String fileHash = DigestUtils.md5DigestAsHex(file.getInputStream());
            Optional<Lesson> existingLesson = lessonService.findByFileHash(fileHash);

            if (existingLesson.isPresent()) {
                Lesson lesson = existingLesson.get();
                if (lesson.getStatus() != LessonStatus.FAILED) {
                    return lesson;
                }
            }

            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            String fileUrl = storageService.store(file, filename);

            Lesson lesson = lessonService.createLesson(fileUrl, fileHash, owner);

            processingService.startTranscription(lesson.getId(), file);

            return lesson;
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate checksum for file", e);
        }
    }
}
