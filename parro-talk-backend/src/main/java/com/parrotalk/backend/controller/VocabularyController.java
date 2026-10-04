package com.parrotalk.backend.controller;

import com.parrotalk.backend.dto.ApiResponse;
import com.parrotalk.backend.dto.vocabulary.VocabularyDetailDTO;
import com.parrotalk.backend.service.VocabularyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/library/vocabulary")
@RequiredArgsConstructor
public class VocabularyController {

    private final VocabularyService vocabularyService;

    @GetMapping("/{word}")
    public ResponseEntity<ApiResponse<VocabularyDetailDTO>> getVocabulary(@PathVariable String word) {
        VocabularyDetailDTO dto = vocabularyService.getVocabularyByWord(word);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.<VocabularyDetailDTO>builder().result(dto).build());
    }
}
