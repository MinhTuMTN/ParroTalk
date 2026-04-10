package com.parrotalk.backend.service;

import com.parrotalk.backend.dto.JobResponse;
import com.parrotalk.backend.entity.Job;
import com.parrotalk.backend.entity.JobStatus;
import com.parrotalk.backend.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JobService {
    private final JobRepository jobRepository;
    private final SseService sseService;

    public JobService(JobRepository jobRepository, SseService sseService) {
        this.jobRepository = jobRepository;
        this.sseService = sseService;
    }

    @Transactional
    public Job createJob(String filePath, String fileHash) {
        Job job = new Job();
        job.setFilePath(filePath);
        job.setFileHash(fileHash);
        return jobRepository.save(job);
    }

    public Optional<Job> findByFileHash(String fileHash) {
        return jobRepository.findByFileHash(fileHash);
    }

    public JobResponse getJobResponse(UUID jobId) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Job not found"));
        return new JobResponse(job.getId(), job.getStatus(), job.getProgress(), job.getCurrentStep(), job.getFilePath(), job.getCreatedAt());
    }

    public List<JobResponse> getAllJobs() {
        return jobRepository.findAll().stream()
            .map(job -> new JobResponse(job.getId(), job.getStatus(), job.getProgress(), job.getCurrentStep(), job.getFilePath(), job.getCreatedAt()))
            .sorted((j1, j2) -> j2.getCreatedAt().compareTo(j1.getCreatedAt()))
            .collect(Collectors.toList());
    }

    @Transactional
    public void updateProgress(UUID jobId, int progress, String step, JobStatus status) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found"));
        boolean updated = false;

        if (status != null && job.getStatus() != status) {
            job.setStatus(status);
            updated = true;
        }
        if (job.getProgress() != progress) {
            job.setProgress(progress);
            updated = true;
        }
        if (step != null && !step.equals(job.getCurrentStep())) {
            job.setCurrentStep(step);
            updated = true;
        }

        if (updated) {
            jobRepository.save(job);
            sseService.sendEvent(jobId, Map.of(
                "status", job.getStatus(),
                "progress", job.getProgress(),
                "step", job.getCurrentStep()
            ));
        }
    }
}
