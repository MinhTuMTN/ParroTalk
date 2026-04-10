package com.parrotalk.backend.dto;

import java.util.List;

public class TranscriptionResponse {
    private List<TranscriptionSegmentDTO> sentences;
    private List<TranscriptionSegmentDTO> words;

    public TranscriptionResponse() {}

    public TranscriptionResponse(List<TranscriptionSegmentDTO> sentences, List<TranscriptionSegmentDTO> words) {
        this.sentences = sentences;
        this.words = words;
    }

    public List<TranscriptionSegmentDTO> getSentences() {
        return sentences;
    }

    public void setSentences(List<TranscriptionSegmentDTO> sentences) {
        this.sentences = sentences;
    }

    public List<TranscriptionSegmentDTO> getWords() {
        return words;
    }

    public void setWords(List<TranscriptionSegmentDTO> words) {
        this.words = words;
    }
}
