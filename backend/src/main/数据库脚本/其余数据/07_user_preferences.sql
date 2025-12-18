-- ============================================
-- 用户偏好表数据注入脚本
-- ============================================

USE `bigdata`;

-- 为新增用户生成偏好数据
INSERT INTO `user_preferences` (`user_id`, `preference_data`, `created_at`, `updated_at`) VALUES
(4, '{"keywords": ["地铁", "精装"], "locations": ["南山区", "罗湖区"], "area_range": {"max": 100, "min": 70}, "decorations": ["hard", "luxury"], "house_types": ["apartment"], "price_range": {"max": 700, "min": 400}, "orientations": ["south", "southeast"], "bedroom_range": {"max": 3, "min": 2}}', NOW(), NOW()),
(5, '{"keywords": ["学区", "大户型"], "locations": ["福田区", "南山区"], "area_range": {"max": 150, "min": 100}, "decorations": ["luxury"], "house_types": ["apartment"], "price_range": {"max": 1000, "min": 600}, "orientations": ["south"], "bedroom_range": {"max": 4, "min": 3}}', NOW(), NOW()),
(6, '{"keywords": ["豪华", "海景", "高端"], "locations": ["南山区"], "area_range": {"max": 180, "min": 120}, "decorations": ["luxury"], "house_types": ["apartment"], "price_range": {"max": 2000, "min": 800}, "orientations": ["south"], "bedroom_range": {"max": 5, "min": 3}}', NOW(), NOW()),
(7, '{"keywords": ["便宜", "小户型"], "locations": ["宝安区", "龙岗区"], "area_range": {"max": 70, "min": 50}, "decorations": ["simple", "hard"], "house_types": ["apartment"], "price_range": {"max": 400, "min": 200}, "orientations": ["south", "east"], "bedroom_range": {"max": 2, "min": 1}}', NOW(), NOW()),
(8, '{"keywords": ["中心区", "地铁"], "locations": ["罗湖区", "福田区"], "area_range": {"max": 110, "min": 75}, "decorations": ["hard"], "house_types": ["apartment"], "price_range": {"max": 800, "min": 500}, "orientations": ["south", "southeast"], "bedroom_range": {"max": 3, "min": 2}}', NOW(), NOW()),
(9, '{"keywords": ["学区", "大户型", "新房"], "locations": ["南山区", "福田区"], "area_range": {"max": 160, "min": 110}, "decorations": ["luxury", "hard"], "house_types": ["apartment"], "price_range": {"max": 1200, "min": 600}, "orientations": ["south"], "bedroom_range": {"max": 4, "min": 3}}', NOW(), NOW()),
(10, '{"keywords": ["性价比", "便宜"], "locations": ["龙岗区", "宝安区"], "area_range": {"max": 90, "min": 60}, "decorations": ["simple", "hard"], "house_types": ["apartment"], "price_range": {"max": 500, "min": 250}, "orientations": ["south", "east", "southeast"], "bedroom_range": {"max": 3, "min": 2}}', NOW(), NOW());

