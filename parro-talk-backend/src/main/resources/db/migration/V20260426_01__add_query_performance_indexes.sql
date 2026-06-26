-- Performance indexes for lesson search, progress lookup, and learning flows.
-- Target DB: PostgreSQL.

-- 1) lessons: public search + paging + owner/admin queries
CREATE INDEX IF NOT EXISTS idx_lessons_visibility_created_at
    ON lessons (visibility_status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_lessons_status_created_at
    ON lessons (status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_lessons_owner_id
    ON lessons (owner_id);

-- Functional index to speed up case-insensitive LIKE with LOWER(title)
CREATE INDEX IF NOT EXISTS idx_lessons_lower_title
    ON lessons (LOWER(title));

-- Optional partial index aligned with soft delete filter (is_deleted = false)
CREATE INDEX IF NOT EXISTS idx_lessons_visible_not_deleted
    ON lessons (visibility_status, created_at DESC)
    WHERE is_deleted = false;

-- 2) user_lesson_progress: left join by (user_id, lesson_id)
CREATE INDEX IF NOT EXISTS idx_user_lesson_progress_user_lesson
    ON user_lesson_progress (user_id, lesson_id);

-- 3) transcription_segments: load by lesson with stable order
CREATE INDEX IF NOT EXISTS idx_segments_lesson_order
    ON transcription_segments (lesson_id, display_order);

-- 4) user_lesson_draft_segment: get/delete drafts by (user_id, lesson_id)
CREATE INDEX IF NOT EXISTS idx_user_lesson_draft_user_lesson
    ON user_lesson_draft_segment (user_id, lesson_id);

-- 5) user_lesson_history: user history and lesson history timeline
CREATE INDEX IF NOT EXISTS idx_user_lesson_history_user_lesson_submitted
    ON user_lesson_history (user_id, lesson_id, submitted_at DESC);

CREATE INDEX IF NOT EXISTS idx_user_lesson_history_user_submitted
    ON user_lesson_history (user_id, submitted_at DESC);

-- 6) user_lesson_answer_detail: details lookup by history
CREATE INDEX IF NOT EXISTS idx_user_lesson_answer_detail_history
    ON user_lesson_answer_detail (history_id);

-- 7) user_lesson_results: fetch by (user_id, lesson_id)
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_lesson_results_user_lesson
    ON user_lesson_results (user_id, lesson_id);

-- 8) user_active_days: daily streak lookups
CREATE UNIQUE INDEX IF NOT EXISTS uk_user_active_days_user_date
    ON user_active_days (user_id, active_date);

CREATE INDEX IF NOT EXISTS idx_user_active_days_user_date_desc
    ON user_active_days (user_id, active_date DESC);

-- 9) lesson_categories join table
CREATE INDEX IF NOT EXISTS idx_lesson_categories_lesson_id
    ON lesson_categories (lesson_id);

CREATE INDEX IF NOT EXISTS idx_lesson_categories_category_id
    ON lesson_categories (category_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_lesson_categories_lesson_category
    ON lesson_categories (lesson_id, category_id);
