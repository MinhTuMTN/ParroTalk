package com.parrotalk.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.parrotalk.backend.dto.CategoryResponse;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.service.CategoryService;

import lombok.RequiredArgsConstructor;

/**
 * Public Category Controller.
 * 
 * @author MinhTuMTN
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    /** Category service. */
    private final CategoryService categoryService;

    /**
     * Search active categories by keyword.
     * 
     * @param query Search query
     * @param page  Page number
     * @param size  Page size
     * @return Page response of categories
     */
    @GetMapping
    public ResponseEntity<PageResponse<CategoryResponse>> searchCategories(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(categoryService.searchCategories(query, page, size));
    }
}
