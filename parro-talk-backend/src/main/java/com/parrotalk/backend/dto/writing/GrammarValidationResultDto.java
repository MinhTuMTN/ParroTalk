package com.parrotalk.backend.dto.writing;

import com.parrotalk.backend.constant.WritingItemValidationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrammarValidationResultDto {
    private boolean valid;
    private WritingItemValidationStatus status;
    private String summaryVn;
    @Builder.Default
    private List<GrammarIssueDto> issues = new ArrayList<>();
}

