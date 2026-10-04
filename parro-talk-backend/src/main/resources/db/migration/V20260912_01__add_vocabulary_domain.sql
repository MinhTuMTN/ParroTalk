create table if not exists vocabularies (
    id uuid primary key,
    word varchar(255) not null,
    normalized_word varchar(255) not null,
    common_meaning_vn varchar(500),
    created_at timestamp not null,
    updated_at timestamp,
    is_deleted boolean not null default false,
    constraint uk_vocabularies_normalized_word unique (normalized_word)
);

create index if not exists idx_vocabularies_normalized_word on vocabularies(normalized_word);

create table if not exists vocabulary_pronunciations (
    id uuid primary key,
    vocabulary_id uuid not null references vocabularies(id) on delete cascade,
    accent varchar(50) not null,
    ipa varchar(255),
    audio_url varchar(500),
    created_at timestamp not null,
    updated_at timestamp,
    is_deleted boolean not null default false,
    constraint uk_vocab_pronunciation unique (vocabulary_id, accent)
);

create table if not exists vocabulary_topics (
    id uuid primary key,
    name varchar(255) not null,
    created_at timestamp not null,
    updated_at timestamp,
    is_deleted boolean not null default false,
    constraint uk_vocabulary_topics_name unique (name)
);

create table if not exists vocabulary_topic_links (
    vocabulary_id uuid not null references vocabularies(id) on delete cascade,
    topic_id uuid not null references vocabulary_topics(id) on delete cascade,
    primary key (vocabulary_id, topic_id)
);

create table if not exists vocabulary_senses (
    id uuid primary key,
    vocabulary_id uuid not null references vocabularies(id) on delete cascade,
    part_of_speech varchar(100),
    cefr_level varchar(10),
    is_ai_cefr boolean not null default false,
    definition_en text,
    definition_vn text,
    example_en text,
    example_vn text,
    created_at timestamp not null,
    updated_at timestamp,
    is_deleted boolean not null default false
);

create index if not exists idx_vocabulary_senses_vocab on vocabulary_senses(vocabulary_id);

create table if not exists sense_synonyms (
    id uuid primary key,
    sense_id uuid not null references vocabulary_senses(id) on delete cascade,
    synonym_word varchar(255) not null,
    synonym_vn varchar(255),
    linked_vocabulary_id uuid references vocabularies(id) on delete set null,
    created_at timestamp not null,
    updated_at timestamp,
    is_deleted boolean not null default false
);

create table if not exists sense_antonyms (
    id uuid primary key,
    sense_id uuid not null references vocabulary_senses(id) on delete cascade,
    antonym_word varchar(255) not null,
    antonym_vn varchar(255),
    linked_vocabulary_id uuid references vocabularies(id) on delete set null,
    created_at timestamp not null,
    updated_at timestamp,
    is_deleted boolean not null default false
);

create table if not exists sense_collocations (
    id uuid primary key,
    sense_id uuid not null references vocabulary_senses(id) on delete cascade,
    collocation_word varchar(255) not null,
    collocation_vn varchar(255),
    created_at timestamp not null,
    updated_at timestamp,
    is_deleted boolean not null default false
);

create table if not exists idioms (
    id uuid primary key,
    vocabulary_id uuid not null references vocabularies(id) on delete cascade,
    phrase varchar(255) not null,
    definition text,
    definition_vn text,
    example text,
    example_vn text,
    created_at timestamp not null,
    updated_at timestamp,
    is_deleted boolean not null default false
);

create table if not exists phrasal_verbs (
    id uuid primary key,
    vocabulary_id uuid not null references vocabularies(id) on delete cascade,
    phrase varchar(255) not null,
    definition text,
    definition_vn text,
    example text,
    example_vn text,
    created_at timestamp not null,
    updated_at timestamp,
    is_deleted boolean not null default false
);
