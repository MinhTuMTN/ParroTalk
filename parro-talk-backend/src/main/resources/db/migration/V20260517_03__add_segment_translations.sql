CREATE TABLE IF NOT EXISTS segment_translations (
    id UUID PRIMARY KEY,
    segment_id UUID NOT NULL REFERENCES transcription_segments (id) ON DELETE CASCADE,
    target_language VARCHAR(16) NOT NULL,
    translated_text TEXT NOT NULL,
    provider VARCHAR(64) NOT NULL,
    model VARCHAR(128) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_segment_translations_segment_language UNIQUE (segment_id, target_language)
);

CREATE INDEX IF NOT EXISTS idx_segment_translations_segment_id
    ON segment_translations (segment_id);

CREATE INDEX IF NOT EXISTS idx_segment_translations_language
    ON segment_translations (target_language);
