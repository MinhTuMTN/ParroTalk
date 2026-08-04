-- ===========================================================================
-- V20260802_06: Rename categories to lesson_categories and add new columns
-- ===========================================================================

-- 1) Rename old join table lesson_categories to lesson_category_lesson if it exists
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'lesson_categories')
       AND EXISTS (SELECT FROM information_schema.columns WHERE table_name = 'lesson_categories' AND column_name = 'lesson_id') THEN
        ALTER TABLE lesson_categories RENAME TO lesson_category_lesson;
    END IF;
END $$;

-- 2) Rename table categories to lesson_categories if it exists
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'categories') 
       AND NOT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'lesson_categories') THEN
        ALTER TABLE categories RENAME TO lesson_categories;
    END IF;
END $$;

-- 2) Add new columns if they do not exist
ALTER TABLE lesson_categories ADD COLUMN IF NOT EXISTS slug VARCHAR(255);
ALTER TABLE lesson_categories ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE lesson_categories ADD COLUMN IF NOT EXISTS icon VARCHAR(255);
ALTER TABLE lesson_categories ADD COLUMN IF NOT EXISTS color VARCHAR(255);
ALTER TABLE lesson_categories ADD COLUMN IF NOT EXISTS image_url VARCHAR(255);
ALTER TABLE lesson_categories ADD COLUMN IF NOT EXISTS parent_category_id UUID;
ALTER TABLE lesson_categories ADD COLUMN IF NOT EXISTS path VARCHAR(255);
ALTER TABLE lesson_categories ADD COLUMN IF NOT EXISTS sort_order INTEGER DEFAULT 0;
ALTER TABLE lesson_categories ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'ACTIVE';
ALTER TABLE lesson_categories ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE lesson_categories ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);

-- 3) Backfill NULL values
UPDATE lesson_categories SET sort_order = 0 WHERE sort_order IS NULL;
UPDATE lesson_categories SET status = 'ACTIVE' WHERE status IS NULL;
UPDATE lesson_categories SET slug = id::text WHERE slug IS NULL;

-- 4) Add NOT NULL and UNIQUE constraints
ALTER TABLE lesson_categories ALTER COLUMN sort_order SET NOT NULL;
ALTER TABLE lesson_categories ALTER COLUMN status SET NOT NULL;
ALTER TABLE lesson_categories ALTER COLUMN slug SET NOT NULL;
ALTER TABLE lesson_categories ADD CONSTRAINT uk_lesson_categories_slug UNIQUE (slug);

-- 5) Add indexes for category tree and slug lookup
CREATE INDEX IF NOT EXISTS idx_lesson_categories_slug ON lesson_categories(slug);
CREATE INDEX IF NOT EXISTS idx_lesson_categories_parent_id ON lesson_categories(parent_category_id);
CREATE INDEX IF NOT EXISTS idx_lesson_categories_path ON lesson_categories(path);
