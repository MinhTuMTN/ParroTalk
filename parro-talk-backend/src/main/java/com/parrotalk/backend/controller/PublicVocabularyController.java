package com.parrotalk.backend.controller;

import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.PublicVocabularyDetailDto;
import com.parrotalk.backend.dto.PublicVocabularySummaryDto;
import com.parrotalk.backend.dto.VocabularyReportRequestDto;
import com.parrotalk.backend.dto.VocabularyTopicSummaryDto;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.service.PublicVocabularyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Controller for public vocabulary lookup, topics, search, detail, and reporting.
 *
 * @author MinhTuMTN
 */
@RestController
@RequestMapping("/api/vocabulary")
@RequiredArgsConstructor
public class PublicVocabularyController {

    private final PublicVocabularyService publicVocabularyService;

    /**
     * Get list of 15 IELTS vocabulary topics.
     *
     * @return List of topic summaries with word counts.
     */
    @GetMapping("/topics")
    public ResponseEntity<List<VocabularyTopicSummaryDto>> getTopics() {
        return ResponseEntity.ok(publicVocabularyService.getTopics());
    }

    /**
     * Search vocabulary with keyword, topic, CEFR level, and POS filters.
     *
     * @return Paginated vocabulary summaries.
     */
    @GetMapping
    public ResponseEntity<PageResponse<PublicVocabularySummaryDto>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String cefrLevel,
            @RequestParam(required = false) String partOfSpeech,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(publicVocabularyService.searchVocabularies(keyword, topic, cefrLevel, partOfSpeech, page, size));
    }

    /**
     * Get featured vocabulary list.
     *
     * @return List of featured words.
     */
    @GetMapping("/featured")
    public ResponseEntity<List<PublicVocabularySummaryDto>> getFeatured() {
        return ResponseEntity.ok(publicVocabularyService.getFeatured());
    }

    /**
     * Get detailed vocabulary item by ID.
     *
     * @param id Vocabulary entry ID
     * @param user Authenticated user (optional)
     * @return Detailed vocabulary info.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PublicVocabularyDetailDto> getDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(publicVocabularyService.getDetail(id, user));
    }

    /**
     * Report content issue for a vocabulary item.
     *
     * @param id Vocabulary entry ID
     * @param user Authenticated reporter
     * @param request Report details
     * @return Response status
     */
    @PostMapping("/{id}/report")
    public ResponseEntity<Void> reportIssue(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody VocabularyReportRequestDto request) {
        publicVocabularyService.submitReport(id, user, request);
        return ResponseEntity.ok().build();
    }
}
