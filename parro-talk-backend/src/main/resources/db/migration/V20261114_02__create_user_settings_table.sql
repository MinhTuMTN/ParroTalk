-- Migration: Create user_settings table storing learner preferences across all 5 setting groups
CREATE TABLE IF NOT EXISTS user_settings (
    id                          UUID PRIMARY KEY,
    user_id                     UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    ui_language                 VARCHAR(16) NOT NULL DEFAULT 'vi',
    translation_language        VARCHAR(16) NOT NULL DEFAULT 'vi',
    theme                       VARCHAR(16) NOT NULL DEFAULT 'light',
    preferred_accent            VARCHAR(16) NOT NULL DEFAULT 'US',
    preferred_us_voice          VARCHAR(64) NOT NULL DEFAULT 'en-US-JennyNeural',
    preferred_uk_voice          VARCHAR(64) NOT NULL DEFAULT 'en-GB-SoniaNeural',
    playback_speed              DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    auto_play_vocab_audio       BOOLEAN NOT NULL DEFAULT FALSE,
    dictation_auto_advance      BOOLEAN NOT NULL DEFAULT FALSE,
    dictation_show_translation  BOOLEAN NOT NULL DEFAULT TRUE,
    dictation_auto_replay       BOOLEAN NOT NULL DEFAULT FALSE,
    dictation_replay_key        VARCHAR(32) NOT NULL DEFAULT 'Escape',
    default_writing_mode        VARCHAR(32) NOT NULL DEFAULT 'SUGGESTION',
    default_difficulty_scale    VARCHAR(16) NOT NULL DEFAULT 'CEFR',
    default_target_level        VARCHAR(16) NOT NULL DEFAULT 'B2',
    vocab_session_size          INTEGER NOT NULL DEFAULT 10,
    default_flashcard_direction VARCHAR(16) NOT NULL DEFAULT 'EN_VI',
    daily_goal                  INTEGER NOT NULL DEFAULT 20,
    created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP,
    is_deleted                  BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_user_settings_user_id
    ON user_settings(user_id);
