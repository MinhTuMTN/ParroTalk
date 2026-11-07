-- Migration: Add topic_practice_sentences table for Hybrid AI practice sentences
CREATE TABLE IF NOT EXISTS topic_practice_sentences (
    id                      UUID PRIMARY KEY,
    topic_id                UUID REFERENCES vocabulary_topics(id) ON DELETE SET NULL,
    vocabulary_id           UUID NOT NULL REFERENCES vocabularies(id) ON DELETE CASCADE,
    target_word             VARCHAR(255) NOT NULL,
    target_form             VARCHAR(255) NOT NULL,
    sentence_with_blank     TEXT NOT NULL,
    completed_sentence      TEXT NOT NULL,
    sentence_vn             TEXT,
    grammar_context_tip     TEXT,
    explanation             TEXT,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP,
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_topic_practice_sentences_topic_vocab
    ON topic_practice_sentences(topic_id, vocabulary_id);

CREATE INDEX IF NOT EXISTS idx_topic_practice_sentences_target_word
    ON topic_practice_sentences(target_word);
