-- ===========================================================================
-- V20260802_04: Create password_reset_tokens table for forgot-password flow
-- ===========================================================================

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id            UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID            NOT NULL,
    token_hash    VARCHAR(64)     NOT NULL,
    expired_at    TIMESTAMP       NOT NULL,
    used_at       TIMESTAMP,
    created_by    VARCHAR(255)    NOT NULL DEFAULT 'system',
    created_at    TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP       NOT NULL DEFAULT now(),
    is_deleted    BOOLEAN         NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_prt_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Unique index on token_hash for fast look-ups
CREATE UNIQUE INDEX IF NOT EXISTS idx_prt_token_hash
    ON password_reset_tokens (token_hash);

-- Index for finding all tokens belonging to a user (invalidation query)
CREATE INDEX IF NOT EXISTS idx_prt_user_id
    ON password_reset_tokens (user_id);

-- Index for scheduled cleanup of expired tokens
CREATE INDEX IF NOT EXISTS idx_prt_expired_at
    ON password_reset_tokens (expired_at);
