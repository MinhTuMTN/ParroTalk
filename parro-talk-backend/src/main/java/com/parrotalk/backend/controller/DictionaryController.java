package com.parrotalk.backend.controller;

import com.parrotalk.backend.dto.dictionary.ContextualDictionaryLookupRequest;
import com.parrotalk.backend.dto.dictionary.ContextualDictionaryLookupResponse;
import com.parrotalk.backend.dto.dictionary.DictionaryLookupResponse;
import com.parrotalk.backend.service.ContextualDictionaryService;
import com.parrotalk.backend.service.DictionaryLookupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dictionary")
@RequiredArgsConstructor
@Validated
public class DictionaryController {

    private final DictionaryLookupService dictionaryLookupService;
    private final ContextualDictionaryService contextualDictionaryService;

    @GetMapping("/lookup")
    public ResponseEntity<DictionaryLookupResponse> lookup(@RequestParam @NotBlank String word) {
        return ResponseEntity.ok(dictionaryLookupService.lookup(word));
    }

    @PostMapping("/contextual-lookup")
    public ResponseEntity<ContextualDictionaryLookupResponse> contextualLookup(
            @Valid @RequestBody ContextualDictionaryLookupRequest request) {
        return ResponseEntity.ok(contextualDictionaryService.lookup(request));
    }
}
