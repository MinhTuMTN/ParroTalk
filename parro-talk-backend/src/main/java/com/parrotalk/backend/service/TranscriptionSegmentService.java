package com.parrotalk.backend.service;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.parrotalk.backend.entity.TranscriptionSegment;
import com.parrotalk.backend.repository.TranscriptionSegmentRepository;

import jakarta.transaction.Transactional;

/**
 * Transcription segment service.
 * 
 * @author MinhTuMTN
 */
@Service
@RequiredArgsConstructor
public class TranscriptionSegmentService {

    /** Transcription Segment Repository */
    private final TranscriptionSegmentRepository transcriptionSegmentRepository;

    /**
     * Delete all transcription segment by lesson ID
     * TODO: Check if segment is using in some DraftSegments.
     * 
     * @param lessonId Lesson ID
     */
    @Transactional
    public void deleteByLessonId(UUID lessonId) {
        transcriptionSegmentRepository.deleteByLessonId(lessonId);
    }

    /**
     * Save all transcription segment
     * 
     * @param segments List of transcription segments
     * @return Number of segments saved
     */
    @Transactional
    public int saveAllTranscriptionSegment(List<TranscriptionSegment> segments) {
        transcriptionSegmentRepository.saveAll(segments);
        return segments.size();
    }
}
