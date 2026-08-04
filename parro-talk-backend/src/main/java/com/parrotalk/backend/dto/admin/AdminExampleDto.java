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
public class AdminExampleDto {
    private UUID id;
    private String sentence;
    private String translation;
    private Integer displayOrder;
}
