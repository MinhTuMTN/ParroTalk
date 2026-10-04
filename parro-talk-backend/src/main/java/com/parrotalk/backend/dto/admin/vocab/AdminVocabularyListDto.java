package com.parrotalk.backend.dto.admin.vocab;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminVocabularyListDto {
    private UUID id;
    private String word;
    private String commonMeaningVn;
    private int senseCount;
    private List<String> topics;
    private List<String> cefrLevels;
    private List<String> partsOfSpeech;
}
