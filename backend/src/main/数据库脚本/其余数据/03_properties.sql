-- ============================================
-- 房源表数据注入脚本
-- ============================================

USE `bigdata`;

-- 扩展房源数据（假设已有9个房源，从property_id=10开始）
-- 注意：community_id需要根据实际插入的小区ID调整

INSERT INTO `properties` (`community_id`, `title`, `basic_info`, `price_info`, `layout_info`, `status`, `view_count`, `favorite_count`, `created_at`, `updated_at`) VALUES
-- 保利香槟国际的房源
(7, '保利香槟国际 精装三房 南向 地铁口', '{"build_year": 2016, "decoration": "hard", "property_type": "apartment"}', '{"unit_price": 88000, "total_price": 704.0, "price_history": [{"date": "2024-01-01", "price": 720}]}', '{"area": 80.0, "floor": 12, "orientation": "south", "total_floors": 30, "bedroom_count": 3, "bathroom_count": 2, "living_room_count": 2}', 'for_sale', 15, 3, NOW(), NOW()),
(7, '保利香槟国际 豪华四房 南北通透', '{"build_year": 2016, "decoration": "luxury", "property_type": "apartment"}', '{"unit_price": 92000, "total_price": 1104.0, "price_history": [{"date": "2024-01-01", "price": 1150}]}', '{"area": 120.0, "floor": 18, "orientation": "south", "total_floors": 30, "bedroom_count": 4, "bathroom_count": 3, "living_room_count": 2}', 'for_sale', 28, 8, NOW(), NOW()),
(7, '保利香槟国际 两房 精装修 地铁口', '{"build_year": 2016, "decoration": "hard", "property_type": "apartment"}', '{"unit_price": 85000, "total_price": 510.0, "price_history": [{"date": "2024-02-01", "price": 530}]}', '{"area": 60.0, "floor": 8, "orientation": "southeast", "total_floors": 30, "bedroom_count": 2, "bathroom_count": 1, "living_room_count": 1}', 'for_sale', 22, 5, NOW(), NOW()),

-- 招商海月花园的房源
(8, '招商海月花园 三房两厅 南向 学区房', '{"build_year": 2014, "decoration": "hard", "property_type": "apartment"}', '{"unit_price": 82000, "total_price": 656.0, "price_history": [{"date": "2024-01-01", "price": 680}]}', '{"area": 80.0, "floor": 15, "orientation": "south", "total_floors": 25, "bedroom_count": 3, "bathroom_count": 2, "living_room_count": 2}', 'for_sale', 35, 12, NOW(), NOW()),
(8, '招商海月花园 四房 豪华装修 海景', '{"build_year": 2014, "decoration": "luxury", "property_type": "apartment"}', '{"unit_price": 95000, "total_price": 1140.0, "price_history": [{"date": "2024-01-01", "price": 1200}]}', '{"area": 120.0, "floor": 22, "orientation": "south", "total_floors": 25, "bedroom_count": 4, "bathroom_count": 3, "living_room_count": 2}', 'for_sale', 42, 15, NOW(), NOW()),

-- 中海翠景花园的房源
(9, '中海翠景花园 精装三房 中心区', '{"build_year": 2017, "decoration": "hard", "property_type": "apartment"}', '{"unit_price": 90000, "total_price": 720.0, "price_history": [{"date": "2024-01-01", "price": 750}]}', '{"area": 80.0, "floor": 20, "orientation": "south", "total_floors": 32, "bedroom_count": 3, "bathroom_count": 2, "living_room_count": 2}', 'for_sale', 18, 4, NOW(), NOW()),
(9, '中海翠景花园 两房 地铁口 精装', '{"build_year": 2017, "decoration": "hard", "property_type": "apartment"}', '{"unit_price": 88000, "total_price": 528.0, "price_history": [{"date": "2024-02-01", "price": 550}]}', '{"area": 60.0, "floor": 10, "orientation": "southeast", "total_floors": 32, "bedroom_count": 2, "bathroom_count": 1, "living_room_count": 1}', 'for_sale', 25, 7, NOW(), NOW()),

-- 金地梅陇镇的房源
(10, '金地梅陇镇 三房 精装修 地铁口', '{"build_year": 2013, "decoration": "hard", "property_type": "apartment"}', '{"unit_price": 65000, "total_price": 520.0, "price_history": [{"date": "2024-01-01", "price": 540}]}', '{"area": 80.0, "floor": 12, "orientation": "south", "total_floors": 28, "bedroom_count": 3, "bathroom_count": 2, "living_room_count": 2}', 'for_sale', 30, 9, NOW(), NOW()),
(10, '金地梅陇镇 四房 大户型 学区', '{"build_year": 2013, "decoration": "hard", "property_type": "apartment"}', '{"unit_price": 68000, "total_price": 816.0, "price_history": [{"date": "2024-01-01", "price": 850}]}', '{"area": 120.0, "floor": 18, "orientation": "south", "total_floors": 28, "bedroom_count": 4, "bathroom_count": 3, "living_room_count": 2}', 'for_sale', 20, 6, NOW(), NOW()),

-- 绿景蓝湾半岛的房源
(11, '绿景蓝湾半岛 精装三房 南向 海景', '{"build_year": 2019, "decoration": "luxury", "property_type": "apartment"}', '{"unit_price": 95000, "total_price": 760.0, "price_history": [{"date": "2024-01-01", "price": 780}]}', '{"area": 80.0, "floor": 25, "orientation": "south", "total_floors": 35, "bedroom_count": 3, "bathroom_count": 2, "living_room_count": 2}', 'for_sale', 45, 18, NOW(), NOW()),
(11, '绿景蓝湾半岛 豪华四房 南北通透', '{"build_year": 2019, "decoration": "luxury", "property_type": "apartment"}', '{"unit_price": 100000, "total_price": 1200.0, "price_history": [{"date": "2024-01-01", "price": 1250}]}', '{"area": 120.0, "floor": 30, "orientation": "south", "total_floors": 35, "bedroom_count": 4, "bathroom_count": 3, "living_room_count": 2}', 'for_sale', 38, 14, NOW(), NOW()),

-- 龙光玖龙玺的房源
(12, '龙光玖龙玺 精装三房 地铁口', '{"build_year": 2020, "decoration": "luxury", "property_type": "apartment"}', '{"unit_price": 72000, "total_price": 576.0, "price_history": [{"date": "2024-01-01", "price": 600}]}', '{"area": 80.0, "floor": 15, "orientation": "south", "total_floors": 33, "bedroom_count": 3, "bathroom_count": 2, "living_room_count": 2}', 'for_sale', 32, 10, NOW(), NOW()),
(12, '龙光玖龙玺 两房 精装修 新房', '{"build_year": 2020, "decoration": "luxury", "property_type": "apartment"}', '{"unit_price": 70000, "total_price": 420.0, "price_history": [{"date": "2024-02-01", "price": 440}]}', '{"area": 60.0, "floor": 8, "orientation": "southeast", "total_floors": 33, "bedroom_count": 2, "bathroom_count": 1, "living_room_count": 1}', 'for_sale', 28, 8, NOW(), NOW()),

-- 佳兆业城市广场的房源
(13, '佳兆业城市广场 三房 精装修', '{"build_year": 2015, "decoration": "hard", "property_type": "apartment"}', '{"unit_price": 58000, "total_price": 464.0, "price_history": [{"date": "2024-01-01", "price": 480}]}', '{"area": 80.0, "floor": 16, "orientation": "south", "total_floors": 30, "bedroom_count": 3, "bathroom_count": 2, "living_room_count": 2}', 'for_sale', 15, 3, NOW(), NOW()),
(13, '佳兆业城市广场 四房 大户型', '{"build_year": 2015, "decoration": "hard", "property_type": "apartment"}', '{"unit_price": 60000, "total_price": 720.0, "price_history": [{"date": "2024-01-01", "price": 750}]}', '{"area": 120.0, "floor": 20, "orientation": "south", "total_floors": 30, "bedroom_count": 4, "bathroom_count": 3, "living_room_count": 2}', 'for_sale', 12, 2, NOW(), NOW()),

-- 星河湾的房源
(14, '星河湾 精装三房 中心区', '{"build_year": 2012, "decoration": "hard", "property_type": "apartment"}', '{"unit_price": 75000, "total_price": 600.0, "price_history": [{"date": "2024-01-01", "price": 620}]}', '{"area": 80.0, "floor": 14, "orientation": "south", "total_floors": 26, "bedroom_count": 3, "bathroom_count": 2, "living_room_count": 2}', 'for_sale', 22, 5, NOW(), NOW()),
(14, '星河湾 两房 地铁口 精装', '{"build_year": 2012, "decoration": "hard", "property_type": "apartment"}', '{"unit_price": 73000, "total_price": 438.0, "price_history": [{"date": "2024-02-01", "price": 450}]}', '{"area": 60.0, "floor": 10, "orientation": "southeast", "total_floors": 26, "bedroom_count": 2, "bathroom_count": 1, "living_room_count": 1}', 'for_sale', 18, 4, NOW(), NOW());

