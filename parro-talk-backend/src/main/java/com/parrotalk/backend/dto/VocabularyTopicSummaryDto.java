package com.parrotalk.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for vocabulary topic summary.
 * 
 * @author MinhTuMTN
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyTopicSummaryDto {
    private java.util.UUID id;
    private String name;
    private Long wordCount;
    private Long learnedWordCount;
    private String description;
    private String icon;
}
