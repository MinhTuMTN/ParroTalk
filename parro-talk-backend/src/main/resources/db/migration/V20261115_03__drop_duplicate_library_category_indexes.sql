-- Drop duplicate indexes on lesson_category_lesson and lesson_categories.
-- Rollback:
--   CREATE INDEX IF NOT EXISTS idx_lesson_category_lesson_category ON lesson_category_lesson (lesson_id, category_id);
--   CREATE INDEX IF NOT EXISTS idx_lesson_categories_lesson_category ON lesson_category_lesson (lesson_id, category_id);
--   CREATE INDEX IF NOT EXISTS idx_lesson_categories_lesson_id ON lesson_category_lesson (lesson_id);
--   CREATE INDEX IF NOT EXISTS idx_lesson_categories_category_id ON lesson_category_lesson (category_id);
--   CREATE INDEX IF NOT EXISTS idx_lesson_categories_slug ON lesson_categories (slug);

DROP INDEX IF EXISTS idx_lesson_category_lesson_category;
DROP INDEX IF EXISTS idx_lesson_categories_lesson_category;
DROP INDEX IF EXISTS idx_lesson_categories_lesson_id;
DROP INDEX IF EXISTS idx_lesson_categories_category_id;
DROP INDEX IF EXISTS idx_lesson_categories_slug;
