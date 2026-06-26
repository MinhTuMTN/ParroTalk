CREATE TABLE IF NOT EXISTS study_activity (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    activity_date DATE NOT NULL,
    study_seconds INTEGER NOT NULL DEFAULT 0,
    segments_completed INTEGER NOT NULL DEFAULT 0,
    lessons_completed INTEGER NOT NULL DEFAULT 0,
    first_activity_at TIMESTAMP NOT NULL,
    last_activity_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_study_activity_user_date UNIQUE (user_id, activity_date)
);

CREATE INDEX IF NOT EXISTS idx_study_activity_user_date
    ON study_activity (user_id, activity_date);
