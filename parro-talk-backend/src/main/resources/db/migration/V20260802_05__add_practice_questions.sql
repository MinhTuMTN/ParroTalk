-- ===========================================================================
-- V20260802_05: Create practice_questions table to persist generated questions
-- ===========================================================================

CREATE TABLE IF NOT EXISTS practice_questions (
    id                    UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id            UUID            NOT NULL,
    user_vocabulary_id    UUID            NOT NULL,
    question_type         VARCHAR(30)     NOT NULL,
    options_json          TEXT,
    
    created_at            TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at            TIMESTAMP       NOT NULL DEFAULT now(),
    is_deleted            BOOLEAN         NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_pq_session
        FOREIGN KEY (session_id) REFERENCES practice_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_pq_vocabulary
        FOREIGN KEY (user_vocabulary_id) REFERENCES user_vocabularies(id) ON DELETE CASCADE
);

-- Index for fetching questions belonging to a session
CREATE INDEX IF NOT EXISTS idx_pq_session_id
    ON practice_questions (session_id);
