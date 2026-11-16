package com.parrotalk.backend.controller;

import com.parrotalk.backend.dto.dictionary.SaveVocabularyRequest;
import com.parrotalk.backend.dto.dictionary.UserVocabularyResponse;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.service.UserVocabularyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
