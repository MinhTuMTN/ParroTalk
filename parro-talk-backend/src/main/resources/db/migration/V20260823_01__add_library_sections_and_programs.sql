-- ===========================================================================
-- V20260823_01: Add library sections and program/source columns to lessons
-- ===========================================================================

-- 1. Add program and source columns to lessons
ALTER TABLE lessons 
ADD COLUMN IF NOT EXISTS program VARCHAR(255),
ADD COLUMN IF NOT EXISTS source VARCHAR(255);

-- 2. Create library_sections table
CREATE TABLE IF NOT EXISTS library_sections (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    category_id UUID,
    program VARCHAR(255),
    order_index INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    max_items INT DEFAULT 6,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Insert default sections
DO $$
DECLARE
    new_id UUID;
    cat_id UUID;
BEGIN
    -- IN_PROGRESS
    INSERT INTO library_sections (id, title, type, order_index, is_active, max_items)
    VALUES (gen_random_uuid(), 'Continue Learning', 'IN_PROGRESS', 1, TRUE, 6);

    -- USER_PROCESSING
    INSERT INTO library_sections (id, title, type, order_index, is_active, max_items)
    VALUES (gen_random_uuid(), 'Your Processing Lessons', 'USER_PROCESSING', 2, TRUE, 6);

    -- USER_UPLOADED
    INSERT INTO library_sections (id, title, type, order_index, is_active, max_items)
    VALUES (gen_random_uuid(), 'Your Uploaded Lessons', 'USER_UPLOADED', 3, TRUE, 6);

    -- PROGRAM: 6 Minute English
    INSERT INTO library_sections (id, title, type, program, order_index, is_active, max_items)
    VALUES (gen_random_uuid(), '6 Minute English', 'PROGRAM', '6 Minute English', 4, TRUE, 6);

    -- PROGRAM: TED Talks
    INSERT INTO library_sections (id, title, type, program, order_index, is_active, max_items)
    VALUES (gen_random_uuid(), 'TED Talks', 'PROGRAM', 'TED Talks', 5, TRUE, 6);

    -- CATEGORY: Work & Career
    SELECT id INTO cat_id FROM lesson_categories WHERE slug = 'work-career' LIMIT 1;
    IF cat_id IS NOT NULL THEN
        INSERT INTO library_sections (id, title, type, category_id, order_index, is_active, max_items)
        VALUES (gen_random_uuid(), 'Work & Career', 'CATEGORY', cat_id, 6, TRUE, 6);
    END IF;

    -- CATEGORY: Technology
    SELECT id INTO cat_id FROM lesson_categories WHERE slug = 'technology' LIMIT 1;
    IF cat_id IS NOT NULL THEN
        INSERT INTO library_sections (id, title, type, category_id, order_index, is_active, max_items)
        VALUES (gen_random_uuid(), 'Technology', 'CATEGORY', cat_id, 7, TRUE, 6);
    END IF;

    -- CATEGORY: Daily Life
    SELECT id INTO cat_id FROM lesson_categories WHERE slug = 'daily-life' LIMIT 1;
    IF cat_id IS NOT NULL THEN
        INSERT INTO library_sections (id, title, type, category_id, order_index, is_active, max_items)
        VALUES (gen_random_uuid(), 'Daily Life', 'CATEGORY', cat_id, 8, TRUE, 6);
    END IF;
END $$;
