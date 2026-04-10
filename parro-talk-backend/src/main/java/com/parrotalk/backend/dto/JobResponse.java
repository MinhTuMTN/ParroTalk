package com.parrotalk.backend.dto;

import com.parrotalk.backend.entity.JobStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public class JobResponse {
    private UUID id;
    private JobStatus status;
    private int progress;
    private String currentStep;
    private String fileUrl;
    private LocalDateTime createdAt;

    public JobResponse() {
    }

    public JobResponse(UUID id, JobStatus status, int progress, String currentStep, String fileUrl,
            LocalDateTime createdAt) {
        this.id = id;
        this.status = status;
        this.progress = progress;
        this.currentStep = currentStep;
        this.fileUrl = fileUrl;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
