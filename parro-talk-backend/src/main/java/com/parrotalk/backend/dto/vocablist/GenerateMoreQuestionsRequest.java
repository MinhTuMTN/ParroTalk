package com.parrotalk.backend.dto.vocablist;

import java.util.UUID;

public record GenerateMoreQuestionsRequest(
        UUID senseId,
        UUID idiomId,
        UUID phrasalVerbId,
        Integer count
) {}

