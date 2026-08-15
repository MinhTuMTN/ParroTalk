-- Migration V20260808_01__refactor_vocabulary_and_add_reports.sql

-- Alter dictionary_entries table
ALTER TABLE dictionary_entries
    ADD COLUMN topic VARCHAR(100),
    ADD COLUMN collocations_json JSONB,
    ADD COLUMN idioms_json JSONB,
    ADD COLUMN phrasal_verbs_json JSONB;

-- Add indexes
CREATE INDEX idx_dictionary_entries_topic ON dictionary_entries (topic);
CREATE INDEX idx_dictionary_entries_cefr ON dictionary_entries (cefr_level);

-- Create vocabulary_reports table
CREATE TABLE vocabulary_reports (
    id UUID PRIMARY KEY,
    reporter_id UUID NOT NULL REFERENCES users(id),
    dictionary_entry_id UUID NOT NULL REFERENCES dictionary_entries(id),
    report_type VARCHAR(50) NOT NULL,
    reason VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(30) DEFAULT 'OPEN',
    priority VARCHAR(30) DEFAULT 'MEDIUM',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE
);
