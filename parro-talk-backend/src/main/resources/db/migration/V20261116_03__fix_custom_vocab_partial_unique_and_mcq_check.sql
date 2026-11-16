-- Migration: Ensure partial unique indexes for soft-deleted custom vocab items/shares and MCQ target CHECK constraint

ALTER TABLE vocabulary_list_items
    DROP CONSTRAINT IF EXISTS uk_vocab_list_items_list_word;

CREATE UNIQUE INDEX IF NOT EXISTS uk_vocab_list_items_list_word_active
    ON vocabulary_list_items (list_id, normalized_word)
    WHERE is_deleted = false;

ALTER TABLE vocabulary_list_shares
    DROP CONSTRAINT IF EXISTS uk_vocab_list_shares_list_user;

CREATE UNIQUE INDEX IF NOT EXISTS uk_vocab_list_shares_list_user_active
    ON vocabulary_list_shares (list_id, shared_with_user_id)
    WHERE is_deleted = false;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_grammar_mcq_has_target'
    ) THEN
        ALTER TABLE grammar_multiple_choice_questions
            ADD CONSTRAINT chk_grammar_mcq_has_target
            CHECK (sense_id IS NOT NULL OR idiom_id IS NOT NULL OR phrasal_verb_id IS NOT NULL) NOT VALID;
    END IF;
END $$;
