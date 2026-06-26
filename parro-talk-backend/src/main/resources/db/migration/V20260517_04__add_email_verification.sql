-- Existing accounts are marked verified to avoid locking out current users
-- when email verification is introduced.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMP NULL;

UPDATE users
SET email_verified = TRUE,
    email_verified_at = COALESCE(email_verified_at, created_at)
WHERE email_verified = TRUE
  AND email_verified_at IS NULL;
