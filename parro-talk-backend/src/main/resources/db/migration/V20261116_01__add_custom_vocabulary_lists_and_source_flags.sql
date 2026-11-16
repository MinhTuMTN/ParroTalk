-- Migration: Add source flags (data_source, generation_source) and Custom Vocabulary Lists domain

-- 1. Source flags on existing core vocabulary tables
ALTER TABLE vocabularies
    ADD COLUMN IF NOT EXISTS data_source VARCHAR(40) NOT NULL DEFAULT 'IMPORT_JSON',
    ADD COLUMN IF NOT EXISTS word_forms_json TEXT;

ALTER TABLE vocabulary_senses
    ADD COLUMN IF NOT EXISTS data_source VARCHAR(40) NOT NULL DEFAULT 'IMPORT_JSON';

ALTER TABLE idioms
    ADD COLUMN IF NOT EXISTS data_source VARCHAR(40) NOT NULL DEFAULT 'IMPORT_JSON';

ALTER TABLE phrasal_verbs
    ADD COLUMN IF NOT EXISTS data_source VARCHAR(40) NOT NULL DEFAULT 'IMPORT_JSON';

-- 2. Custom Vocabulary Lists
CREATE TABLE IF NOT EXISTS vocabulary_lists (
    id                  UUID PRIMARY KEY,
    owner_id            UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title               VARCHAR(200) NOT NULL,
    description         TEXT,
    visibility          VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    share_token         VARCHAR(64) UNIQUE,
    status              VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    cloned_from_list_id UUID REFERENCES vocabulary_lists(id) ON DELETE SET NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_vocabulary_lists_owner ON vocabulary_lists(owner_id);
CREATE INDEX IF NOT EXISTS idx_vocabulary_lists_share_token ON vocabulary_lists(share_token);

-- 3. Custom Vocabulary List Items
CREATE TABLE IF NOT EXISTS vocabulary_list_items (
    id                              UUID PRIMARY KEY,
    list_id                         UUID NOT NULL REFERENCES vocabulary_lists(id) ON DELETE CASCADE,
    vocabulary_id                   UUID REFERENCES vocabularies(id) ON DELETE SET NULL,
    raw_word                        VARCHAR(255) NOT NULL,
    normalized_word                 VARCHAR(255) NOT NULL,
    source_option                   VARCHAR(30) NOT NULL DEFAULT 'DB_EXISTING',
    crawl_status                    VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    crawl_error_message             TEXT,
    user_note                       TEXT,
    display_order                   INTEGER NOT NULL DEFAULT 0,
    selected_sense_ids_json         TEXT,
    selected_idiom_ids_json         TEXT,
    selected_phrasal_verb_ids_json  TEXT,
    created_at                      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP,
    is_deleted                      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_vocab_list_items_list_word UNIQUE (list_id, normalized_word)
);

CREATE INDEX IF NOT EXISTS idx_vocabulary_list_items_list ON vocabulary_list_items(list_id);
CREATE INDEX IF NOT EXISTS idx_vocabulary_list_items_vocab ON vocabulary_list_items(vocabulary_id);

-- 4. Custom Vocabulary List Shares
CREATE TABLE IF NOT EXISTS vocabulary_list_shares (
    id                  UUID PRIMARY KEY,
    list_id             UUID NOT NULL REFERENCES vocabulary_lists(id) ON DELETE CASCADE,
    shared_with_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    granted_by_user_id  UUID REFERENCES users(id) ON DELETE SET NULL,
    role                VARCHAR(20) NOT NULL DEFAULT 'VIEWER',
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_vocab_list_shares_list_user UNIQUE (list_id, shared_with_user_id)
);

CREATE INDEX IF NOT EXISTS idx_vocabulary_list_shares_user ON vocabulary_list_shares(shared_with_user_id);

-- 5. User Progress on Custom Vocabulary Lists
CREATE TABLE IF NOT EXISTS user_vocabulary_list_progress (
    id                UUID PRIMARY KEY,
    user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    list_id           UUID NOT NULL REFERENCES vocabulary_lists(id) ON DELETE CASCADE,
    list_item_id      UUID NOT NULL REFERENCES vocabulary_list_items(id) ON DELETE CASCADE,
    mastery_level     VARCHAR(20) NOT NULL DEFAULT 'NEW',
    correct_count     INTEGER NOT NULL DEFAULT 0,
    wrong_count       INTEGER NOT NULL DEFAULT 0,
    last_practiced_at TIMESTAMP,
    next_review_at    TIMESTAMP,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP,
    is_deleted        BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_user_vocab_list_progress UNIQUE (user_id, list_item_id)
);

CREATE INDEX IF NOT EXISTS idx_user_vocab_list_progress_user_list ON user_vocabulary_list_progress(user_id, list_id);

-- 6. User In-App Notifications
CREATE TABLE IF NOT EXISTS user_notifications (
    id           UUID PRIMARY KEY,
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type         VARCHAR(40) NOT NULL,
    title        VARCHAR(255) NOT NULL,
    message      TEXT NOT NULL,
    reference_id UUID,
    payload_json TEXT,
    is_read      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP,
    is_deleted   BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_user_notifications_user_read ON user_notifications(user_id, is_read);

-- 7. Extend grammar_multiple_choice_questions with Idiom/PhrasalVerb support, question_category, and generation_source flags
ALTER TABLE grammar_multiple_choice_questions
    ADD COLUMN IF NOT EXISTS idiom_id            UUID REFERENCES idioms(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS phrasal_verb_id     UUID REFERENCES phrasal_verbs(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS question_category   VARCHAR(40) NOT NULL DEFAULT 'GRAMMAR_PATTERN',
    ADD COLUMN IF NOT EXISTS generation_source   VARCHAR(40) NOT NULL DEFAULT 'PIPELINE_BATCH',
    ADD COLUMN IF NOT EXISTS ai_provider_model   VARCHAR(100),
    ADD COLUMN IF NOT EXISTS source_list_id      UUID REFERENCES vocabulary_lists(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS created_by_user_id  UUID REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_grammar_mcq_idiom ON grammar_multiple_choice_questions(idiom_id);
CREATE INDEX IF NOT EXISTS idx_grammar_mcq_phrasal_verb ON grammar_multiple_choice_questions(phrasal_verb_id);
CREATE INDEX IF NOT EXISTS idx_grammar_mcq_generation_source ON grammar_multiple_choice_questions(generation_source);

