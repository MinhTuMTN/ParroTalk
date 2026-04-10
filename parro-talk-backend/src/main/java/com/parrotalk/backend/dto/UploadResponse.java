package com.parrotalk.backend.dto;

import java.util.UUID;

public class UploadResponse {
    private UUID jobId;
    private String message;

    public UploadResponse(UUID jobId, String message) {
        this.jobId = jobId;
        this.message = message;
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
