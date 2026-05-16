package com.parrotalk.backend.controller;

import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.dto.UploadResponse;
import com.parrotalk.backend.dto.YoutubeUploadRequest;
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
@PreAuthorize("hasAnyRole('ADMIN', 'PRO_USER')")
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
            @RequestParam MultipartFile file,
            @RequestParam String title,
            @AuthenticationPrincipal User user) {
        UUID lessonId = audioService.processUpload(file, title, user);
        return ResponseEntity.ok(UploadResponse.builder()
                .lessonId(lessonId)
                .message("File uploaded successfully and processing started")
                .build());
    }

    /**
     * Process YouTube upload.
     * 
     * @param request YouTube upload request
     * @param user    Uploaded user
     * @return Upload response
     */
    @PostMapping("/youtube")
    public ResponseEntity<UploadResponse> processYoutube(
            @RequestBody YoutubeUploadRequest request,
            @AuthenticationPrincipal User user) {
        UUID lessonId = audioService.processYoutube(request, user);
        return ResponseEntity.ok(UploadResponse.builder()
                .lessonId(lessonId)
                .message("YouTube video added successfully and processing started")
                .build());
    }

    /**
     * Retry process lesson.
     * 
     * @param lessonId Lesson id
     * @return Upload response
     */
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
     * @throws InterruptedException
     */
    @GetMapping("/retry-all-processing-lesson")
    public ResponseEntity<UploadResponse> retryAllProcessingLesson() {
        audioService.retryAllProcessingLesson();
        return ResponseEntity.ok(UploadResponse.builder()
                .message("All processing lessons have been retried")
                .build());
    }

}
