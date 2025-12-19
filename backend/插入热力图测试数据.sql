-- 插入测试数据以便热力图显示

-- 1. 先检查是否已有经纬度数据
SELECT 
    COUNT(*) as total_properties,
    COUNT(DISTINCT c.community_id) as communities_with_coords
FROM properties p
INNER JOIN communities c ON p.community_id = c.community_id
WHERE JSON_EXTRACT(c.location_info, '$.latitude') IS NOT NULL;

-- 2. 如果没有数据，插入一些测试小区（深圳地区）
INSERT INTO communities (name, basic_info, location_info, facility_info) VALUES
('测试小区A - 南山科技园', 
 '{"developer": "测试开发商", "build_year": 2018, "total_size": 50000, "total_households": 800}',
 '{"city": "深圳市", "address": "科技园路1号", "district": "南山区", "latitude": 22.541, "province": "广东省", "longitude": 113.940}',
 '{"schools": ["南山小学"], "plot_ratio": 2.5, "green_ratio": 0.35, "management_fee": 3.5, "parking_spaces": 800}'),

('测试小区B - 南山后海', 
 '{"developer": "测试开发商", "build_year": 2019, "total_size": 60000, "total_households": 900}',
 '{"city": "深圳市", "address": "后海大道2号", "district": "南山区", "latitude": 22.522, "province": "广东省", "longitude": 113.935}',
 '{"schools": ["后海小学"], "plot_ratio": 2.8, "green_ratio": 0.38, "management_fee": 4.0, "parking_spaces": 900}'),

('测试小区C - 福田中心区', 
 '{"developer": "测试开发商", "build_year": 2017, "total_size": 70000, "total_households": 1000}',
 '{"city": "深圳市", "address": "福华路3号", "district": "福田区", "latitude": 22.535, "province": "广东省", "longitude": 114.058}',
 '{"schools": ["福田小学"], "plot_ratio": 3.0, "green_ratio": 0.40, "management_fee": 4.5, "parking_spaces": 1000}'),

('测试小区D - 福田莲花', 
 '{"developer": "测试开发商", "build_year": 2016, "total_size": 55000, "total_households": 850}',
 '{"city": "深圳市", "address": "莲花路4号", "district": "福田区", "latitude": 22.550, "province": "广东省", "longitude": 114.065}',
 '{"schools": ["莲花小学"], "plot_ratio": 2.6, "green_ratio": 0.36, "management_fee": 3.8, "parking_spaces": 850}'),

('测试小区E - 罗湖东门', 
 '{"developer": "测试开发商", "build_year": 2015, "total_size": 45000, "total_households": 700}',
 '{"city": "深圳市", "address": "东门路5号", "district": "罗湖区", "latitude": 22.548, "province": "广东省", "longitude": 114.125}',
 '{"schools": ["东门小学"], "plot_ratio": 2.4, "green_ratio": 0.32, "management_fee": 3.2, "parking_spaces": 700}');

-- 3. 为新增的小区添加房源（使用刚插入的小区ID）
-- 获取最新插入的小区ID
SET @community_a = (SELECT community_id FROM communities WHERE name = '测试小区A - 南山科技园' LIMIT 1);
SET @community_b = (SELECT community_id FROM communities WHERE name = '测试小区B - 南山后海' LIMIT 1);
SET @community_c = (SELECT community_id FROM communities WHERE name = '测试小区C - 福田中心区' LIMIT 1);
SET @community_d = (SELECT community_id FROM communities WHERE name = '测试小区D - 福田莲花' LIMIT 1);
SET @community_e = (SELECT community_id FROM communities WHERE name = '测试小区E - 罗湖东门' LIMIT 1);

-- 插入房源数据（每个小区插入多个房源以形成热力效果）
INSERT INTO properties (community_id, title, basic_info, price_info, layout_info, status) VALUES
-- 小区A的房源（高价位）
(@community_a, '南山科技园 精装三房', '{"build_year": 2018, "decoration": "hard"}', '{"unit_price": 95000, "total_price": 855}', '{"area": 90, "bedroom_count": 3, "bathroom_count": 2}', 'for_sale'),
(@community_a, '南山科技园 豪华两房', '{"build_year": 2018, "decoration": "luxury"}', '{"unit_price": 98000, "total_price": 686}', '{"area": 70, "bedroom_count": 2, "bathroom_count": 1}', 'for_sale'),
(@community_a, '南山科技园 大四房', '{"build_year": 2018, "decoration": "hard"}', '{"unit_price": 92000, "total_price": 1242}', '{"area": 135, "bedroom_count": 4, "bathroom_count": 3}', 'for_sale'),

-- 小区B的房源（中高价位）
(@community_b, '后海花园 舒适三房', '{"build_year": 2019, "decoration": "hard"}', '{"unit_price": 88000, "total_price": 792}', '{"area": 90, "bedroom_count": 3, "bathroom_count": 2}', 'for_sale'),
(@community_b, '后海花园 温馨两房', '{"build_year": 2019, "decoration": "simple"}', '{"unit_price": 85000, "total_price": 595}', '{"area": 70, "bedroom_count": 2, "bathroom_count": 1}', 'for_sale'),
(@community_b, '后海花园 高层四房', '{"build_year": 2019, "decoration": "hard"}', '{"unit_price": 90000, "total_price": 1170}', '{"area": 130, "bedroom_count": 4, "bathroom_count": 2}', 'for_sale'),

-- 小区C的房源（超高价位）
(@community_c, '福田中心 顶级三房', '{"build_year": 2017, "decoration": "luxury"}', '{"unit_price": 120000, "total_price": 1080}', '{"area": 90, "bedroom_count": 3, "bathroom_count": 2}', 'for_sale'),
(@community_c, '福田中心 精品两房', '{"build_year": 2017, "decoration": "luxury"}', '{"unit_price": 118000, "total_price": 826}', '{"area": 70, "bedroom_count": 2, "bathroom_count": 1}', 'for_sale'),
(@community_c, '福田中心 豪宅四房', '{"build_year": 2017, "decoration": "luxury"}', '{"unit_price": 125000, "total_price": 1750}', '{"area": 140, "bedroom_count": 4, "bathroom_count": 3}', 'for_sale'),

-- 小区D的房源（中价位）
(@community_d, '莲花公馆 普通三房', '{"build_year": 2016, "decoration": "simple"}', '{"unit_price": 75000, "total_price": 675}', '{"area": 90, "bedroom_count": 3, "bathroom_count": 2}', 'for_sale'),
(@community_d, '莲花公馆 小两房', '{"build_year": 2016, "decoration": "simple"}', '{"unit_price": 72000, "total_price": 504}', '{"area": 70, "bedroom_count": 2, "bathroom_count": 1}', 'for_sale'),
(@community_d, '莲花公馆 大三房', '{"build_year": 2016, "decoration": "hard"}', '{"unit_price": 78000, "total_price": 858}', '{"area": 110, "bedroom_count": 3, "bathroom_count": 2}', 'for_sale'),

-- 小区E的房源（中低价位）
(@community_e, '东门广场 老三房', '{"build_year": 2015, "decoration": "simple"}', '{"unit_price": 65000, "total_price": 585}', '{"area": 90, "bedroom_count": 3, "bathroom_count": 2}', 'for_sale'),
(@community_e, '东门广场 实惠两房', '{"build_year": 2015, "decoration": "simple"}', '{"unit_price": 62000, "total_price": 434}', '{"area": 70, "bedroom_count": 2, "bathroom_count": 1}', 'for_sale'),
(@community_e, '东门广场 经典三房', '{"build_year": 2015, "decoration": "hard"}', '{"unit_price": 68000, "total_price": 748}', '{"area": 110, "bedroom_count": 3, "bathroom_count": 2}', 'for_sale');

-- 4. 验证数据
SELECT 
    c.name as community_name,
    COUNT(p.property_id) as property_count,
    JSON_UNQUOTE(JSON_EXTRACT(c.location_info, '$.latitude')) as latitude,
    JSON_UNQUOTE(JSON_EXTRACT(c.location_info, '$.longitude')) as longitude,
    AVG(CAST(JSON_UNQUOTE(JSON_EXTRACT(p.price_info, '$.unit_price')) AS UNSIGNED)) as avg_price
FROM communities c
LEFT JOIN properties p ON c.community_id = p.community_id
WHERE JSON_EXTRACT(c.location_info, '$.latitude') IS NOT NULL
GROUP BY c.community_id, c.name
ORDER BY c.community_id DESC
LIMIT 10;

-- 5. 测试热力图API的SQL查询
SELECT 
    p.property_id, 
    p.title as property_name,
    JSON_UNQUOTE(JSON_EXTRACT(c.location_info, '$.district')) as district,
    JSON_UNQUOTE(JSON_EXTRACT(p.price_info, '$.unit_price')) as unit_price,
    JSON_UNQUOTE(JSON_EXTRACT(c.location_info, '$.latitude')) as latitude,
    JSON_UNQUOTE(JSON_EXTRACT(c.location_info, '$.longitude')) as longitude
FROM properties p
INNER JOIN communities c ON p.community_id = c.community_id
WHERE JSON_EXTRACT(c.location_info, '$.latitude') IS NOT NULL
  AND JSON_EXTRACT(c.location_info, '$.longitude') IS NOT NULL
LIMIT 20;
