ALTER TABLE library_sections 
ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_lessons_program_visibility_created
    ON lessons (program, visibility_status, created_at DESC)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_library_sections_active_order
    ON library_sections (is_active, order_index ASC);

