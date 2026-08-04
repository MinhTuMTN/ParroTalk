-- Tags Table
CREATE TABLE tags (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE
);

-- Admin Vocabularies Table
CREATE TABLE admin_vocabularies (
    id UUID PRIMARY KEY,
    word VARCHAR(255) NOT NULL,
    ipa_uk VARCHAR(255),
    ipa_us VARCHAR(255),
    audio_uk VARCHAR(500),
    audio_us VARCHAR(500),
    cefr_level VARCHAR(10),
    frequency_rank INTEGER,
    part_of_speech VARCHAR(100),
    image_url VARCHAR(500),
    notes TEXT,
    source VARCHAR(255),
    status VARCHAR(50) DEFAULT 'DRAFT',
    created_by UUID,
    updated_by UUID,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_admin_vocabularies_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_admin_vocabularies_updated_by FOREIGN KEY (updated_by) REFERENCES users(id)
);

CREATE INDEX idx_admin_vocabularies_word ON admin_vocabularies(word);
CREATE INDEX idx_admin_vocabularies_cefr ON admin_vocabularies(cefr_level);

-- Vocabulary Definitions
CREATE TABLE admin_vocabulary_definitions (
    id UUID PRIMARY KEY,
    vocabulary_id UUID NOT NULL,
    definition TEXT NOT NULL,
    english_definition TEXT,
    vietnamese_definition TEXT,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_vocab_definitions_vocab FOREIGN KEY (vocabulary_id) REFERENCES admin_vocabularies(id) ON DELETE CASCADE
);

CREATE INDEX idx_admin_vocab_def_vocab_id ON admin_vocabulary_definitions(vocabulary_id);

-- Vocabulary Examples
CREATE TABLE admin_vocabulary_examples (
    id UUID PRIMARY KEY,
    vocabulary_id UUID NOT NULL,
    sentence TEXT NOT NULL,
    translation TEXT,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_vocab_examples_vocab FOREIGN KEY (vocabulary_id) REFERENCES admin_vocabularies(id) ON DELETE CASCADE
);

CREATE INDEX idx_admin_vocab_ex_vocab_id ON admin_vocabulary_examples(vocabulary_id);

-- Vocabulary Relations (Synonyms, Antonyms, etc.)
CREATE TABLE admin_vocabulary_relations (
    id UUID PRIMARY KEY,
    vocabulary_id UUID NOT NULL,
    relation_type VARCHAR(50) NOT NULL, -- SYNONYM, ANTONYM, COLLOCATION, IDIOM, PHRASAL_VERB, WORD_FORM
    related_word VARCHAR(255) NOT NULL,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_vocab_relations_vocab FOREIGN KEY (vocabulary_id) REFERENCES admin_vocabularies(id) ON DELETE CASCADE
);

CREATE INDEX idx_admin_vocab_rel_vocab_id ON admin_vocabulary_relations(vocabulary_id, relation_type);

-- Vocabulary Categories
CREATE TABLE admin_vocabulary_categories (
    vocabulary_id UUID NOT NULL,
    category_id UUID NOT NULL,
    PRIMARY KEY (vocabulary_id, category_id),
    CONSTRAINT fk_vocab_cat_vocab FOREIGN KEY (vocabulary_id) REFERENCES admin_vocabularies(id) ON DELETE CASCADE,
    CONSTRAINT fk_vocab_cat_cat FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

-- Vocabulary Tags
CREATE TABLE admin_vocabulary_tags (
    vocabulary_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    PRIMARY KEY (vocabulary_id, tag_id),
    CONSTRAINT fk_vocab_tag_vocab FOREIGN KEY (vocabulary_id) REFERENCES admin_vocabularies(id) ON DELETE CASCADE,
    CONSTRAINT fk_vocab_tag_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

-- Import Jobs
CREATE TABLE admin_import_jobs (
    id UUID PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_rows INTEGER DEFAULT 0,
    success_rows INTEGER DEFAULT 0,
    error_rows INTEGER DEFAULT 0,
    started_at TIMESTAMP WITHOUT TIME ZONE,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    created_by UUID,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE
);

-- Import Errors
CREATE TABLE admin_import_errors (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL,
    row_number INTEGER NOT NULL,
    error_message TEXT NOT NULL,
    raw_data TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_import_errors_job FOREIGN KEY (job_id) REFERENCES admin_import_jobs(id) ON DELETE CASCADE
);

-- Audit Logs
CREATE TABLE admin_audit_logs (
    id UUID PRIMARY KEY,
    entity_name VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    changes JSONB,
    created_by UUID,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_logs_user FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE INDEX idx_admin_audit_entity ON admin_audit_logs(entity_name, entity_id);
