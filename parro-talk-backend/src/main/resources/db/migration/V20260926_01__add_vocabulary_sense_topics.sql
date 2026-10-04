create table if not exists vocabulary_sense_topics (
    sense_id uuid not null references vocabulary_senses(id) on delete cascade,
    topic_id uuid not null references vocabulary_topics(id) on delete cascade,
    primary key (sense_id, topic_id)
);
