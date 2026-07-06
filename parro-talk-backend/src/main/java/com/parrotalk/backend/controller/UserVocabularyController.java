package com.parrotalk.backend.controller;

import com.parrotalk.backend.constant.VocabularyStatus;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.dictionary.SaveVocabularyRequest;
import com.parrotalk.backend.dto.dictionary.UpdateVocabularyRequest;
import com.parrotalk.backend.dto.dictionary.UserVocabularyResponse;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.service.UserVocabularyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/me/vocabulary")
@RequiredArgsConstructor
public class UserVocabularyController {

    private final UserVocabularyService userVocabularyService;

    @PostMapping
    public ResponseEntity<UserVocabularyResponse> save(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SaveVocabularyRequest request) {
        return ResponseEntity.ok(userVocabularyService.save(user, request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<UserVocabularyResponse>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) VocabularyStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userVocabularyService.list(user, status, page, size));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserVocabularyResponse> update(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @RequestBody UpdateVocabularyRequest request) {
        return ResponseEntity.ok(userVocabularyService.update(user, id, request));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<UserVocabularyResponse> archive(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(userVocabularyService.archive(user, id));
    }
}
