package com.parrotalk.backend.controller.admin;

import com.parrotalk.backend.dto.ApiResponse;
import com.parrotalk.backend.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/media")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('EDITOR')")
public class AdminMediaController {

    private final StorageService storageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<String> uploadMedia(@RequestParam("file") MultipartFile file) {
        String filename = "admin_upload_" + UUID.randomUUID().toString();
        String url = storageService.store(file, filename);
        return ApiResponse.<String>builder()
                .result(url)
                .build();
    }
}
