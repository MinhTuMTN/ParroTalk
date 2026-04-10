package com.parrotalk.backend.dto;

public class TranscriptionSegmentDTO {
    private String text;
    private double start;
    private double end;

    public TranscriptionSegmentDTO() {}

    public TranscriptionSegmentDTO(String text, double start, double end) {
        this.text = text;
        this.start = start;
        this.end = end;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public double getStart() {
        return start;
    }

    public void setStart(double start) {
        this.start = start;
    }

    public double getEnd() {
        return end;
    }

    public void setEnd(double end) {
        this.end = end;
    }
}
