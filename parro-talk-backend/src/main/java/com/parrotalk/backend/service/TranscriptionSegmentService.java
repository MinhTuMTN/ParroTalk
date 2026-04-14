package com.parrotalk.backend.service;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.parrotalk.backend.repository.TranscriptionSegmentRepository;

/**
 * Transcription segment service.
 * 
 * @author MinhTuMTN
 */
@Service
@RequiredArgsConstructor
public class TranscriptionSegmentService {

    private final TranscriptionSegmentRepository transcriptionSegmentRepository;

    public void deleteByLessonId(UUID lessonId) {
        transcriptionSegmentRepository.deleteByLessonId(lessonId);
    }
}
