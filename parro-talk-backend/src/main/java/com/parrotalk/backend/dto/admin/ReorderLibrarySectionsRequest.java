package com.parrotalk.backend.dto.admin;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
public class ReorderLibrarySectionsRequest {

    @NotEmpty(message = "Items cannot be empty")
    @Valid
    private List<ReorderSectionItem> items;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public ReorderLibrarySectionsRequest(List<ReorderSectionItem> items) {
        this.items = items;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReorderSectionItem {

        @NotNull(message = "Section ID is required")
        private UUID id;

        @NotNull(message = "Order index is required")
        private Integer orderIndex;
    }
}
