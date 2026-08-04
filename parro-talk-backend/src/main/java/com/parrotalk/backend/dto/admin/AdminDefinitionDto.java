package com.parrotalk.backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDefinitionDto {
    private UUID id;
    private String definition;
    private String englishDefinition;
    private String vietnameseDefinition;
    private Integer displayOrder;
}
