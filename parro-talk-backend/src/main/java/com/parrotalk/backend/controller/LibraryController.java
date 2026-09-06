package com.parrotalk.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.parrotalk.backend.dto.ApiResponse;
import com.parrotalk.backend.dto.LessonWithProgressDTO;
import com.parrotalk.backend.dto.LibrarySectionDTO;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.service.LibraryService;

import lombok.RequiredArgsConstructor;

/**
 * Library controller.
 * 
 * @author MinhTuMTN
 */
@RestController
@RequestMapping("/api/library")
@RequiredArgsConstructor
public class LibraryController {

    /** Library Service */
    private final LibraryService libraryService;

    /**
     * Get library sections.
     * 
     * @param user User
     * @return List of library sections
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<LibrarySectionDTO>>> getLibrarySections(
            @AuthenticationPrincipal User user) {
        UUID userId = user != null ? user.getId() : null;
        List<LibrarySectionDTO> sections = libraryService.getLibrarySections(userId);
        return ResponseEntity.ok(ApiResponse.<List<LibrarySectionDTO>>builder()
                .result(sections)
                .build());
    }

    /**
     * Get lessons by category.
     * 
     * @param categorySlug Category slug
     * @param program Program
     * @param source Source
     * @param sort Sort
     * @param page Page
     * @param size Size
     * @param user User
     * @return List of lessons
     */
    @GetMapping("/categories/{categorySlug}/lessons")
    public ResponseEntity<ApiResponse<PageResponse<LessonWithProgressDTO>>> getCategoryLessons(
            @PathVariable String categorySlug,
            @RequestParam(required = false) String program,
            @RequestParam(required = false) String source,
            @RequestParam(required = false, defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @AuthenticationPrincipal User user) {
        PageResponse<LessonWithProgressDTO> response = libraryService.getCategoryLessons(
                categorySlug, program, source, sort, page, size, user);
        return ResponseEntity.ok(ApiResponse.<PageResponse<LessonWithProgressDTO>>builder()
                .result(response)
                .build());
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<LessonWithProgressDTO>>> searchPublicLessons(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String program,
            @RequestParam(required = false) String source,
            @RequestParam(required = false, defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @AuthenticationPrincipal User user) {
        PageResponse<LessonWithProgressDTO> response = libraryService.searchPublicLessons(
                query, category, program, source, sort, page, size, user);
        return ResponseEntity.ok(ApiResponse.<PageResponse<LessonWithProgressDTO>>builder()
                .result(response)
                .build());
    }
}
