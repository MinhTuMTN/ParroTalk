package com.parrotalk.backend.controller;

import com.parrotalk.backend.dto.TranscriptionResponse;
import com.parrotalk.backend.dto.TranscriptionSegmentDTO;
import com.parrotalk.backend.entity.SegmentType;
import com.parrotalk.backend.entity.TranscriptionSegment;
import com.parrotalk.backend.repository.TranscriptionSegmentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/jobs/{jobId}/result")
public class TranscriptionController {

    private final TranscriptionSegmentRepository segmentRepository;

    public TranscriptionController(TranscriptionSegmentRepository segmentRepository) {
        this.segmentRepository = segmentRepository;
    }

    @GetMapping
    public ResponseEntity<TranscriptionResponse> getResult(@PathVariable UUID jobId) {
        List<TranscriptionSegment> sentencesEntity = segmentRepository.findByJobIdAndTypeOrderByStartTimeAsc(jobId, SegmentType.SENTENCE);
        List<TranscriptionSegment> wordsEntity = segmentRepository.findByJobIdAndTypeOrderByStartTimeAsc(jobId, SegmentType.WORD);

        List<TranscriptionSegmentDTO> sentences = sentencesEntity.stream()
                .map(s -> new TranscriptionSegmentDTO(s.getText(), s.getStartTime(), s.getEndTime()))
                .collect(Collectors.toList());

        List<TranscriptionSegmentDTO> words = wordsEntity.stream()
                .map(w -> new TranscriptionSegmentDTO(w.getText(), w.getStartTime(), w.getEndTime()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new TranscriptionResponse(sentences, words));
    }
}
