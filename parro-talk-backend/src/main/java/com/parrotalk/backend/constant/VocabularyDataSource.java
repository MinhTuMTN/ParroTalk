package com.parrotalk.backend.constant;

/**
 * Tracks the origin/source of a vocabulary entry, sense, idiom, or phrasal verb.
 */
public enum VocabularyDataSource {
    IMPORT_JSON,
    OXFORD_CRAWLER,
    MERRIAM_WEBSTER_API,
    OXFORD_MW_HYBRID,
    AI_GENERATED,
    ADMIN_MANUAL
}

