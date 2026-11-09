-- Migration: Add target_cefr to vocabulary_lists table
ALTER TABLE vocabulary_lists
    ADD COLUMN IF NOT EXISTS target_cefr VARCHAR(10);
