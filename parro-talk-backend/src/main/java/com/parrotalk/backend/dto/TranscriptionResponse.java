package com.parrotalk.backend.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.parrotalk.backend.constant.DictationMode;
import com.parrotalk.backend.constant.Difficulty;
import com.parrotalk.backend.dto.dictation.BlankSlotDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranscriptionResponse {
    private UUID id;
    private String text;
    private double start;
    private double end;
    private Integer order;
    private Difficulty difficulty;
    private SegmentTranslationResponse translation;
    private Integer wordCount;
    private Boolean suitableForBlanks;
    private Map<DictationMode, List<BlankSlotDto>> blanks;
}
