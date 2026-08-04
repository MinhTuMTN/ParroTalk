package com.parrotalk.backend.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminVocabularyRequestDto {
    @NotBlank(message = "Word cannot be blank")
    private String word;

    private String ipaUk;
    private String ipaUs;
    private String audioUk;
    private String audioUs;
    private String cefrLevel;
    private Integer frequencyRank;
    private String partOfSpeech;
    private String imageUrl;
    private String notes;
    private String source;
    private String status;

    private List<AdminDefinitionDto> definitions;
    private List<AdminExampleDto> examples;
    private List<AdminRelationDto> relations;
    private Set<UUID> categoryIds;
    private Set<String> tags; // List of tag names to create or associate
}
