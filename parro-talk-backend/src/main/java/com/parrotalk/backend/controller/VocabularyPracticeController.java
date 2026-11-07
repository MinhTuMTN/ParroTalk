package com.parrotalk.backend.controller;

import com.parrotalk.backend.dto.ApiResponse;
import com.parrotalk.backend.dto.VocabularyPracticeQuestionDto;
import com.parrotalk.backend.dto.practice.FillBlankQuestionDto;
import com.parrotalk.backend.dto.practice.LearnWordDto;
import com.parrotalk.backend.dto.practice.MultipleChoiceQuestionDto;
import com.parrotalk.backend.dto.practice.TypeWordQuestionDto;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.service.VocabularyPracticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Controller for interactive vocabulary practice sessions across IELTS topic suites:
 * 1. Learn Words (interactive 3D flashcards)
 * 2. Multiple Choice
 * 3. Type the Word
 * 4. Fill in the Blank (Hybrid AI pre-generated + on-demand fallback)
 *
 * @author MinhTuMTN
 */
@RestController
@RequestMapping("/api/vocabulary/practice")
@RequiredArgsConstructor
public class VocabularyPracticeController {

    private final VocabularyPracticeService practiceService;

    private User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User u && u.getId() != null) {
            return u;
        }
        return null;
    }

    private List<String> parseExcludeWords(String exclude) {
        if (exclude == null || exclude.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(exclude.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Mode 4: Fill in the Blank (Hybrid AI data flow).
     *
     * @param topic Topic name (required)
     * @param limit Number of questions (default 10)
     * @return ApiResponse containing list of FillBlankQuestionDto
     */
    @GetMapping("/fill-blank")
    public ResponseEntity<ApiResponse<List<FillBlankQuestionDto>>> getFillBlankQuestions(
            @RequestParam String topic,
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false) Integer count,
            @RequestParam(required = false) String exclude) {
        int effectiveLimit = count != null ? count : (limit != null ? limit : 10);
        List<String> excludeWords = parseExcludeWords(exclude);
        User user = getCurrentUser();
        List<FillBlankQuestionDto> questions = (excludeWords.isEmpty() && user == null)
                ? practiceService.getFillBlankQuestions(topic, effectiveLimit)
                : practiceService.getFillBlankQuestions(topic, effectiveLimit, excludeWords, user);
        return ResponseEntity.ok(ApiResponse.<List<FillBlankQuestionDto>>builder()
                .code(1000)
                .result(questions)
                .build());
    }

    /**
     * Mode 1: Learn Words / Flashcards with reversible EN ↔ VI translations.
     *
     * @param topic Topic name (required)
     * @param limit Number of words (default 10)
     * @return ApiResponse containing list of LearnWordDto
     */
    @GetMapping("/learn-words")
    public ResponseEntity<ApiResponse<List<LearnWordDto>>> getLearnWords(
            @RequestParam String topic,
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false) Integer count,
            @RequestParam(required = false) String exclude) {
        int effectiveLimit = count != null ? count : (limit != null ? limit : 10);
        List<String> excludeWords = parseExcludeWords(exclude);
        User user = getCurrentUser();
        List<LearnWordDto> words = (excludeWords.isEmpty() && user == null)
                ? practiceService.getLearnWordCards(topic, effectiveLimit)
                : practiceService.getLearnWordCards(topic, effectiveLimit, excludeWords, user);
        return ResponseEntity.ok(ApiResponse.<List<LearnWordDto>>builder()
                .code(1000)
                .result(words)
                .build());
    }

    /**
     * Mode 2: Multiple Choice Quiz.
     *
     * @param topic Topic name (required)
     * @param limit Number of questions (default 10)
     * @return ApiResponse containing list of MultipleChoiceQuestionDto
     */
    @GetMapping("/multiple-choice")
    public ResponseEntity<ApiResponse<List<MultipleChoiceQuestionDto>>> getMultipleChoiceQuestions(
            @RequestParam String topic,
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false) Integer count,
            @RequestParam(required = false) String exclude) {
        int effectiveLimit = count != null ? count : (limit != null ? limit : 10);
        List<String> excludeWords = parseExcludeWords(exclude);
        User user = getCurrentUser();
        List<MultipleChoiceQuestionDto> questions = (excludeWords.isEmpty() && user == null)
                ? practiceService.getMultipleChoiceQuestions(topic, effectiveLimit)
                : practiceService.getMultipleChoiceQuestions(topic, effectiveLimit, excludeWords, user);
        return ResponseEntity.ok(ApiResponse.<List<MultipleChoiceQuestionDto>>builder()
                .code(1000)
                .result(questions)
                .build());
    }

    /**
     * Mode 3: Type the Word (active recall with definitions and clues).
     *
     * @param topic Topic name (required)
     * @param limit Number of questions (default 10)
     * @return ApiResponse containing list of TypeWordQuestionDto
     */
    @GetMapping("/type-word")
    public ResponseEntity<ApiResponse<List<TypeWordQuestionDto>>> getTypeWordQuestions(
            @RequestParam String topic,
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false) Integer count,
            @RequestParam(required = false) String exclude) {
        int effectiveLimit = count != null ? count : (limit != null ? limit : 10);
        List<String> excludeWords = parseExcludeWords(exclude);
        User user = getCurrentUser();
        List<TypeWordQuestionDto> questions = (excludeWords.isEmpty() && user == null)
                ? practiceService.getTypeWordQuestions(topic, effectiveLimit)
                : practiceService.getTypeWordQuestions(topic, effectiveLimit, excludeWords, user);
        return ResponseEntity.ok(ApiResponse.<List<TypeWordQuestionDto>>builder()
                .code(1000)
                .result(questions)
                .build());
    }

    /**
     * Backward-compatible practice questions endpoint with optional mode routing.
     *
     * @param topic Topic filter (optional)
     * @param cefrLevel CEFR level filter (optional)
     * @param count Question count (default 10)
     * @param mode Practice mode (optional)
     * @return List of practice questions
     */
    @GetMapping("/questions")
    public ResponseEntity<List<VocabularyPracticeQuestionDto>> getQuestions(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String cefrLevel,
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(required = false) String mode) {
        return ResponseEntity.ok(practiceService.generateQuestions(topic, cefrLevel, count, mode));
    }

    @PostMapping("/submit")
    public ResponseEntity<Void> submitPractice(@RequestBody List<Map<String, Object>> answers) {
        User user = getCurrentUser();
        if (user != null) {
            practiceService.submitPractice(answers, user);
        } else {
            practiceService.submitPractice(answers);
        }
        return ResponseEntity.ok().build();
    }
}
