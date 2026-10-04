package com.parrotalk.backend.controller.admin;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.parrotalk.backend.dto.ApiResponse;
import com.parrotalk.backend.dto.admin.vocab.AdminVocabularyDetailDto;
import com.parrotalk.backend.dto.admin.vocab.AdminVocabularyListDto;
import com.parrotalk.backend.dto.admin.vocab.AdminVocabularyRequestDto;
import com.parrotalk.backend.service.admin.AdminCoreVocabularyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/vocabularies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")
public class AdminVocabularyController {

    private final AdminCoreVocabularyService adminVocabularyService;
    private final com.parrotalk.backend.service.admin.AdminVocabularyAiService adminVocabularyAiService;

    @GetMapping
    public ApiResponse<Page<AdminVocabularyListDto>> getVocabularies(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String partOfSpeech,
            @RequestParam(required = false) UUID topicId,
            Pageable pageable) {
        
        return ApiResponse.<Page<AdminVocabularyListDto>>builder()
                .result(adminVocabularyService.getVocabularies(search, level, partOfSpeech, topicId, pageable))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminVocabularyDetailDto> getVocabulary(@PathVariable UUID id) {
        return ApiResponse.<AdminVocabularyDetailDto>builder()
                .result(adminVocabularyService.getVocabulary(id))
                .build();
    }

    @PostMapping
    public ApiResponse<AdminVocabularyDetailDto> createVocabulary(
            @Valid @RequestBody AdminVocabularyRequestDto request) {
        return ApiResponse.<AdminVocabularyDetailDto>builder()
                .result(adminVocabularyService.createVocabulary(request))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminVocabularyDetailDto> updateVocabulary(
            @PathVariable UUID id,
            @Valid @RequestBody AdminVocabularyRequestDto request) {
        return ApiResponse.<AdminVocabularyDetailDto>builder()
                .result(adminVocabularyService.updateVocabulary(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteVocabulary(@PathVariable UUID id) {
        adminVocabularyService.deleteVocabulary(id);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/ai/refine-senses")
    public ApiResponse<java.util.List<com.parrotalk.backend.dto.admin.vocab.LlmSenseRefineResponseItem>> refineSenses(
            @Valid @RequestBody com.parrotalk.backend.dto.admin.vocab.AdminVocabularyAiRefineRequestDto request) {
        return ApiResponse.<java.util.List<com.parrotalk.backend.dto.admin.vocab.LlmSenseRefineResponseItem>>builder()
                .result(adminVocabularyAiService.refineSenses(request))
                .build();
    }

    @PostMapping("/ai/common-meaning")
    public ApiResponse<com.parrotalk.backend.dto.admin.vocab.AdminVocabularyAiCommonMeaningResponseDto> generateCommonMeaning(
            @Valid @RequestBody com.parrotalk.backend.dto.admin.vocab.AdminVocabularyAiCommonMeaningRequestDto request) {
        return ApiResponse.<com.parrotalk.backend.dto.admin.vocab.AdminVocabularyAiCommonMeaningResponseDto>builder()
                .result(adminVocabularyAiService.generateCommonMeaningVn(request))
                .build();
    }
}
