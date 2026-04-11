package com.parrotalk.backend.controller;

import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.dto.UploadResponse;
import com.parrotalk.backend.entity.Lesson;
import com.parrotalk.backend.service.AudioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/audio")
@RequiredArgsConstructor
public class AudioController {

    private final AudioService audioService;

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
}
