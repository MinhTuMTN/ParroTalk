ALTER TABLE vocabularies
    ADD COLUMN IF NOT EXISTS word_forms jsonb NOT NULL DEFAULT '[]'::jsonb;

CREATE INDEX IF NOT EXISTS idx_vocabularies_word_forms_gin
    ON vocabularies USING GIN (word_forms);
