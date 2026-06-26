-- Google OAuth account linking support.

ALTER TABLE users
    ALTER COLUMN password DROP NOT NULL;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(2048);

CREATE TABLE IF NOT EXISTS user_oauth_accounts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id),
    provider VARCHAR(32) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_user_oauth_accounts_user_id
    ON user_oauth_accounts (user_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_oauth_accounts_provider_subject
    ON user_oauth_accounts (provider, provider_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_oauth_accounts_user_provider
    ON user_oauth_accounts (user_id, provider);
