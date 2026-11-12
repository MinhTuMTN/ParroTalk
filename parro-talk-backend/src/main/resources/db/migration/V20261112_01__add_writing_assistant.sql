-- Migration: Add writing_sessions and writing_generated_items tables for Writing Assistant MVP
CREATE TABLE IF NOT EXISTS writing_sessions (
    id                      UUID PRIMARY KEY,
    user_id                 UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    input_text              TEXT NOT NULL,
    input_language          VARCHAR(16) NOT NULL DEFAULT 'VI',
    mode                    VARCHAR(20) NOT NULL CHECK (mode IN ('SUGGESTION', 'GUIDANCE')),
    scale_type              VARCHAR(16) NOT NULL CHECK (scale_type IN ('CEFR', 'IELTS')),
    target_level            VARCHAR(16) NOT NULL,
    core_idea_summary_vn    TEXT,
    combination_guide_vn    TEXT,
    user_draft              TEXT,
    response_json           TEXT NOT NULL,
    grammar_check_json      TEXT,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP,
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_writing_sessions_user_created
    ON writing_sessions(user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS writing_generated_items (
    id                      UUID PRIMARY KEY,
    session_id              UUID NOT NULL REFERENCES writing_sessions(id) ON DELETE CASCADE,
    item_type               VARCHAR(32) NOT NULL CHECK (item_type IN ('VOCABULARY', 'EXPRESSION', 'COLLOCATION', 'SENTENCE_PATTERN', 'COMPLETE_SENTENCE')),
    content_en              TEXT NOT NULL,
    normalized_text         VARCHAR(500),
    part_of_speech          VARCHAR(64),
    cefr_level              VARCHAR(16),
    meaning_vn              TEXT,
    explanation_vn          TEXT,
    usage_context_vn        TEXT,
    example_en              TEXT,
    example_vn              TEXT,
    linked_vocabulary_id    UUID REFERENCES vocabularies(id) ON DELETE SET NULL,
    linked_sense_id         UUID REFERENCES vocabulary_senses(id) ON DELETE SET NULL,
    validation_status       VARCHAR(32) NOT NULL DEFAULT 'PENDING' CHECK (validation_status IN ('PENDING', 'VALIDATING', 'APPROVED', 'REJECTED', 'NEEDS_REVIEW')),
    validation_notes        TEXT,
    display_order           INT NOT NULL DEFAULT 0,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP,
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_writing_items_session
    ON writing_generated_items(session_id, display_order ASC);
CREATE INDEX IF NOT EXISTS idx_writing_items_validation_status
    ON writing_generated_items(validation_status);
CREATE INDEX IF NOT EXISTS idx_writing_items_linked_vocab
    ON writing_generated_items(linked_vocabulary_id);

