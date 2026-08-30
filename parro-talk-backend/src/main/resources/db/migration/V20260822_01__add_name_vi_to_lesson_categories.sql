-- ===========================================================================
-- V20260822_01: Add name_vi to lesson_categories and backfill default categories
-- ===========================================================================

-- 1) Add name_vi column if not exists
ALTER TABLE lesson_categories ADD COLUMN IF NOT EXISTS name_vi VARCHAR(255);

-- 2) Backfill Vietnamese names using UPDATE FROM VALUES mapping
UPDATE lesson_categories AS lc
SET name_vi = v.name_vi
FROM (VALUES
    ('work-career', 'Công việc & Nghề nghiệp'),
    ('travel-transport', 'Du lịch & Di chuyển'),
    ('daily-life', 'Đời sống hằng ngày'),
    ('family-relationships', 'Gia đình & Mối quan hệ'),
    ('health', 'Sức khỏe'),
    ('education-learning', 'Giáo dục & Học tập'),
    ('technology', 'Công nghệ'),
    ('business-finance', 'Kinh doanh & Tài chính'),
    ('science', 'Khoa học'),
    ('environment-nature', 'Môi trường & Thiên nhiên'),
    ('culture-society', 'Văn hóa & Xã hội'),
    ('entertainment', 'Giải trí'),
    ('sports', 'Thể thao'),
    ('food-restaurants', 'Thực phẩm & Nhà hàng'),
    ('shopping-services', 'Mua sắm & Dịch vụ')
) AS v(slug, name_vi)
WHERE lc.slug = v.slug;

