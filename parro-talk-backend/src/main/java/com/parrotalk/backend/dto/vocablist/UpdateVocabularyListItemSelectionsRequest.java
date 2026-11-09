package com.parrotalk.backend.dto.vocablist;

import java.util.List;
import java.util.UUID;

public record UpdateVocabularyListItemSelectionsRequest(
        List<UUID> selectedSenseIds,
        List<UUID> selectedIdiomIds,
        List<UUID> selectedPhrasalVerbIds,
        String userNote
) {}

