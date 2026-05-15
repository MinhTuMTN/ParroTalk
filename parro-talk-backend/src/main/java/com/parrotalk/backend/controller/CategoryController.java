package com.parrotalk.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.parrotalk.backend.dto.ApiResponse;
import com.parrotalk.backend.dto.CategoryResponse;
import com.parrotalk.backend.dto.CreateCategoryRequest;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.service.CategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Category Controller.
 * 
 * @author MinhTuMTN
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    /** Category service */
    private final CategoryService categoryService;

    /**
     * Search categories by name.
     * 
     * @param query Search query
     * @param page  Page number
     * @param size  Page size
     * @return Page response of categories
     */
    @GetMapping
    public ResponseEntity<PageResponse<CategoryResponse>> searchCategories(
        @RequestParam("query") String query,
        @RequestParam("page") int page,
        @RequestParam("size") int size
    ) {
        return ResponseEntity.ok(categoryService.searchCategories(query, page, size));
    }

    /**
     * Create category.
     * 
     * @param request Create category request
     * @return Category
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
        @Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.ok(
            ApiResponse.<CategoryResponse>builder()
                .result(categoryService.createCategory(request))
                .message("Category created successfully")
                .build());
    }
}
