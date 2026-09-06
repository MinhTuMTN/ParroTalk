package com.parrotalk.backend.controller.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.parrotalk.backend.dto.ApiResponse;
import com.parrotalk.backend.dto.admin.AdminLibrarySectionRequest;
import com.parrotalk.backend.dto.admin.AdminLibrarySectionResponse;
import com.parrotalk.backend.dto.admin.ReorderLibrarySectionsRequest;
import com.parrotalk.backend.service.LibrarySectionAdminService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Admin Library Section Controller.
 * Restricts section management to users with ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/admin/library/sections")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Validated
public class AdminLibrarySectionController {

    private final LibrarySectionAdminService librarySectionAdminService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminLibrarySectionResponse>>> getAllSections() {
        List<AdminLibrarySectionResponse> sections = librarySectionAdminService.getAllSections();
        return ResponseEntity.ok(ApiResponse.<List<AdminLibrarySectionResponse>>builder()
                .result(sections)
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminLibrarySectionResponse>> createSection(
            @Valid @RequestBody AdminLibrarySectionRequest request) {
        AdminLibrarySectionResponse response = librarySectionAdminService.createSection(request);
        return ResponseEntity.ok(ApiResponse.<AdminLibrarySectionResponse>builder()
                .result(response)
                .message("Library section created successfully")
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminLibrarySectionResponse>> updateSection(
            @PathVariable UUID id,
            @Valid @RequestBody AdminLibrarySectionRequest request) {
        AdminLibrarySectionResponse response = librarySectionAdminService.updateSection(id, request);
        return ResponseEntity.ok(ApiResponse.<AdminLibrarySectionResponse>builder()
                .result(response)
                .message("Library section updated successfully")
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSection(@PathVariable UUID id) {
        librarySectionAdminService.deleteSection(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Library section deleted successfully")
                .build());
    }

    @PutMapping("/reorder")
    public ResponseEntity<ApiResponse<Void>> reorderSections(
            @Valid @RequestBody ReorderLibrarySectionsRequest request) {
        librarySectionAdminService.reorderSections(request);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Library sections reordered successfully")
                .build());
    }
}
