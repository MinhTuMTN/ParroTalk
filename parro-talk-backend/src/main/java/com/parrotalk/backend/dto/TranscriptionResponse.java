package com.parrotalk.backend.dto;

import java.util.UUID;

import com.parrotalk.backend.constant.Difficulty;

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
}
