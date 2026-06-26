ALTER TABLE users
    ADD COLUMN IF NOT EXISTS username VARCHAR(120);

UPDATE users
SET username = LOWER(SPLIT_PART(email, '@', 1)) || '-' || SUBSTRING(REPLACE(id::text, '-', ''), 1, 6)
WHERE username IS NULL;

ALTER TABLE users
    ALTER COLUMN username SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_username_lower
    ON users (LOWER(username));
