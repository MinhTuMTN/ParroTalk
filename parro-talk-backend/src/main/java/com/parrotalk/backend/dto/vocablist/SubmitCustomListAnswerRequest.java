package com.parrotalk.backend.dto.vocablist;

import java.util.UUID;

public record SubmitCustomListAnswerRequest(
        UUID listItemId,
        UUID questionId,
        boolean isCorrect
) {}

