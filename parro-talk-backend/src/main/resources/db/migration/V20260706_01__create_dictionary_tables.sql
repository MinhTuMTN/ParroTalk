create table if not exists dictionary_entries (
    id uuid primary key,
    normalized_word varchar(255) not null,
    display_word varchar(255) not null,
    language varchar(20) not null default 'en',
    part_of_speech varchar(100),
    phonetic varchar(255),
    definitions_json jsonb not null,
    examples_json jsonb,
    synonyms_json jsonb,
    antonyms_json jsonb,
    source varchar(100),
    last_accessed_at timestamp,
    created_at timestamp not null,
    updated_at timestamp,
    is_deleted boolean not null default false,
    constraint uk_dictionary_entries_word_lang unique (normalized_word, language)
);

create index if not exists idx_dictionary_entries_normalized_word
    on dictionary_entries(normalized_word);

create table if not exists dictionary_context_lookups (
    id uuid primary key,
    normalized_word varchar(255) not null,
    original_word varchar(255) not null,
    context_hash varchar(64) not null,
    context_text text not null,
    meaning_vi text not null,
    short_meaning_vi varchar(500),
    explanation_vi text,
    part_of_speech varchar(100),
    confidence numeric(4, 3),
    provider varchar(100),
    model varchar(255),
    created_at timestamp not null,
    updated_at timestamp,
    is_deleted boolean not null default false,
    constraint uk_dictionary_context_word_hash unique (normalized_word, context_hash)
);

create index if not exists idx_dictionary_context_word
    on dictionary_context_lookups(normalized_word);

create table if not exists user_vocabularies (
    id uuid primary key,
    user_id uuid not null references users(id),
    normalized_word varchar(255) not null,
    display_word varchar(255) not null,
    dictionary_entry_id uuid references dictionary_entries(id),
    note text,
    status varchar(30) not null,
    difficulty varchar(30),
    review_count integer not null default 0,
    last_reviewed_at timestamp,
    next_review_at timestamp,
    created_at timestamp not null,
    updated_at timestamp,
    is_deleted boolean not null default false,
    constraint uk_user_vocabularies_user_word unique (user_id, normalized_word)
);

create index if not exists idx_user_vocabularies_user_status
    on user_vocabularies(user_id, status);

create index if not exists idx_user_vocabularies_next_review
    on user_vocabularies(user_id, next_review_at);

create table if not exists user_vocabulary_occurrences (
    id uuid primary key,
    user_vocabulary_id uuid not null references user_vocabularies(id) on delete cascade,
    lesson_id uuid references lessons(id),
    segment_id uuid references transcription_segments(id),
    word varchar(255) not null,
    start_time double precision,
    end_time double precision,
    context_text text,
    created_at timestamp not null,
    updated_at timestamp,
    is_deleted boolean not null default false
);

create index if not exists idx_vocab_occurrences_vocab
    on user_vocabulary_occurrences(user_vocabulary_id);

create index if not exists idx_vocab_occurrences_lesson_segment
    on user_vocabulary_occurrences(lesson_id, segment_id);
