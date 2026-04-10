package com.parrotalk.backend.controller;

import com.parrotalk.backend.dto.UploadResponse;
import com.parrotalk.backend.entity.Job;
import com.parrotalk.backend.service.AudioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/audio")
public class AudioController {
    
    private final AudioService audioService;

    public AudioController(AudioService audioService) {
        this.audioService = audioService;
    }

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadAudio(@RequestParam("file") MultipartFile file) {
        Job job = audioService.processUpload(file);
        return ResponseEntity.ok(new UploadResponse(job.getId(), "File uploaded successfully and processing started"));
    }
}
