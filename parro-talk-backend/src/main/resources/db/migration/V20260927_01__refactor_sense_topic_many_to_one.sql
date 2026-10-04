-- Migration: refactor sense-topic relationship from M:N join table to 1:N FK on vocabulary_senses
-- The topic_id column and FK constraint were already created by the normalize_topics.py migration script.
-- This migration adds the performance index and is idempotent.

-- Add index for topic_id lookups (idempotent)
CREATE INDEX IF NOT EXISTS idx_vocabulary_senses_topic_id
    ON vocabulary_senses (topic_id);

-- The old join table vocabulary_sense_topics is kept as a backup.
-- Uncomment to drop when no longer needed:
-- DROP TABLE IF EXISTS vocabulary_sense_topics;
