-- Migration: Add phonetic and audio_url to writing_generated_items for pronunciation & audio playback
ALTER TABLE writing_generated_items
    ADD COLUMN IF NOT EXISTS phonetic VARCHAR(255),
    ADD COLUMN IF NOT EXISTS audio_url VARCHAR(500);
