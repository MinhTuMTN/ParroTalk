package com.parrotalk.backend.dto.dictation;

import java.util.List;
import java.util.Map;

import com.parrotalk.backend.constant.DictationMode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Serialized payload stored in {@code transcription_segments.blanks_json}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SegmentBlanksPayload {

    @Builder.Default
    private int v = 1;

    private int wordCount;

    private boolean suitable;

    private Map<DictationMode, List<BlankSlotDto>> levels;
}
