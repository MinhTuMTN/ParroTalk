package com.parrotalk.backend.controller.cms;

import com.parrotalk.backend.dto.ApiResponse;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.admin.AdminCategoryCreateRequest;
import com.parrotalk.backend.dto.admin.AdminCategoryDto;
import com.parrotalk.backend.dto.admin.AdminCategoryUpdateRequest;
import com.parrotalk.backend.service.cms.AdminCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/lesson-categories")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
public class AdminCategoryController {

    private final AdminCategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminCategoryDto>>> getCategories(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<AdminCategoryDto> response = categoryService.searchCategories(search, page, size);
        return ResponseEntity.ok(ApiResponse.<PageResponse<AdminCategoryDto>>builder()
                .result(response)
                .build());
    }

    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<AdminCategoryDto>>> getCategoryTree() {
        List<AdminCategoryDto> tree = categoryService.getCategoryTree();
        return ResponseEntity.ok(ApiResponse.<List<AdminCategoryDto>>builder()
                .result(tree)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminCategoryDto>> getCategory(@PathVariable UUID id) {
        AdminCategoryDto dto = categoryService.getCategory(id);
        return ResponseEntity.ok(ApiResponse.<AdminCategoryDto>builder()
                .result(dto)
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminCategoryDto>> createCategory(@Valid @RequestBody AdminCategoryCreateRequest request) {
        AdminCategoryDto dto = categoryService.createCategory(request);
        return ResponseEntity.ok(ApiResponse.<AdminCategoryDto>builder()
                .result(dto)
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminCategoryDto>> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody AdminCategoryUpdateRequest request
    ) {
        AdminCategoryDto dto = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.<AdminCategoryDto>builder()
                .result(dto)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .build());
    }
}
