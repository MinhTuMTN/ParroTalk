-- Migration: Grammar multiple-choice question bank (hand-authored, linked to vocabulary senses)
CREATE TABLE IF NOT EXISTS grammar_multiple_choice_questions (
    id                  UUID PRIMARY KEY,
    sense_id            UUID REFERENCES vocabulary_senses(id) ON DELETE SET NULL,
    vocabulary_id       UUID REFERENCES vocabularies(id) ON DELETE SET NULL,
    grammar_topic       VARCHAR(64)  NOT NULL,
    difficulty          VARCHAR(2)   NOT NULL CHECK (difficulty IN ('A1','A2','B1','B2','C1','C2')),
    question_text       TEXT         NOT NULL,   -- blank written as ____
    options_json        TEXT         NOT NULL,   -- JSON array of 4 options
    correct_index       SMALLINT     NOT NULL CHECK (correct_index BETWEEN 0 AND 3),
    correct_answer      VARCHAR(255) NOT NULL,
    completed_sentence  TEXT         NOT NULL,
    explanation_en      TEXT         NOT NULL,
    explanation_vn      TEXT,
    content_hash        VARCHAR(64)  NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_grammar_mcq_content_hash ON grammar_multiple_choice_questions(content_hash);
CREATE INDEX IF NOT EXISTS idx_grammar_mcq_topic_diff ON grammar_multiple_choice_questions(grammar_topic, difficulty);
CREATE INDEX IF NOT EXISTS idx_grammar_mcq_sense ON grammar_multiple_choice_questions(sense_id);
