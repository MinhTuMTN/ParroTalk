package com.parrotalk.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for public vocabulary summary.
 * 
 * @author MinhTuMTN
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicVocabularySummaryDto {
    private java.util.UUID id;
    private String word;
    private String commonMeaningVi;
    private String partOfSpeech;
    private String phonetic;
    private String cefrLevel;
    private String topic;
    private String audioUsUrl;
    private String audioUkUrl;
}
