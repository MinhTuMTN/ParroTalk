package com.parrotalk.backend.dto.writing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.parrotalk.backend.constant.WritingAssessmentCriterion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WritingAssessmentCriterionDto {
    private WritingAssessmentCriterion criterion;
    private String nameEn;
    private String labelVn;
    private Integer score;
    private Double weight;
    private String feedbackVn;
}
