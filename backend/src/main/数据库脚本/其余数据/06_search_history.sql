-- ============================================
-- 搜索历史表数据注入脚本
-- ============================================

USE `bigdata`;

-- 为不同用户生成搜索历史记录
INSERT INTO `search_history` (`user_id`, `search_data`, `created_at`) VALUES
-- 用户1的搜索记录
(1, '{"keywords": ["三房", "南山区"], "filters": {"price_min": 300, "price_max": 600, "area_min": 80, "area_max": 120, "bedroom_count": 3}}', '2024-11-28 10:00:00'),
(1, '{"keywords": ["地铁", "学区"], "filters": {"price_min": 400, "price_max": 700, "district": "南山区"}}', '2024-11-27 09:00:00'),
(1, '{"keywords": ["精装修"], "filters": {"decoration": "hard", "price_min": 500, "price_max": 800}}', '2024-11-26 14:00:00'),

-- 用户2的搜索记录
(2, '{"keywords": ["四房", "学区房"], "filters": {"price_min": 600, "price_max": 1000, "area_min": 100, "bedroom_count": 4}}', '2024-11-25 15:00:00'),
(2, '{"keywords": ["大户型", "福田区"], "filters": {"price_min": 700, "price_max": 1200, "area_min": 120}}', '2024-11-24 16:00:00'),
(2, '{"keywords": ["地铁口"], "filters": {"price_min": 500, "price_max": 900}}', '2024-11-23 10:00:00'),

-- 用户3的搜索记录
(3, '{"keywords": ["两房", "宝安区"], "filters": {"price_min": 200, "price_max": 400, "bedroom_count": 2}}', '2024-11-30 12:00:00'),
(3, '{"keywords": ["小户型"], "filters": {"price_min": 150, "price_max": 350, "area_max": 70}}', '2024-11-29 08:00:00'),
(3, '{"keywords": ["便宜"], "filters": {"price_max": 300}}', '2024-11-28 14:00:00'),

-- 用户4的搜索记录
(4, '{"keywords": ["三房", "精装修"], "filters": {"price_min": 400, "price_max": 700, "decoration": "hard", "bedroom_count": 3}}', '2024-11-26 11:00:00'),
(4, '{"keywords": ["南山区", "地铁"], "filters": {"price_min": 500, "price_max": 800, "district": "南山区"}}', '2024-11-25 15:00:00'),
(4, '{"keywords": ["新房"], "filters": {"build_year_min": 2015}}', '2024-11-24 10:00:00'),

-- 用户5的搜索记录
(5, '{"keywords": ["四房", "大户型"], "filters": {"price_min": 600, "price_max": 1000, "area_min": 120, "bedroom_count": 4}}', '2024-11-25 13:00:00'),
(5, '{"keywords": ["学区房", "福田区"], "filters": {"price_min": 700, "price_max": 1100, "district": "福田区"}}', '2024-11-24 14:00:00'),
(5, '{"keywords": ["豪华装修"], "filters": {"decoration": "luxury", "price_min": 800}}', '2024-11-23 11:00:00'),

-- 用户6的搜索记录
(6, '{"keywords": ["豪华", "海景"], "filters": {"price_min": 800, "price_max": 1500, "decoration": "luxury"}}', '2024-11-24 09:00:00'),
(6, '{"keywords": ["南山区", "高端"], "filters": {"price_min": 1000, "price_max": 2000, "district": "南山区"}}', '2024-11-23 15:00:00'),
(6, '{"keywords": ["新房", "精装"], "filters": {"build_year_min": 2018, "decoration": "luxury"}}', '2024-11-22 10:00:00'),

-- 用户7的搜索记录
(7, '{"keywords": ["两房", "地铁口"], "filters": {"price_min": 300, "price_max": 500, "bedroom_count": 2}}', '2024-11-28 10:00:00'),
(7, '{"keywords": ["宝安区", "便宜"], "filters": {"price_max": 400, "district": "宝安区"}}', '2024-11-27 14:00:00'),
(7, '{"keywords": ["小户型"], "filters": {"area_max": 70, "price_max": 350}}', '2024-11-26 09:00:00'),

-- 用户8的搜索记录
(8, '{"keywords": ["三房", "中心区"], "filters": {"price_min": 500, "price_max": 800, "bedroom_count": 3}}', '2024-11-26 13:00:00'),
(8, '{"keywords": ["罗湖区"], "filters": {"price_min": 400, "price_max": 700, "district": "罗湖区"}}', '2024-11-25 16:00:00'),
(8, '{"keywords": ["精装修"], "filters": {"decoration": "hard", "price_min": 450}}', '2024-11-24 10:00:00'),

-- 用户9的搜索记录
(9, '{"keywords": ["四房", "学区"], "filters": {"price_min": 600, "price_max": 1000, "bedroom_count": 4}}', '2024-11-25 12:00:00'),
(9, '{"keywords": ["大户型", "南山区"], "filters": {"price_min": 700, "price_max": 1200, "area_min": 120, "district": "南山区"}}', '2024-11-24 15:00:00'),
(9, '{"keywords": ["新房"], "filters": {"build_year_min": 2016}}', '2024-11-23 11:00:00'),

-- 用户10的搜索记录
(10, '{"keywords": ["两房", "便宜"], "filters": {"price_max": 400, "bedroom_count": 2}}', '2024-11-28 14:00:00'),
(10, '{"keywords": ["龙岗区"], "filters": {"price_min": 200, "price_max": 500, "district": "龙岗区"}}', '2024-11-27 17:00:00'),
(10, '{"keywords": ["性价比"], "filters": {"price_min": 250, "price_max": 450}}', '2024-11-26 10:00:00');

