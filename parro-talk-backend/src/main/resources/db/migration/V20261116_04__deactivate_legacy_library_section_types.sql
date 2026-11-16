-- Migration: Deactivate legacy library_sections rows with removed SectionType values
UPDATE library_sections
SET is_active = false
WHERE type IN ('USER_UPLOADED', 'USER_PROCESSING')
  AND is_active = true;
