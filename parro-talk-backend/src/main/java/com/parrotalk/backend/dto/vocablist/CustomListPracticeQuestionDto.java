package com.parrotalk.backend.dto.vocablist;

import java.util.List;
import java.util.UUID;

public record CustomListPracticeQuestionDto(
        UUID id,
        UUID vocabularyId,
        UUID senseId,
        UUID idiomId,
        UUID phrasalVerbId,
        String targetWord,
        String questionCategory,
        String grammarTopic,
        String generationSource,
        String difficulty,
        String questionText,
        List<String> options,
        int correctIndex,
        String correctAnswer,
        String completedSentence,
        String explanationEn,
        String explanationVn
) {}

