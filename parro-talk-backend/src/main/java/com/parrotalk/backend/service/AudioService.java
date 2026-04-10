package com.parrotalk.backend.service;

import com.parrotalk.backend.entity.Job;
import com.parrotalk.backend.entity.JobStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Service
public class AudioService {

    private final StorageService storageService;
    private final JobService jobService;
    private final TranscriptionProcessingService processingService;

    public AudioService(StorageService storageService, JobService jobService,
            TranscriptionProcessingService processingService) {
        this.storageService = storageService;
        this.jobService = jobService;
        this.processingService = processingService;
    }

    public Job processUpload(MultipartFile file) {
        try {
            String fileHash = DigestUtils.md5DigestAsHex(file.getInputStream());
            Optional<Job> existingJob = jobService.findByFileHash(fileHash);

            if (existingJob.isPresent()) {
                Job job = existingJob.get();
                if (job.getStatus() != JobStatus.FAILED) {
                    return job; // Short-circuit, don't re-upload
                }
            }

            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            String fileUrl = storageService.store(file, filename);

            Job job = jobService.createJob(fileUrl, fileHash);

            processingService.startTranscription(job.getId(), file);

            return job;
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate checksum for file", e);
        }
    }
}
