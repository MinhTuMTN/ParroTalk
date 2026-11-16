package com.parrotalk.backend.dto.admin;

import java.time.Instant;
import java.util.List;

/**
 * DTOs for Admin AI Key Monitor & Runtime Control endpoints.
 */
public final class AdminAiKeyStatusDto {

    private AdminAiKeyStatusDto() {
    }

    public record KeyStatusItem(
            String maskedApiKey,
            List<String> models,
            String status,
            boolean disabled,
            boolean currentKey,
            Instant quarantinedUntil,
            Long quarantineRemainingSeconds,
            String lastError,
            Instant lastErrorAt,
            Instant lastCheckedAt,
            Long lastLatencyMs
    ) {}

    public record ProviderStatusDto(
            String providerName,
            boolean defaultProvider,
            int totalNodes,
            int healthyNodes,
            List<KeyStatusItem> keys
    ) {}

    public record ToggleKeyRequest(
            String maskedApiKey,
            boolean disabled
    ) {}

    public record TestKeyRequest(
            String maskedApiKey
    ) {}
}
