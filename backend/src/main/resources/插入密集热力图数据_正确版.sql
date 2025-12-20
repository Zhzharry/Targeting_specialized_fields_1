-- 插入密集热力图测试数据
-- 基于实际表结构：communities有name字段，properties有title字段

-- ========== 福田区-高价区（10-12万） ==========
INSERT INTO communities (name, basic_info, location_info, facility_info) VALUES
('福田中心花园', 
 '{"developer": "测试开发商", "build_year": 2020, "total_size": 80000, "total_households": 1200}',
 '{"city": "深圳市", "address": "福华路188号", "district": "福田区", "latitude": 22.5420, "province": "广东省", "longitude": 114.0570}',
 '{"schools": ["福田外国语学校"], "plot_ratio": 3.0, "green_ratio": 0.40, "management_fee": 5.0, "parking_spaces": 1200}'),

('卓越世纪中心', 
 '{"developer": "卓越集团", "build_year": 2019, "total_size": 100000, "total_households": 1500}',
 '{"city": "深圳市", "address": "福华三路", "district": "福田区", "latitude": 22.5390, "province": "广东省", "longitude": 114.0540}',
 '{"schools": ["福田中学"], "plot_ratio": 3.2, "green_ratio": 0.38, "management_fee": 5.5, "parking_spaces": 1500}'),

('福田印象城', 
 '{"developer": "测试开发商", "build_year": 2021, "total_size": 75000, "total_households": 1100}',
 '{"city": "深圳市", "address": "深南大道", "district": "福田区", "latitude": 22.5410, "province": "广东省", "longitude": 114.0560}',
 '{"schools": ["福田小学"], "plot_ratio": 2.9, "green_ratio": 0.42, "management_fee": 4.8, "parking_spaces": 1100}');

-- 获取小区ID并插入房源
SET @fukuda1 = (SELECT community_id FROM communities WHERE name = '福田中心花园' LIMIT 1);
SET @fukuda2 = (SELECT community_id FROM communities WHERE name = '卓越世纪中心' LIMIT 1);
SET @fukuda3 = (SELECT community_id FROM communities WHERE name = '福田印象城' LIMIT 1);

INSERT INTO properties (community_id, title, basic_info, price_info, layout_info, status) VALUES
-- 福田中心花园房源（10-11万）
(@fukuda1, '福田中心花园 精装三房A', '{"build_year": 2020, "decoration": "hard"}', '{"unit_price": 105000, "total_price": 945}', '{"area": 90, "bedroom_count": 3, "bathroom_count": 2}', 'for_sale'),
(@fukuda1, '福田中心花园 精装三房B', '{"build_year": 2020, "decoration": "hard"}', '{"unit_price": 108000, "total_price": 972}', '{"area": 90, "bedroom_count": 3, "bathroom_count": 2}', 'for_sale'),
(@fukuda1, '福田中心花园 豪华两房A', '{"build_year": 2020, "decoration": "luxury"}', '{"unit_price": 110000, "total_price": 770}', '{"area": 70, "bedroom_count": 2, "bathroom_count": 2}', 'for_sale'),
(@fukuda1, '福田中心花园 豪华两房B', '{"build_year": 2020, "decoration": "luxury"}', '{"unit_price": 112000, "total_price": 784}', '{"area": 70, "bedroom_count": 2, "bathroom_count": 2}', 'for_sale'),
(@fukuda1, '福田中心花园 四房A', '{"build_year": 2020, "decoration": "hard"}', '{"unit_price": 106000, "total_price": 1378}', '{"area": 130, "bedroom_count": 4, "bathroom_count": 3}', 'for_sale'),

-- 卓越世纪中心房源（11-12万）
(@fukuda2, '卓越世纪 顶级三房A', '{"build_year": 2019, "decoration": "luxury"}', '{"unit_price": 118000, "total_price": 1062}', '{"area": 90, "bedroom_count": 3, "bathroom_count": 2}', 'for_sale'),
(@fukuda2, '卓越世纪 顶级三房B', '{"build_year": 2019, "decoration": "luxury"}', '{"unit_price": 120000, "total_price": 1080}', '{"area": 90, "bedroom_count": 3, "bathroom_count": 2}', 'for_sale'),
(@fukuda2, '卓越世纪 精品两房A', '{"build_year": 2019, "decoration": "luxury"}', '{"unit_price": 122000, "total_price": 854}', '{"area": 70, "bedroom_count": 2, "bathroom_count": 2}', 'for_sale'),
(@fukuda2, '卓越世纪 精品两房B', '{"build_year": 2019, "decoration": "luxury"}', '{"unit_price": 125000, "total_price": 875}', '{"area": 70, "bedroom_count": 2, "bathroom_count": 2}', 'for_sale'),
(@fukuda2, '卓越世纪 豪宅四房', '{"build_year": 2019, "decoration": "luxury"}', '{"unit_price": 115000, "total_price": 1495}', '{"area": 130, "bedroom_count": 4, "bathroom_count": 3}', 'for_sale'),

-- 福田印象城房源（10-11万）
(@fukuda3, '福田印象 精装三房A', '{"build_year": 2021, "decoration": "hard"}', '{"unit_price": 102000, "total_price": 918}', '{"area": 90, "bedroom_count": 3, "bathroom_count": 2}', 'for_sale'),
(@fukuda3, '福田印象 精装三房B', '{"build_year": 2021, "decoration": "hard"}', '{"unit_price": 105000, "total_price": 945}', '{"area": 90, "bedroom_count": 3, "bathroom_count": 2}', 'for_sale'),
(@fukuda3, '福田印象 温馨两房A', '{"build_year": 2021, "decoration": "hard"}', '{"unit_price": 108000, "total_price": 756}', '{"area": 70, "bedroom_count": 2, "bathroom_count": 2}', 'for_sale'),
(@fukuda3, '福田印象 温馨两房B', '{"build_year": 2021, "decoration": "hard"}', '{"unit_price": 110000, "total_price": 770}', '{"area": 70, "bedroom_count": 2, "bathroom_count": 2}', 'for_sale');

-- ========== 南山区-中高价区（8-10万） ==========
INSERT INTO communities (name, basic_info, location_info, facility_info) VALUES
('科技园国际', 
 '{"developer": "万科集团", "build_year": 2018, "total_size": 90000, "total_households": 1300}',
 '{"city": "深圳市", "address": "科技园路", "district": "南山区", "latitude": 22.5350, "province": "广东省", "longitude": 113.9520}',
 '{"schools": ["南山实验学校"], "plot_ratio": 2.8, "green_ratio": 0.40, "management_fee": 4.5, "parking_spaces": 1300}'),

('后海湾花园', 
 '{"developer": "测试开发商", "build_year": 2019, "total_size": 85000, "total_households": 1200}',
 '{"city": "深圳市", "address": "后海大道", "district": "南山区", "latitude": 22.5200, "province": "广东省", "longitude": 113.9350}',
 '{"schools": ["后海小学"], "plot_ratio": 2.9, "green_ratio": 0.38, "management_fee": 4.2, "parking_spaces": 1200}'),

('高新科技园', 
 '{"developer": "测试开发商", "build_year": 2020, "total_size": 95000, "total_households": 1400}',
 '{"city": "深圳市", "address": "高新南路", "district": "南山区", "latitude": 22.5370, "province": "广东省", "longitude": 113.9540}',
 '{"schools": ["南山中学"], "plot_ratio": 3.0, "green_ratio": 0.36, "management_fee": 4.8, "parking_spaces": 1400}');

SET @nanshan1 = (SELECT community_id FROM communities WHERE name = '科技园国际' LIMIT 1);
SET @nanshan2 = (SELECT community_id FROM communities WHERE name = '后海湾花园' LIMIT 1);
SET @nanshan3 = (SELECT community_id FROM communities WHERE name = '高新科技园' LIMIT 1);

INSERT INTO properties (community_id, title, basic_info, price_info, layout_info, status) VALUES
-- 科技园国际房源（9-10万）
(@nanshan1, '科技园国际 精装三房A', '{"build_year": 2018, "decoration": "hard"}', '{"unit_price": 90000, "total_price": 810}', '{"area": 90, "bedroom_count": 3, "bathroom_count": 2}', 'for_sale'),
(@nanshan1, '科技园国际 精装三房B', '{"build_year": 2018, "decoration": "hard"}', '{"unit_price": 92000, "total_price": 828}', '{"area": 90, "bedroom_count": 3, "bathroom_count": 2}', 'for_sale'),
(@nanshan1, '科技园国际 舒适两房A', '{"build_year": 2018, "decoration": "hard"}', '{"unit_price": 95000, "total_price": 665}', '{"area": 70, "bedroom_count": 2, "bathroom_count": 2}', 'for_sale'),
(@nanshan1, '科技园国际 舒适两房B', '{"build_year": 2018, "decoration": "hard"}', '{"unit_price": 88000, "total_price": 616}', '{"area": 70, "bedroom_count": 2, "bathroom_count": 2}', 'for_sale'),

-- 后海湾花园房源（8-9万）
(@nanshan2, '后海湾 温馨三房A', '{"build_year": 2019, "decoration": "hard"}', '{"unit_price": 82000, "total_price": 738}', '{"area": 90, "bedroom_count": 3, "bathroom_count": 2}', 'for_sale'),
(@nanshan2, '后海湾 温馨三房B', '{"build_year": 2019, "decoration": "hard"}', '{"unit_price": 85000, "total_price": 765}', '{"area": 90, "bedroom_count": 3, "bathroom_count": 2}', 'for_sale'),
(@nanshan2, '后海湾 实用两房A', '{"build_year": 2019, "decoration": "simple"}', '{"unit_price": 78000, "total_price": 546}', '{"area": 70, "bedroom_count": 2, "bathroom_count": 1}', 'for_sale'),
(@nanshan2, '后海湾 实用两房B', '{"build_year": 2019, "decoration": "simple"}', '{"unit_price": 80000, "total_price": 560}', '{"area": 70, "bedroom_count": 2, "bathroom_count": 1}', 'for_sale'),

-- 高新科技园房源（9-10万）
(@nanshan3, '高新园 精装三房A', '{"build_year": 2020, "decoration": "hard"}', '{"unit_price": 91000, "total_price": 819}', '{"area": 90, "bedroom_count": 3, "bathroom_count": 2}', 'for_sale'),
(@nanshan3, '高新园 精装三房B', '{"build_year": 2020, "decoration": "hard"}', '{"unit_price": 93000, "total_price": 837}', '{"area": 90, "bedroom_count": 3, "bathroom_count": 2}', 'for_sale'),
(@nanshan3, '高新园 豪华两房A', '{"build_year": 2020, "decoration": "luxury"}', '{"unit_price": 96000, "total_price": 672}', '{"area": 70, "bedroom_count": 2, "bathroom_count": 2}', 'for_sale');

-- ========== 宝安区-中等价区（6-8万） ==========
INSERT INTO communities (name, basic_info, location_info, facility_info) VALUES
('宝安中心', 
 '{"developer": "测试开发商", "build_year": 2017, "total_size": 70000, "total_households": 1000}',
 '{"city": "深圳市", "address": "宝安大道", "district": "宝安区", "latitude": 22.5550, "province": "广东省", "longitude": 113.8850}',
 '{"schools": ["宝安小学"], "plot_ratio": 2.5, "green_ratio": 0.35, "management_fee": 3.5, "parking_spaces": 1000}'),

('西乡花园', 
 '{"developer": "测试开发商", "build_year": 2018, "total_size": 65000, "total_households": 900}',
 '{"city": "深圳市", "address": "西乡大道", "district": "宝安区", "latitude": 22.5700, "province": "广东省", "longitude": 113.8700}',
 '{"schools": ["西乡小学"], "plot_ratio": 2.6, "green_ratio": 0.36, "management_fee": 3.2, "parking_spaces": 900}');

SET @baoan1 = (SELECT community_id FROM communities WHERE name = '宝安中心' LIMIT 1);
SET @baoan2 = (SELECT community_id FROM communities WHERE name = '西乡花园' LIMIT 1);

INSERT INTO properties (community_id, title, basic_info, price_info, layout_info, status) VALUES
-- 宝安中心房源（6.5-7.5万）
(@baoan1, '宝安中心 精装三房A', '{"build_year": 2017, "decoration": "hard"}', '{"unit_price": 70000, "total_price": 630}', '{"area": 90, "bedroom_count": 3, "bathroom_count": 2}', 'for_sale'),
(@baoan1, '宝安中心 精装三房B', '{"build_year": 2017, "decoration": "hard"}', '{"unit_price": 72000, "total_price": 648}', '{"area": 90, "bedroom_count": 3, "bathroom_count": 2}', 'for_sale'),
(@baoan1, '宝安中心 温馨两房A', '{"build_year": 2017, "decoration": "simple"}', '{"unit_price": 68000, "total_price": 476}', '{"area": 70, "bedroom_count": 2, "bathroom_count": 1}', 'for_sale'),
(@baoan1, '宝安中心 温馨两房B', '{"build_year": 2017, "decoration": "simple"}', '{"unit_price": 69000, "total_price": 483}', '{"area": 70, "bedroom_count": 2, "bathroom_count": 1}', 'for_sale'),

-- 西乡花园房源（6-7万）
(@baoan2, '西乡花园 实用三房A', '{"build_year": 2018, "decoration": "simple"}', '{"unit_price": 62000, "total_price": 558}', '{"area": 90, "bedroom_count": 3, "bathroom_count": 2}', 'for_sale'),
(@baoan2, '西乡花园 实用三房B', '{"build_year": 2018, "decoration": "simple"}', '{"unit_price": 64000, "total_price": 576}', '{"area": 90, "bedroom_count": 3, "bathroom_count": 2}', 'for_sale'),
(@baoan2, '西乡花园 经济两房A', '{"build_year": 2018, "decoration": "simple"}', '{"unit_price": 60000, "total_price": 420}', '{"area": 70, "bedroom_count": 2, "bathroom_count": 1}', 'for_sale'),
(@baoan2, '西乡花园 经济两房B', '{"build_year": 2018, "decoration": "simple"}', '{"unit_price": 61000, "total_price": 427}', '{"area": 70, "bedroom_count": 2, "bathroom_count": 1}', 'for_sale');

-- ========== 龙华区-中低价区（5-6.5万） ==========
INSERT INTO communities (name, basic_info, location_info, facility_info) VALUES
('龙华中心城', 
 '{"developer": "测试开发商", "build_year": 2019, "total_size": 60000, "total_households": 850}',
 '{"city": "深圳市", "address": "龙华大道", "district": "龙华区", "latitude": 22.6500, "province": "广东省", "longitude": 114.0300}',
 '{"schools": ["龙华小学"], "plot_ratio": 2.4, "green_ratio": 0.34, "management_fee": 3.0, "parking_spaces": 850}'),

('民治新城', 
 '{"developer": "测试开发商", "build_year": 2020, "total_size": 55000, "total_households": 800}',
 '{"city": "深圳市", "address": "民治大道", "district": "龙华区", "latitude": 22.6150, "province": "广东省", "longitude": 114.0380}',
 '{"schools": ["民治小学"], "plot_ratio": 2.5, "green_ratio": 0.32, "management_fee": 2.8, "parking_spaces": 800}');

SET @longhua1 = (SELECT community_id FROM communities WHERE name = '龙华中心城' LIMIT 1);
SET @longhua2 = (SELECT community_id FROM communities WHERE name = '民治新城' LIMIT 1);

INSERT INTO properties (community_id, title, basic_info, price_info, layout_info, status) VALUES
-- 龙华中心城房源（5.5-6.5万）
(@longhua1, '龙华中心 精装三房A', '{"build_year": 2019, "decoration": "hard"}', '{"unit_price": 60000, "total_price": 540}', '{"area": 90, "bedroom_count": 3, "bathroom_count": 2}', 'for_sale'),
(@longhua1, '龙华中心 精装三房B', '{"build_year": 2019, "decoration": "hard"}', '{"unit_price": 62000, "total_price": 558}', '{"area": 90, "bedroom_count": 3, "bathroom_count": 2}', 'for_sale'),
(@longhua1, '龙华中心 温馨两房A', '{"build_year": 2019, "decoration": "simple"}', '{"unit_price": 58000, "total_price": 406}', '{"area": 70, "bedroom_count": 2, "bathroom_count": 1}', 'for_sale'),
(@longhua1, '龙华中心 温馨两房B', '{"build_year": 2019, "decoration": "simple"}', '{"unit_price": 59000, "total_price": 413}', '{"area": 70, "bedroom_count": 2, "bathroom_count": 1}', 'for_sale'),

-- 民治新城房源（5-6万）
(@longhua2, '民治新城 经济三房A', '{"build_year": 2020, "decoration": "simple"}', '{"unit_price": 52000, "total_price": 468}', '{"area": 90, "bedroom_count": 3, "bathroom_count": 2}', 'for_sale'),
(@longhua2, '民治新城 经济三房B', '{"build_year": 2020, "decoration": "simple"}', '{"unit_price": 54000, "total_price": 486}', '{"area": 90, "bedroom_count": 3, "bathroom_count": 2}', 'for_sale'),
(@longhua2, '民治新城 实惠两房A', '{"build_year": 2020, "decoration": "simple"}', '{"unit_price": 50000, "total_price": 350}', '{"area": 70, "bedroom_count": 2, "bathroom_count": 1}', 'for_sale'),
(@longhua2, '民治新城 实惠两房B', '{"build_year": 2020, "decoration": "simple"}', '{"unit_price": 51000, "total_price": 357}', '{"area": 70, "bedroom_count": 2, "bathroom_count": 1}', 'for_sale');

-- 统计
SELECT '数据插入完成！' AS status;
SELECT COUNT(*) AS '新增小区数量' FROM communities WHERE name LIKE '福田%' OR name LIKE '科技%' OR name LIKE '卓越%' OR name LIKE '后海%' OR name LIKE '高新%' OR name LIKE '宝安%' OR name LIKE '西乡%' OR name LIKE '龙华%' OR name LIKE '民治%';
SELECT COUNT(*) AS '新增房源数量' FROM properties WHERE title LIKE '福田%' OR title LIKE '科技%' OR title LIKE '卓越%' OR title LIKE '后海%' OR title LIKE '高新%' OR title LIKE '宝安%' OR title LIKE '西乡%' OR title LIKE '龙华%' OR title LIKE '民治%';

-- 验证热力图数据
SELECT 
    c.name as community_name,
    COUNT(p.property_id) as property_count,
    JSON_UNQUOTE(JSON_EXTRACT(c.location_info, '$.district')) as district,
    JSON_UNQUOTE(JSON_EXTRACT(c.location_info, '$.latitude')) as latitude,
    JSON_UNQUOTE(JSON_EXTRACT(c.location_info, '$.longitude')) as longitude,
    AVG(CAST(JSON_UNQUOTE(JSON_EXTRACT(p.price_info, '$.unit_price')) AS UNSIGNED)) as avg_price
FROM communities c
LEFT JOIN properties p ON c.community_id = p.community_id
WHERE (c.name LIKE '福田%' OR c.name LIKE '科技%' OR c.name LIKE '卓越%' OR c.name LIKE '后海%' 
       OR c.name LIKE '高新%' OR c.name LIKE '宝安%' OR c.name LIKE '西乡%' OR c.name LIKE '龙华%' OR c.name LIKE '民治%')
GROUP BY c.community_id, c.name
ORDER BY avg_price DESC;
