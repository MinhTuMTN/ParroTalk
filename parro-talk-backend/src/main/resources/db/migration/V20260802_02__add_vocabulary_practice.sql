-- Update user_vocabularies table for SM-2
ALTER TABLE user_vocabularies
    ADD COLUMN ease_factor DOUBLE PRECISION DEFAULT 2.5 NOT NULL,
    ADD COLUMN interval_days INTEGER DEFAULT 0 NOT NULL,
    ADD COLUMN repetitions INTEGER DEFAULT 0 NOT NULL,
    ADD COLUMN correct_count INTEGER DEFAULT 0 NOT NULL,
    ADD COLUMN wrong_count INTEGER DEFAULT 0 NOT NULL,
    ADD COLUMN is_favorite BOOLEAN DEFAULT FALSE NOT NULL;

-- Create practice_sessions table
CREATE TABLE practice_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    started_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    total_questions INTEGER DEFAULT 0 NOT NULL,
    correct_answers INTEGER DEFAULT 0 NOT NULL,
    status VARCHAR(30) NOT NULL,
    xp_earned INTEGER DEFAULT 0 NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_practice_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_practice_sessions_user ON practice_sessions(user_id, status);

-- Create practice_answers table
CREATE TABLE practice_answers (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    user_vocabulary_id UUID NOT NULL,
    question_type VARCHAR(30) NOT NULL,
    is_correct BOOLEAN NOT NULL,
    user_answer TEXT,
    rating VARCHAR(20),
    answered_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    time_spent_ms BIGINT DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_practice_answers_session FOREIGN KEY (session_id) REFERENCES practice_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_practice_answers_vocab FOREIGN KEY (user_vocabulary_id) REFERENCES user_vocabularies(id) ON DELETE CASCADE
);

CREATE INDEX idx_practice_answers_session ON practice_answers(session_id);
CREATE INDEX idx_practice_answers_vocab ON practice_answers(user_vocabulary_id);
