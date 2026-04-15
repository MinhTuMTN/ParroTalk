package com.parrotalk.backend.controller;

import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.dto.UploadResponse;
import com.parrotalk.backend.entity.Lesson;
import com.parrotalk.backend.service.AudioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

/**
 * Audio controller.
 * 
 * @author MinhTuMTN
 */
@RestController
@RequestMapping("/api/audio")
@RequiredArgsConstructor
public class AudioController {

    /** Audio service */
    private final AudioService audioService;

    /**
     * Upload new audio lesson.
     *
     * @param file Audio file
     * @param user Uploaded user
     * @return Upload response
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> uploadAudio(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) {
        Lesson lesson = audioService.processUpload(file, user);
        return ResponseEntity.ok(UploadResponse.builder()
                .lessonId(lesson.getId())
                .message("File uploaded successfully and processing started")
                .build());
    }

    /**
     * Retry process lesson.
     * 
     * @param lessonId Lesson id
     * @return Upload response
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/retry/{lessonId}")
    public ResponseEntity<UploadResponse> retryLesson(@PathVariable UUID lessonId) {
        Lesson lesson = audioService.retryLesson(lessonId);
        return ResponseEntity.ok(UploadResponse.builder()
                .lessonId(lesson.getId())
                .message("File processed successfully")
                .build());
    }

    /**
     * Retry all processing lessons.
     * Retry all lessons that are in PROCESSING status and created before 1 hour.
     * 
     * @return Upload response
     */
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/retry-all-processing-lesson")
    public ResponseEntity<UploadResponse> retryAllProcessingLesson() {
        audioService.retryAllProcessingLesson();
        return ResponseEntity.ok(UploadResponse.builder()
                .message("All processing lessons have been retried")
                .build());
    }

}
