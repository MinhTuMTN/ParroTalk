package com.parrotalk.backend.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminCategoryReorderRequest {

    private UUID parentCategoryId;

    @NotEmpty(message = "orderedIds cannot be empty")
    private List<@NotNull UUID> orderedIds;
}
