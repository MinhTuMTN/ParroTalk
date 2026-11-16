package com.parrotalk.backend.controller.admin;

import com.parrotalk.backend.config.ChatModelProvider;
import com.parrotalk.backend.config.ChatModelProviderRegistry;
import com.parrotalk.backend.dto.ApiResponse;
import com.parrotalk.backend.dto.ChatModelNode;
import com.parrotalk.backend.dto.admin.AdminAiKeyStatusDto.KeyStatusItem;
import com.parrotalk.backend.dto.admin.AdminAiKeyStatusDto.ProviderStatusDto;
import com.parrotalk.backend.dto.admin.AdminAiKeyStatusDto.TestKeyRequest;
import com.parrotalk.backend.dto.admin.AdminAiKeyStatusDto.ToggleKeyRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Admin controller for inspecting and live-testing AI provider API keys loaded from environment variables.
 */
@RestController
@RequestMapping("/api/admin/ai-keys")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminAiKeyController {

    private final ChatModelProviderRegistry providerRegistry;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProviderStatusDto>>> getAllProviderStatuses() {
        List<ProviderStatusDto> result = providerRegistry.getAllProviders().entrySet().stream()
                .map(entry -> buildProviderStatus(entry.getKey(), entry.getValue()))
                .toList();
        return ResponseEntity.ok(ApiResponse.<List<ProviderStatusDto>>builder()
                .result(result)
                .build());
    }

    @PostMapping("/{providerName}/test")
    public ResponseEntity<ApiResponse<ProviderStatusDto>> testProviderKeys(
            @PathVariable String providerName,
            @RequestBody(required = false) TestKeyRequest request) {
        ChatModelProvider provider = requireProvider(providerName);
        String targetKey = request != null && request.maskedApiKey() != null && !request.maskedApiKey().isBlank()
                ? request.maskedApiKey().trim()
                : null;

        Map<String, ChatModelNode> representativeByKey = new LinkedHashMap<>();
        for (ChatModelNode node : provider.getNodes()) {
            if (node.maskedApiKey() == null) {
                continue;
            }
            if (targetKey == null || targetKey.equals(node.maskedApiKey())) {
                representativeByKey.putIfAbsent(node.maskedApiKey(), node);
            }
        }

        if (representativeByKey.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy API key cần kiểm tra.");
        }

        for (Map.Entry<String, ChatModelNode> entry : representativeByKey.entrySet()) {
            String maskedKey = entry.getKey();
            ChatModelNode node = entry.getValue();
            long startNs = System.nanoTime();
            try {
                node.chatClient()
                        .prompt()
                        .user("Reply with the single word OK")
                        .call()
                        .content();
                long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
                provider.recordCheckSuccess(maskedKey, elapsedMs);
            } catch (Exception ex) {
                long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
                provider.recordCheckFailure(node, ex, elapsedMs);
                log.warn("Admin live test failed for provider '{}' key {}: {}", providerName, maskedKey, ex.getMessage());
            }
        }

        return ResponseEntity.ok(ApiResponse.<ProviderStatusDto>builder()
                .result(buildProviderStatus(providerName, provider))
                .build());
    }

    @PostMapping("/{providerName}/reset")
    public ResponseEntity<ApiResponse<ProviderStatusDto>> resetProviderKeys(
            @PathVariable String providerName,
            @RequestBody(required = false) TestKeyRequest request) {
        ChatModelProvider provider = requireProvider(providerName);
        if (request != null && request.maskedApiKey() != null && !request.maskedApiKey().isBlank()) {
            provider.resetKey(request.maskedApiKey().trim());
        } else {
            provider.resetAll();
        }
        return ResponseEntity.ok(ApiResponse.<ProviderStatusDto>builder()
                .result(buildProviderStatus(providerName, provider))
                .build());
    }

    @PostMapping("/{providerName}/toggle")
    public ResponseEntity<ApiResponse<ProviderStatusDto>> toggleProviderKey(
            @PathVariable String providerName,
            @RequestBody ToggleKeyRequest request) {
        if (request == null || request.maskedApiKey() == null || request.maskedApiKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maskedApiKey is required");
        }
        ChatModelProvider provider = requireProvider(providerName);
        provider.setDisabled(request.maskedApiKey().trim(), request.disabled());
        return ResponseEntity.ok(ApiResponse.<ProviderStatusDto>builder()
                .result(buildProviderStatus(providerName, provider))
                .build());
    }

    private ChatModelProvider requireProvider(String providerName) {
        return providerRegistry.getProvider(providerName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Provider not found: " + providerName));
    }

    private ProviderStatusDto buildProviderStatus(String providerName, ChatModelProvider provider) {
        List<ChatModelNode> nodes = provider.getNodes();
        int currentIdx = provider.getCurrentIndex();
        String activeMaskedKey = (!nodes.isEmpty() && currentIdx >= 0 && currentIdx < nodes.size())
                ? nodes.get(currentIdx).maskedApiKey()
                : null;

        Map<String, Set<String>> modelsByKey = new LinkedHashMap<>();
        for (ChatModelNode node : nodes) {
            if (node.maskedApiKey() == null) {
                continue;
            }
            modelsByKey
                    .computeIfAbsent(node.maskedApiKey(), ignored -> new LinkedHashSet<>())
                    .add(node.modelName());
        }

        Instant now = Instant.now();
        List<KeyStatusItem> keys = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : modelsByKey.entrySet()) {
            String maskedKey = entry.getKey();
            boolean disabled = provider.isDisabled(maskedKey);
            Instant quarantinedUntil = provider.getQuarantinedUntil(maskedKey);
            Long remainingSeconds = null;
            if (quarantinedUntil != null && now.isBefore(quarantinedUntil)) {
                remainingSeconds = Math.max(0L, Duration.between(now, quarantinedUntil).getSeconds());
            }

            String status;
            if (disabled) {
                status = "DISABLED";
            } else if (remainingSeconds != null) {
                status = remainingSeconds > 300L ? "DEAD" : "RATE_LIMITED";
            } else {
                status = "ACTIVE";
            }

            keys.add(new KeyStatusItem(
                    maskedKey,
                    List.copyOf(entry.getValue()),
                    status,
                    disabled,
                    maskedKey.equals(activeMaskedKey),
                    quarantinedUntil,
                    remainingSeconds,
                    provider.getLastError(maskedKey),
                    provider.getLastErrorAt(maskedKey),
                    provider.getLastCheckedAt(maskedKey),
                    provider.getLastLatencyMs(maskedKey)
            ));
        }

        boolean isDefault = providerName.equalsIgnoreCase(providerRegistry.getDefaultProviderName());
        return new ProviderStatusDto(
                providerName,
                isDefault,
                provider.size(),
                provider.healthyCount(),
                keys
        );
    }
}
