-- Migration: Add Fill in the Blanks mode support for dictation
-- 1) transcription_segments.blanks_json (nullable for lazy backfill of legacy rows)
-- 2) user_settings.default_dictation_mode
-- 3) dictation_mode scoping on user_lesson_progress, user_lesson_draft_segment, user_lesson_history, user_lesson_results

ALTER TABLE transcription_segments
    ADD COLUMN IF NOT EXISTS blanks_json JSONB;

ALTER TABLE user_settings
    ADD COLUMN IF NOT EXISTS default_dictation_mode VARCHAR(16) NOT NULL DEFAULT 'FULL';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_user_settings_default_dictation_mode'
    ) THEN
        ALTER TABLE user_settings
            ADD CONSTRAINT chk_user_settings_default_dictation_mode
            CHECK (default_dictation_mode IN ('EASY', 'MEDIUM', 'HARD', 'FULL'));
    END IF;
END $$;

ALTER TABLE user_lesson_progress
    ADD COLUMN IF NOT EXISTS dictation_mode VARCHAR(16) NOT NULL DEFAULT 'FULL';

ALTER TABLE user_lesson_draft_segment
    ADD COLUMN IF NOT EXISTS dictation_mode VARCHAR(16) NOT NULL DEFAULT 'FULL';

ALTER TABLE user_lesson_history
    ADD COLUMN IF NOT EXISTS dictation_mode VARCHAR(16) NOT NULL DEFAULT 'FULL';

ALTER TABLE user_lesson_results
    ADD COLUMN IF NOT EXISTS dictation_mode VARCHAR(16) NOT NULL DEFAULT 'FULL';

-- Drop any existing FK from user_lesson_draft_segment referencing user_lesson_progress before updating PKs
DO $$
DECLARE
    fk_record RECORD;
BEGIN
    FOR fk_record IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_class t ON c.conrelid = t.oid
        JOIN pg_class ref ON c.confrelid = ref.oid
        WHERE c.contype = 'f'
          AND t.relname = 'user_lesson_draft_segment'
          AND ref.relname = 'user_lesson_progress'
    LOOP
        EXECUTE format('ALTER TABLE user_lesson_draft_segment DROP CONSTRAINT IF EXISTS %I', fk_record.conname);
    END LOOP;
END $$;

-- Replace primary key on user_lesson_progress -> (user_id, lesson_id, dictation_mode)
DO $$
DECLARE
    pk_name TEXT;
BEGIN
    SELECT c.conname INTO pk_name
    FROM pg_constraint c
    JOIN pg_class t ON c.conrelid = t.oid
    WHERE c.contype = 'p'
      AND t.relname = 'user_lesson_progress';

    IF pk_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE user_lesson_progress DROP CONSTRAINT %I', pk_name);
    END IF;

    ALTER TABLE user_lesson_progress
        ADD CONSTRAINT pk_user_lesson_progress PRIMARY KEY (user_id, lesson_id, dictation_mode);
END $$;

-- Replace primary key on user_lesson_draft_segment -> (user_id, lesson_id, segment_id, dictation_mode)
DO $$
DECLARE
    pk_name TEXT;
BEGIN
    SELECT c.conname INTO pk_name
    FROM pg_constraint c
    JOIN pg_class t ON c.conrelid = t.oid
    WHERE c.contype = 'p'
      AND t.relname = 'user_lesson_draft_segment';

    IF pk_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE user_lesson_draft_segment DROP CONSTRAINT %I', pk_name);
    END IF;

    ALTER TABLE user_lesson_draft_segment
        ADD CONSTRAINT pk_user_lesson_draft_segment PRIMARY KEY (user_id, lesson_id, segment_id, dictation_mode);
END $$;

-- Recreate 3-column FK (user_id, lesson_id, dictation_mode)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_user_lesson_draft_progress_mode'
    ) THEN
        ALTER TABLE user_lesson_draft_segment
            ADD CONSTRAINT fk_user_lesson_draft_progress_mode
            FOREIGN KEY (user_id, lesson_id, dictation_mode)
            REFERENCES user_lesson_progress (user_id, lesson_id, dictation_mode)
            ON DELETE CASCADE;
    END IF;
END $$;

-- Update unique constraint/index on user_lesson_results to include dictation_mode
ALTER TABLE user_lesson_results
    DROP CONSTRAINT IF EXISTS uk_user_lesson_results_user_lesson;
DROP INDEX IF EXISTS uk_user_lesson_results_user_lesson;

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_lesson_results_user_lesson_mode
    ON user_lesson_results (user_id, lesson_id, dictation_mode);

CREATE INDEX IF NOT EXISTS idx_user_lesson_progress_user_lesson_mode
    ON user_lesson_progress (user_id, lesson_id, dictation_mode);

CREATE INDEX IF NOT EXISTS idx_user_lesson_draft_user_lesson_mode
    ON user_lesson_draft_segment (user_id, lesson_id, dictation_mode);
