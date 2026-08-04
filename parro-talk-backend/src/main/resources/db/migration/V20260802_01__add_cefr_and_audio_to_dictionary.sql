ALTER TABLE dictionary_entries 
    ADD COLUMN IF NOT EXISTS cefr_level VARCHAR(10),
    ADD COLUMN IF NOT EXISTS audio_uk_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS audio_us_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS common_meaning_vi VARCHAR(500);
