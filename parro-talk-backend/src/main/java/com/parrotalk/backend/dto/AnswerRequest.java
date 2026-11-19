package com.parrotalk.backend.dto;

import java.util.List;
import java.util.UUID;

import com.parrotalk.backend.constant.DictationMode;
import com.parrotalk.backend.dto.dictation.BlankAnswerItem;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerRequest {
    private UUID segmentId;
    private String userAnswer;
    private Integer studySecondsDelta;
    private DictationMode mode;
    private List<BlankAnswerItem> blankAnswers;
}
