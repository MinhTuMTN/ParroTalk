package com.parrotalk.backend.controller;

import com.parrotalk.backend.dto.JobResponse;
import com.parrotalk.backend.service.JobService;
import com.parrotalk.backend.service.SseService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;
    private final SseService sseService;

    public JobController(JobService jobService, SseService sseService) {
        this.jobService = jobService;
        this.sseService = sseService;
    }

    @GetMapping
    public ResponseEntity<List<JobResponse>> listJobs() {
        return ResponseEntity.ok(jobService.getAllJobs());
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<JobResponse> getJobStatus(@PathVariable UUID jobId) {
        return ResponseEntity.ok(jobService.getJobResponse(jobId));
    }

    @GetMapping(path = "/{jobId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamJobStatus(@PathVariable UUID jobId) {
        // Return active SSE connection mapped to this job
        return sseService.connect(jobId);
    }
}
