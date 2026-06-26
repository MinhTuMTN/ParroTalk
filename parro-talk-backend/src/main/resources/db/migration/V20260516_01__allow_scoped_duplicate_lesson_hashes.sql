ALTER TABLE lessons
    DROP CONSTRAINT IF EXISTS lessons_file_hash_key;

DROP INDEX IF EXISTS lessons_file_hash_key;

CREATE INDEX IF NOT EXISTS idx_lessons_file_hash_owner_id
    ON lessons (file_hash, owner_id);
