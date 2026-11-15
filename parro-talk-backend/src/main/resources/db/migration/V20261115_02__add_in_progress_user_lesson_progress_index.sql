-- Add partial covering index on user_lesson_progress for IN_PROGRESS lessons query.
-- Supports filtering by user_id and (0 < last_progress < 1.0) ordered by updated_at DESC
-- while covering lesson_id and last_progress for Index-Only Scans as the table grows.
-- Note: For zero-downtime production rollout, run with CREATE INDEX CONCURRENTLY outside a transaction.
-- Rollback: DROP INDEX IF EXISTS idx_ulp_user_updated_in_progress;

CREATE INDEX IF NOT EXISTS idx_ulp_user_updated_in_progress
    ON user_lesson_progress (user_id, updated_at DESC)
    INCLUDE (lesson_id, last_progress)
    WHERE last_progress > 0 AND last_progress < 1.0;
