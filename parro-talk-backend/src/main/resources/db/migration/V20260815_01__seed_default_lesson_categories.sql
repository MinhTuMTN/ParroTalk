-- ===========================================================================
-- V20260815_01: Seed default lesson categories
-- ===========================================================================

DO $$
DECLARE
    cat RECORD;
    new_id UUID;
BEGIN
    FOR cat IN
        SELECT * FROM (VALUES
            ('Work & Career', 'work-career', 'Công việc & Nghề nghiệp', 1),
            ('Travel & Transportation', 'travel-transport', 'Du lịch & Di chuyển', 2),
            ('Daily Life', 'daily-life', 'Đời sống hằng ngày', 3),
            ('Family & Relationships', 'family-relationships', 'Gia đình & Mối quan hệ', 4),
            ('Health', 'health', 'Sức khỏe', 5),
            ('Education & Learning', 'education-learning', 'Giáo dục & Học tập', 6),
            ('Technology', 'technology', 'Công nghệ', 7),
            ('Business & Finance', 'business-finance', 'Kinh doanh & Tài chính', 8),
            ('Science', 'science', 'Khoa học', 9),
            ('Environment & Nature', 'environment-nature', 'Môi trường & Thiên nhiên', 10),
            ('Culture & Society', 'culture-society', 'Văn hóa & Xã hội', 11),
            ('Entertainment', 'entertainment', 'Giải trí', 12),
            ('Sports', 'sports', 'Thể thao', 13),
            ('Food & Restaurants', 'food-restaurants', 'Thực phẩm & Nhà hàng', 14),
            ('Shopping & Services', 'shopping-services', 'Mua sắm & Dịch vụ', 15)
        ) AS t(name, slug, description, sort_order)
    LOOP
        IF NOT EXISTS (SELECT 1 FROM lesson_categories WHERE slug = cat.slug) THEN
            new_id := gen_random_uuid();
            INSERT INTO lesson_categories (id, name, slug, description, sort_order, status, path, is_deleted, created_at, updated_at)
            VALUES (new_id, cat.name, cat.slug, cat.description, cat.sort_order, 'ACTIVE', '/' || new_id || '/', FALSE, NOW(), NOW());
        END IF;
    END LOOP;
END $$;
