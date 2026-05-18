package com.parrotalk.backend.controller;

import com.parrotalk.backend.constant.CategoryStatus;
import com.parrotalk.backend.dto.ApiResponse;
import com.parrotalk.backend.dto.CategoryAiSuggestDescriptionRequest;
import com.parrotalk.backend.dto.CategoryAiSuggestDescriptionResponse;
import com.parrotalk.backend.dto.CategoryResponse;
import com.parrotalk.backend.dto.CreateCategoryRequest;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.ReorderCategoriesRequest;
import com.parrotalk.backend.dto.UpdateCategoryRequest;
import com.parrotalk.backend.dto.UpdateCategoryStatusRequest;
import com.parrotalk.backend.service.CategoryService;
import com.parrotalk.backend.service.ai.CategoryAiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Category Controller for Admin.
 */
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    /** Category service. */
    private final CategoryService categoryService;

    /** Category AI service. */
    private final CategoryAiService categoryAiService;

    /**
     * Get admin category list.
     *
     * @param keyword Search keyword
     * @param status Category status filter
     * @param page Page number
     * @param size Page size
     * @param sortBy Sort field: displayOrder, createdAt, or name
     * @param direction Sort direction
     * @return Page of categories
     */
    @GetMapping
    public ResponseEntity<PageResponse<CategoryResponse>> getCategories(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CategoryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "displayOrder") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction) {
        return ResponseEntity.ok(categoryService.getAdminCategories(keyword, status, page, size, sortBy, direction));
    }

    /**
     * Get category detail.
     *
     * @param id Category ID
     * @return Category detail
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategory(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryService.getCategory(id));
    }

    /**
     * Create category.
     *
     * @param request Create request
     * @return Created category
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.<CategoryResponse>builder()
                .result(categoryService.createCategory(request))
                .message("Category created successfully")
                .build());
    }

    /**
     * Update category.
     *
     * @param id Category ID
     * @param request Update request
     * @return Updated category
     */
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    /**
     * Delete category by soft-deleting unused categories or deactivating used categories.
     *
     * @param id Category ID
     * @return Empty response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Update category status.
     *
     * @param id Category ID
     * @param request Status request
     * @return Updated category
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<CategoryResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryStatusRequest request) {
        return ResponseEntity.ok(categoryService.updateStatus(id, request.getStatus()));
    }

    /**
     * Reorder multiple categories.
     *
     * @param request Reorder request
     * @return Updated categories
     */
    @PatchMapping("/reorder")
    public ResponseEntity<List<CategoryResponse>> reorderCategories(
            @Valid @RequestBody ReorderCategoriesRequest request) {
        return ResponseEntity.ok(categoryService.reorderCategories(request.getCategories()));
    }

    /**
     * Suggest category description with AI abstraction.
     *
     * @param request AI suggestion request
     * @return Suggested description
     */
    @PostMapping("/ai/suggest-description")
    public ResponseEntity<CategoryAiSuggestDescriptionResponse> suggestDescription(
            @Valid @RequestBody CategoryAiSuggestDescriptionRequest request) {
        return ResponseEntity.ok(categoryAiService.suggestDescription(request));
    }
}
