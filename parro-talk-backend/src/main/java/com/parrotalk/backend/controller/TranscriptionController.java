package com.parrotalk.backend.controller;

import com.parrotalk.backend.dto.TranscriptionResponse;
import com.parrotalk.backend.dto.TranscriptionSegmentDTO;
import com.parrotalk.backend.entity.SegmentType;
import com.parrotalk.backend.entity.TranscriptionSegment;
import com.parrotalk.backend.repository.TranscriptionSegmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/lessons/{lessonId}/result")
@RequiredArgsConstructor
public class TranscriptionController {

    private final TranscriptionSegmentRepository segmentRepository;

    @GetMapping
    public ResponseEntity<TranscriptionResponse> getResult(@PathVariable UUID lessonId) {
        List<TranscriptionSegment> sentencesEntity = segmentRepository.findByLessonIdAndTypeOrderByStartTimeAsc(lessonId, SegmentType.SENTENCE);
        List<TranscriptionSegment> wordsEntity = segmentRepository.findByLessonIdAndTypeOrderByStartTimeAsc(lessonId, SegmentType.WORD);

        List<TranscriptionSegmentDTO> sentences = sentencesEntity.stream()
                .map(s -> TranscriptionSegmentDTO.builder()
                    .text(s.getText())
                    .start(s.getStartTime())
                    .end(s.getEndTime())
                    .build())
                .collect(Collectors.toList());

        List<TranscriptionSegmentDTO> words = wordsEntity.stream()
                .map(w -> TranscriptionSegmentDTO.builder()
                    .text(w.getText())
                    .start(w.getStartTime())
                    .end(w.getEndTime())
                    .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(TranscriptionResponse.builder()
            .sentences(sentences)
            .words(words)
            .build());
    }
}
