package com.parrotalk.backend.controller.cms;

import com.parrotalk.backend.dto.admin.AdminVocabularyListDto;
import com.parrotalk.backend.dto.admin.AdminVocabularyRequestDto;
import com.parrotalk.backend.dto.admin.AdminVocabularyResponseDto;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.entity.cms.AdminVocabulary;
import com.parrotalk.backend.service.cms.AdminVocabularyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/vocabularies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")
public class AdminVocabularyController {

    private final AdminVocabularyService vocabularyService;

    @GetMapping
    public ResponseEntity<Page<AdminVocabularyListDto>> getVocabularies(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String partOfSpeech,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        
        Specification<AdminVocabulary> spec = Specification.where((Specification<AdminVocabulary>) null);
        if (search != null && !search.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("word")), "%" + search.toLowerCase() + "%"));
        }
        if (level != null && !level.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("cefrLevel"), level));
        }
        if (partOfSpeech != null && !partOfSpeech.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("partOfSpeech"), partOfSpeech));
        }
        if (status != null && !status.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        return ResponseEntity.ok(vocabularyService.getVocabularies(spec, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminVocabularyResponseDto> getVocabulary(@PathVariable UUID id) {
        return ResponseEntity.ok(vocabularyService.getVocabulary(id));
    }

    @PostMapping
    public ResponseEntity<AdminVocabularyResponseDto> createVocabulary(
            @Valid @RequestBody AdminVocabularyRequestDto request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vocabularyService.createVocabulary(request, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminVocabularyResponseDto> updateVocabulary(
            @PathVariable UUID id,
            @Valid @RequestBody AdminVocabularyRequestDto request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(vocabularyService.updateVocabulary(id, request, user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteVocabulary(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        vocabularyService.deleteVocabulary(id, user);
        return ResponseEntity.noContent().build();
    }
}
