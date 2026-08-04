package com.parrotalk.backend.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminVocabularyListDto {
    private UUID id;
    private String word;
    private String partOfSpeech;
    private String cefrLevel;
    private int definitionsCount;
    private int examplesCount;
    private boolean hasAudio;
    private String status;
    private LocalDateTime updatedAt;
}
