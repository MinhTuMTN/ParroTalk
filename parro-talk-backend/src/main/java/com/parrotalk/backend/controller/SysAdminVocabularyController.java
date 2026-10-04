package com.parrotalk.backend.controller;

import com.parrotalk.backend.dto.ApiResponse;
import com.parrotalk.backend.service.VocabularyImporterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/vocabulary-sys")
@RequiredArgsConstructor
public class SysAdminVocabularyController {

    private final VocabularyImporterService importerService;

    @PostMapping("/import")
    public ResponseEntity<ApiResponse<String>> importVocabulary(@RequestParam String filePath) {
        // Run async or sync depending on requirement, but for this task we just run it directly.
        new Thread(() -> importerService.importFromJson(filePath)).start();
        return ResponseEntity.ok(ApiResponse.<String>builder().result("Import started in background for file: " + filePath).build());
    }
}
