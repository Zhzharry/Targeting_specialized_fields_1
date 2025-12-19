-- 为properties表添加经纬度字段并从communities表同步数据

-- 1. 添加经纬度字段
ALTER TABLE properties 
ADD COLUMN latitude DECIMAL(10, 7) NULL COMMENT '纬度' AFTER layout_info,
ADD COLUMN longitude DECIMAL(10, 7) NULL COMMENT '经度' AFTER latitude;

-- 2. 创建索引以提升查询性能
ALTER TABLE properties
ADD INDEX idx_location (latitude, longitude);

-- 3. 从communities表同步经纬度数据到properties表
UPDATE properties p
INNER JOIN communities c ON p.community_id = c.community_id
SET 
    p.latitude = JSON_UNQUOTE(JSON_EXTRACT(c.location_info, '$.latitude')),
    p.longitude = JSON_UNQUOTE(JSON_EXTRACT(c.location_info, '$.longitude'));

-- 4. 为方便查询，创建一个视图包含房源和小区的完整信息
CREATE OR REPLACE VIEW v_properties_with_location AS
SELECT 
    p.property_id,
    p.title AS property_name,
    p.community_id,
    c.name AS community_name,
    JSON_UNQUOTE(JSON_EXTRACT(c.location_info, '$.district')) AS district,
    JSON_UNQUOTE(JSON_EXTRACT(c.location_info, '$.city')) AS city,
    JSON_UNQUOTE(JSON_EXTRACT(p.price_info, '$.total_price')) AS total_price,
    JSON_UNQUOTE(JSON_EXTRACT(p.price_info, '$.unit_price')) AS unit_price,
    JSON_UNQUOTE(JSON_EXTRACT(p.layout_info, '$.area')) AS area,
    JSON_UNQUOTE(JSON_EXTRACT(p.layout_info, '$.bedroom_count')) AS bedroom_count,
    COALESCE(p.latitude, JSON_UNQUOTE(JSON_EXTRACT(c.location_info, '$.latitude'))) AS latitude,
    COALESCE(p.longitude, JSON_UNQUOTE(JSON_EXTRACT(c.location_info, '$.longitude'))) AS longitude,
    p.status,
    p.view_count,
    p.favorite_count
FROM properties p
INNER JOIN communities c ON p.community_id = c.community_id;

-- 5. 为测试插入一些不同位置和价格的房源数据（可选）
-- 这些数据可以帮助形成更好的热力图效果

-- 深圳南山区不同位置的房源
INSERT INTO communities (name, basic_info, location_info, facility_info) VALUES
('海岸城花园', 
 '{"developer": "华侨城", "build_year": 2016, "total_size": 60000, "total_households": 900}',
 '{"city": "深圳市", "address": "海德三道100号", "district": "南山区", "latitude": 22.52, "province": "广东省", "longitude": 114.01}',
 '{"schools": ["南山小学"], "plot_ratio": 2.8, "green_ratio": 0.33, "management_fee": 3.8, "parking_spaces": 1100}'),
('科技园壹号', 
 '{"developer": "万科", "build_year": 2020, "total_size": 70000, "total_households": 1000}',
 '{"city": "深圳市", "address": "科苑路200号", "district": "南山区", "latitude": 22.55, "province": "广东省", "longitude": 114.08}',
 '{"schools": ["实验学校"], "plot_ratio": 3.2, "green_ratio": 0.38, "management_fee": 4.5, "parking_spaces": 1300}');

-- 福田区不同价格段的房源
INSERT INTO communities (name, basic_info, location_info, facility_info) VALUES
('福田中心', 
 '{"developer": "保利", "build_year": 2017, "total_size": 90000, "total_households": 1500}',
 '{"city": "深圳市", "address": "福华路300号", "district": "福田区", "latitude": 22.53, "province": "广东省", "longitude": 114.05}',
 '{"schools": ["福田学校"], "plot_ratio": 3.5, "green_ratio": 0.4, "management_fee": 5.0, "parking_spaces": 1800}');

-- 更新properties表中的经纬度（如果新添加了小区）
UPDATE properties p
INNER JOIN communities c ON p.community_id = c.community_id
SET 
    p.latitude = JSON_UNQUOTE(JSON_EXTRACT(c.location_info, '$.latitude')),
    p.longitude = JSON_UNQUOTE(JSON_EXTRACT(c.location_info, '$.longitude'))
WHERE p.latitude IS NULL OR p.longitude IS NULL;
