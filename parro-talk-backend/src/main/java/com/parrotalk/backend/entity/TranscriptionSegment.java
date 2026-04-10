package com.parrotalk.backend.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "transcription_segments")
public class TranscriptionSegment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID jobId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    private double startTime;
    private double endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SegmentType type;

    public TranscriptionSegment() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public double getStartTime() {
        return startTime;
    }

    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    public double getEndTime() {
        return endTime;
    }

    public void setEndTime(double endTime) {
        this.endTime = endTime;
    }

    public SegmentType getType() {
        return type;
    }

    public void setType(SegmentType type) {
        this.type = type;
    }
}
