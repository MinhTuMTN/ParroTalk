package com.parrotalk.backend.dto.practice;

import com.parrotalk.backend.constant.PracticeQuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeQuestionDto {
    private UUID userVocabularyId;
    private String word;
    private String displayWord;
    private String phonetic;
    private String audioUrl;
    private String partOfSpeech;
    private String definition;
    private PracticeQuestionType questionType;
    private List<String> options; // For MULTIPLE_CHOICE
    private String sentenceTemplate; // For SENTENCE_FILL
}
