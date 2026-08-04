package com.parrotalk.backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminVocabularyResponseDto {
    private UUID id;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<AdminDefinitionDto> definitions;
    private List<AdminExampleDto> examples;
    private List<AdminRelationDto> relations;
    private Set<String> categories;
    private Set<String> tags;
}
