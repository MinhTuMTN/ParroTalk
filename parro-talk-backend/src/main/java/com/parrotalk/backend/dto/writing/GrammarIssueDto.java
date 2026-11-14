package com.parrotalk.backend.dto.writing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GrammarIssueDto {
    private String ruleId;
    private String type;
    private String message;
    private String messageVn;
    private int offset;
    private int length;
    private List<String> replacements;
}

