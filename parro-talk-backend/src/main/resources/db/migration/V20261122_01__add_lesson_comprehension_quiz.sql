-- Migration: Add Listening Comprehension Quiz tables from lesson transcript
CREATE TABLE IF NOT EXISTS lesson_quiz_sets (
    id                  UUID PRIMARY KEY,
    lesson_id           UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    mode                VARCHAR(32) NOT NULL CHECK (mode IN ('MULTIPLE_CHOICE', 'FILL_IN_THE_BLANK', 'MIXED')),
    question_count      INT NOT NULL,
    transcript_hash     VARCHAR(64) NOT NULL,
    ai_provider_model   VARCHAR(100),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    generated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_user_id  UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_lesson_quiz_sets_active
    ON lesson_quiz_sets(lesson_id, mode)
    WHERE is_active = TRUE AND is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_lesson_quiz_sets_lesson_mode_generated
    ON lesson_quiz_sets(lesson_id, mode, generated_at DESC);

CREATE TABLE IF NOT EXISTS lesson_quiz_questions (
    id                  UUID PRIMARY KEY,
    quiz_set_id         UUID NOT NULL REFERENCES lesson_quiz_sets(id) ON DELETE CASCADE,
    display_order       INT NOT NULL,
    question_type       VARCHAR(32) NOT NULL CHECK (question_type IN ('MULTIPLE_CHOICE', 'FILL_IN_THE_BLANK')),
    question_text       TEXT NOT NULL,
    options_json        TEXT,
    correct_index       SMALLINT,
    model_answer        TEXT NOT NULL,
    explanation_vn      TEXT,
    evidence_segment_id UUID REFERENCES transcription_segments(id) ON DELETE SET NULL,
    evidence_text       TEXT,
    evidence_start_time DOUBLE PRECISION,
    evidence_end_time   DOUBLE PRECISION,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_lesson_quiz_questions_set_order
    ON lesson_quiz_questions(quiz_set_id, display_order ASC);

CREATE TABLE IF NOT EXISTS user_lesson_quiz_attempts (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lesson_id           UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    quiz_set_id         UUID NOT NULL REFERENCES lesson_quiz_sets(id) ON DELETE CASCADE,
    mode                VARCHAR(32) NOT NULL CHECK (mode IN ('MULTIPLE_CHOICE', 'FILL_IN_THE_BLANK', 'MIXED')),
    status              VARCHAR(32) NOT NULL CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
    total_questions     INT NOT NULL,
    answered_count      INT NOT NULL DEFAULT 0,
    correct_count       INT NOT NULL DEFAULT 0,
    partial_count       INT NOT NULL DEFAULT 0,
    overall_score       INT NOT NULL DEFAULT 0,
    started_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at        TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_user_lesson_quiz_attempts_user_lesson
    ON user_lesson_quiz_attempts(user_id, lesson_id, started_at DESC);

CREATE TABLE IF NOT EXISTS user_lesson_quiz_answers (
    id                  UUID PRIMARY KEY,
    attempt_id          UUID NOT NULL REFERENCES user_lesson_quiz_attempts(id) ON DELETE CASCADE,
    question_id         UUID NOT NULL REFERENCES lesson_quiz_questions(id) ON DELETE CASCADE,
    user_answer         TEXT NOT NULL,
    selected_index      SMALLINT,
    grading_status      VARCHAR(32) NOT NULL CHECK (grading_status IN ('CORRECT', 'PARTIAL', 'INCORRECT')),
    score               INT NOT NULL,
    feedback_vn         TEXT,
    answered_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_quiz_answer_attempt_question UNIQUE (attempt_id, question_id)
);

CREATE INDEX IF NOT EXISTS idx_user_lesson_quiz_answers_attempt
    ON user_lesson_quiz_answers(attempt_id);
