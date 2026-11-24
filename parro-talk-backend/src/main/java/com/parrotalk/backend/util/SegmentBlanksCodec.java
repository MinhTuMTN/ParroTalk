package com.parrotalk.backend.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parrotalk.backend.dto.dictation.SegmentBlanksPayload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

/**
 * Serializes and deserializes {@link SegmentBlanksPayload} to/from {@code transcription_segments.blanks_json}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SegmentBlanksCodec {

    private final ObjectMapper objectMapper;

    public String serialize(SegmentBlanksPayload payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize SegmentBlanksPayload", e);
        }
    }

    public SegmentBlanksPayload deserialize(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, SegmentBlanksPayload.class);
        } catch (JsonProcessingException e) {
            log.warn("Invalid blanks_json encountered, ignoring stored payload: {}", e.getMessage());
            return null;
        }
    }
}
