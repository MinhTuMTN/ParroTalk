package com.parrotalk.backend.controller.cms;

import com.parrotalk.backend.dto.ApiResponse;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.admin.AdminTagCreateRequest;
import com.parrotalk.backend.dto.admin.AdminTagDto;
import com.parrotalk.backend.dto.admin.AdminTagUpdateRequest;
import com.parrotalk.backend.service.cms.AdminTagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/lesson-tags")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
public class AdminTagController {

    private final AdminTagService tagService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminTagDto>>> getTags(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<AdminTagDto> response = tagService.searchTags(page, size);
        return ResponseEntity.ok(ApiResponse.<PageResponse<AdminTagDto>>builder()
                .result(response)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminTagDto>> getTag(@PathVariable UUID id) {
        AdminTagDto dto = tagService.getTag(id);
        return ResponseEntity.ok(ApiResponse.<AdminTagDto>builder()
                .result(dto)
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminTagDto>> createTag(@Valid @RequestBody AdminTagCreateRequest request) {
        AdminTagDto dto = tagService.createTag(request);
        return ResponseEntity.ok(ApiResponse.<AdminTagDto>builder()
                .result(dto)
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminTagDto>> updateTag(
            @PathVariable UUID id,
            @Valid @RequestBody AdminTagUpdateRequest request
    ) {
        AdminTagDto dto = tagService.updateTag(id, request);
        return ResponseEntity.ok(ApiResponse.<AdminTagDto>builder()
                .result(dto)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTag(@PathVariable UUID id) {
        tagService.deleteTag(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .build());
    }
}
