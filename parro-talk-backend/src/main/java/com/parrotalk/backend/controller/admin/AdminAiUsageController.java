package com.parrotalk.backend.controller.admin;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.parrotalk.backend.dto.ApiResponse;
import com.parrotalk.backend.dto.admin.AiUsageByFeatureDto;
import com.parrotalk.backend.dto.admin.AiUsageByModelDto;
import com.parrotalk.backend.dto.admin.AiUsageDailyPointDto;
import com.parrotalk.backend.dto.admin.AiUsageSummaryDto;
import com.parrotalk.backend.dto.admin.AiUsageUserPageDto;
import com.parrotalk.backend.service.admin.AdminAiUsageService;
import com.parrotalk.backend.service.admin.AdminAiUsageService.Filter;

import lombok.RequiredArgsConstructor;

/** Admin endpoints exposing AI token usage aggregates. See docs/ai-usage-api.md. */
@RestController
@RequestMapping("/api/admin/ai-usage")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAiUsageController {

    private final AdminAiUsageService service;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AiUsageSummaryDto>> summary(
            @RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false) String tz, @RequestParam(required = false) String userId,
            @RequestParam(required = false) String model, @RequestParam(required = false) String provider,
            @RequestParam(required = false) String feature) {
        Filter f = service.buildFilter(from, to, tz, userId, model, provider, feature);
        return ok(service.summary(f));
    }

    @GetMapping("/timeseries")
    public ResponseEntity<ApiResponse<List<AiUsageDailyPointDto>>> timeseries(
            @RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false) String tz, @RequestParam(required = false) String userId,
            @RequestParam(required = false) String model, @RequestParam(required = false) String provider,
            @RequestParam(required = false) String feature) {
        Filter f = service.buildFilter(from, to, tz, userId, model, provider, feature);
        return ok(service.timeseries(f));
    }

    @GetMapping("/models")
    public ResponseEntity<ApiResponse<List<AiUsageByModelDto>>> models(
            @RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false) String tz, @RequestParam(required = false) String userId,
            @RequestParam(required = false) String model, @RequestParam(required = false) String provider,
            @RequestParam(required = false) String feature) {
        Filter f = service.buildFilter(from, to, tz, userId, model, provider, feature);
        return ok(service.byModel(f));
    }

    @GetMapping("/features")
    public ResponseEntity<ApiResponse<List<AiUsageByFeatureDto>>> features(
            @RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false) String tz, @RequestParam(required = false) String userId,
            @RequestParam(required = false) String model, @RequestParam(required = false) String provider,
            @RequestParam(required = false) String feature) {
        Filter f = service.buildFilter(from, to, tz, userId, model, provider, feature);
        return ok(service.byFeature(f));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<AiUsageUserPageDto>> users(
            @RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false) String tz, @RequestParam(required = false) String userId,
            @RequestParam(required = false) String model, @RequestParam(required = false) String provider,
            @RequestParam(required = false) String feature,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search, @RequestParam(required = false) String sort) {
        Filter f = service.buildFilter(from, to, tz, userId, model, provider, feature);
        return ok(service.byUser(f, page, size, search, sort));
    }

    private static <T> ResponseEntity<ApiResponse<T>> ok(T result) {
        return ResponseEntity.ok(ApiResponse.<T>builder().result(result).build());
    }
}
