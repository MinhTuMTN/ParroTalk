package com.parrotalk.backend.dto.writing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrammarIssueDto {
    private String ruleId;
    private String message;
    private String messageVn;
    private int offset;
    private int length;
    private List<String> replacements;
}

