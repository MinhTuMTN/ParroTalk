-- Lesson Report: single table, discriminated by report_type.
-- segment_id is nullable at column level but constrained per report type below.
create table if not exists lesson_reports (
    id uuid primary key,
    lesson_id uuid not null references lessons(id),
    segment_id uuid references transcription_segments(id),
    report_type varchar(20) not null,
    reason varchar(40) not null,
    description varchar(1000),
    reporter_id uuid not null references users(id),
    status varchar(20) not null default 'OPEN',
    priority varchar(20) not null default 'MEDIUM',
    assignee_id uuid references users(id),
    resolution_note varchar(1000),
    resolved_at timestamp,
    created_at timestamp not null,
    updated_at timestamp,
    is_deleted boolean not null default false,
    constraint ck_lesson_reports_translation_requires_segment
        check (report_type <> 'TRANSLATION' or segment_id is not null),
    constraint ck_lesson_reports_file_has_no_segment
        check (report_type <> 'FILE' or segment_id is null)
);

create index if not exists idx_lesson_reports_lesson_created_at
    on lesson_reports(lesson_id, created_at desc);

create index if not exists idx_lesson_reports_status_created_at
    on lesson_reports(status, created_at desc);

create index if not exists idx_lesson_reports_reporter_created_at
    on lesson_reports(reporter_id, created_at desc);

create index if not exists idx_lesson_reports_assignee_status
    on lesson_reports(assignee_id, status);

create index if not exists idx_lesson_reports_segment_id
    on lesson_reports(segment_id);

-- App Feedback: independent domain, no relation to lesson data.
create table if not exists app_feedbacks (
    id uuid primary key,
    user_id uuid not null references users(id),
    category varchar(30) not null,
    title varchar(150) not null,
    description varchar(4000) not null,
    status varchar(20) not null default 'OPEN',
    priority varchar(20) not null default 'MEDIUM',
    assignee_id uuid references users(id),
    resolution_note varchar(1000),
    resolved_at timestamp,
    created_at timestamp not null,
    updated_at timestamp,
    is_deleted boolean not null default false
);

create index if not exists idx_app_feedbacks_status_created_at
    on app_feedbacks(status, created_at desc);

create index if not exists idx_app_feedbacks_category_status
    on app_feedbacks(category, status);

create index if not exists idx_app_feedbacks_user_created_at
    on app_feedbacks(user_id, created_at desc);

create index if not exists idx_app_feedbacks_assignee_status
    on app_feedbacks(assignee_id, status);

-- Shared append-only audit trail for both domains.
-- target_id has no foreign key on purpose: the log is immutable and must
-- survive soft-deleted targets, and a single table avoids duplicating an
-- identical structure per domain.
create table if not exists moderation_events (
    id uuid primary key,
    target_type varchar(30) not null,
    target_id uuid not null,
    actor_id uuid not null references users(id),
    field varchar(30) not null,
    old_value varchar(255),
    new_value varchar(255),
    note varchar(1000),
    created_at timestamp not null,
    updated_at timestamp,
    is_deleted boolean not null default false
);

create index if not exists idx_moderation_events_target
    on moderation_events(target_type, target_id, created_at);

create index if not exists idx_moderation_events_actor
    on moderation_events(actor_id, created_at desc);
