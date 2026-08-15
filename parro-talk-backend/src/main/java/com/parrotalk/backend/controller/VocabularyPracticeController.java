package com.parrotalk.backend.controller;

import com.parrotalk.backend.dto.VocabularyPracticeQuestionDto;
import com.parrotalk.backend.service.VocabularyPracticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import java.util.Map;

/**
 * Controller for interactive vocabulary practice sessions.
 *
 * @author MinhTuMTN
 */
@RestController
@RequestMapping("/api/vocabulary/practice")
@RequiredArgsConstructor
public class VocabularyPracticeController {

    private final VocabularyPracticeService practiceService;

    /**
     * Generate dynamic practice questions for Quiz, Flashcards, and Cloze Test modes.
     *
     * @param topic Topic filter (optional)
     * @param cefrLevel CEFR level filter (optional)
     * @param count Question count (default 10)
     * @return List of practice questions
     */
    @GetMapping("/questions")
    public ResponseEntity<List<VocabularyPracticeQuestionDto>> getQuestions(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String cefrLevel,
            @RequestParam(defaultValue = "10") int count) {
        return ResponseEntity.ok(practiceService.generateQuestions(topic, cefrLevel, count));
    }

    @org.springframework.web.bind.annotation.PostMapping("/submit")
    public ResponseEntity<Void> submitPractice(
            @org.springframework.web.bind.annotation.RequestBody List<Map<String, Object>> answers) {
        practiceService.submitPractice(answers);
        return ResponseEntity.ok().build();
    }
}
