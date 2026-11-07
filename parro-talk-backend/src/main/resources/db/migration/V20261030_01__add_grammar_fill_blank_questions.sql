-- Migration: Grammar fill-in-the-blank question bank (hand-authored, linked to vocabulary senses)
CREATE TABLE IF NOT EXISTS grammar_fill_blank_questions (
    id                  UUID PRIMARY KEY,
    sense_id            UUID REFERENCES vocabulary_senses(id) ON DELETE SET NULL,
    vocabulary_id       UUID REFERENCES vocabularies(id) ON DELETE SET NULL,
    grammar_topic       VARCHAR(64)  NOT NULL,
    difficulty          VARCHAR(2)   NOT NULL CHECK (difficulty IN ('A1','A2','B1','B2','C1','C2')),
    sentence_with_blanks TEXT        NOT NULL,   -- blanks written as ____
    completed_sentence  TEXT         NOT NULL,
    answers_json        TEXT         NOT NULL,   -- JSON array, one entry per blank: ["answer", ...accepted alternatives]
    blank_count         INT          NOT NULL DEFAULT 1,
    explanation_en      TEXT         NOT NULL,
    explanation_vn      TEXT,
    content_hash        VARCHAR(64)  NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_grammar_fib_content_hash ON grammar_fill_blank_questions(content_hash);
CREATE INDEX IF NOT EXISTS idx_grammar_fib_topic_diff ON grammar_fill_blank_questions(grammar_topic, difficulty);
CREATE INDEX IF NOT EXISTS idx_grammar_fib_sense ON grammar_fill_blank_questions(sense_id);
