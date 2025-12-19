-- 插入热力图密集测试数据
-- 策略：在每个小区周围生成多个点，形成区域热力分布


 DELETE FROM properties WHERE property_name LIKE '测试房源%';
 DELETE FROM communities WHERE community_name LIKE '测试小区%';

-- 为每个小区生成多个房源，在小区中心周围0.005度范围内（约500米）随机分布
-- 这样可以形成区域性的热力图效果

-- ========== 福田区-高价区（8-12万） ==========
-- 福田中心区小区1 (22.5400, 114.0550)
INSERT INTO properties (property_name, price, total_price, area, community_id, created_at) VALUES
('福田豪宅A-1', 110000, 5500000, 50, (SELECT community_id FROM communities WHERE community_name = '测试小区A' LIMIT 1), NOW()),
('福田豪宅A-2', 115000, 5750000, 50, (SELECT community_id FROM communities WHERE community_name = '测试小区A' LIMIT 1), NOW()),
('福田豪宅A-3', 108000, 6480000, 60, (SELECT community_id FROM communities WHERE community_name = '测试小区A' LIMIT 1), NOW()),
('福田豪宅A-4', 112000, 6720000, 60, (SELECT community_id FROM communities WHERE community_name = '测试小区A' LIMIT 1), NOW()),
('福田豪宅A-5', 118000, 7080000, 60, (SELECT community_id FROM communities WHERE community_name = '测试小区A' LIMIT 1), NOW()),
('福田豪宅A-6', 120000, 8400000, 70, (SELECT community_id FROM communities WHERE community_name = '测试小区A' LIMIT 1), NOW()),
('福田豪宅A-7', 115000, 9200000, 80, (SELECT community_id FROM communities WHERE community_name = '测试小区A' LIMIT 1), NOW()),
('福田豪宅A-8', 122000, 9760000, 80, (SELECT community_id FROM communities WHERE community_name = '测试小区A' LIMIT 1), NOW());

-- 为福田中心区生成更多相邻位置的小区和房源
INSERT INTO communities (community_name, district, address, location_info, created_at) VALUES
('福田中心花园', '福田区', '深圳市福田区中心区', '{"latitude": 22.5420, "longitude": 114.0570, "type": "Point"}', NOW()),
('卓越世纪中心', '福田区', '深圳市福田区福华路', '{"latitude": 22.5390, "longitude": 114.0540, "type": "Point"}', NOW()),
('福田印象', '福田区', '深圳市福田区深南大道', '{"latitude": 22.5410, "longitude": 114.0560, "type": "Point"}', NOW());

INSERT INTO properties (property_name, price, total_price, area, community_id, created_at) VALUES
('福田中心花园1号', 105000, 5250000, 50, (SELECT community_id FROM communities WHERE community_name = '福田中心花园' LIMIT 1), NOW()),
('福田中心花园2号', 108000, 6480000, 60, (SELECT community_id FROM communities WHERE community_name = '福田中心花园' LIMIT 1), NOW()),
('福田中心花园3号', 110000, 7700000, 70, (SELECT community_id FROM communities WHERE community_name = '福田中心花园' LIMIT 1), NOW()),
('福田中心花园4号', 112000, 8960000, 80, (SELECT community_id FROM communities WHERE community_name = '福田中心花园' LIMIT 1), NOW()),
('卓越世纪1号', 118000, 5900000, 50, (SELECT community_id FROM communities WHERE community_name = '卓越世纪中心' LIMIT 1), NOW()),
('卓越世纪2号', 120000, 7200000, 60, (SELECT community_id FROM communities WHERE community_name = '卓越世纪中心' LIMIT 1), NOW()),
('卓越世纪3号', 115000, 8050000, 70, (SELECT community_id FROM communities WHERE community_name = '卓越世纪中心' LIMIT 1), NOW()),
('卓越世纪4号', 125000, 10000000, 80, (SELECT community_id FROM communities WHERE community_name = '卓越世纪中心' LIMIT 1), NOW()),
('福田印象1号', 102000, 5100000, 50, (SELECT community_id FROM communities WHERE community_name = '福田印象' LIMIT 1), NOW()),
('福田印象2号', 105000, 6300000, 60, (SELECT community_id FROM communities WHERE community_name = '福田印象' LIMIT 1), NOW()),
('福田印象3号', 108000, 7560000, 70, (SELECT community_id FROM communities WHERE community_name = '福田印象' LIMIT 1), NOW());

-- ========== 南山区-中高价区（7-10万） ==========
INSERT INTO communities (community_name, district, address, location_info, created_at) VALUES
('科技园1号', '南山区', '深圳市南山区科技园', '{"latitude": 22.5350, "longitude": 113.9520, "type": "Point"}', NOW()),
('科技园2号', '南山区', '深圳市南山区高新区', '{"latitude": 22.5370, "longitude": 113.9540, "type": "Point"}', NOW()),
('科技园3号', '南山区', '深圳市南山区科苑路', '{"latitude": 22.5340, "longitude": 113.9510, "type": "Point"}', NOW()),
('后海湾1号', '南山区', '深圳市南山区后海', '{"latitude": 22.5200, "longitude": 113.9350, "type": "Point"}', NOW()),
('后海湾2号', '南山区', '深圳市南山区后海大道', '{"latitude": 22.5220, "longitude": 113.9370, "type": "Point"}', NOW());

INSERT INTO properties (property_name, price, total_price, area, community_id, created_at) VALUES
('科技园1号-A', 88000, 4400000, 50, (SELECT community_id FROM communities WHERE community_name = '科技园1号' LIMIT 1), NOW()),
('科技园1号-B', 90000, 5400000, 60, (SELECT community_id FROM communities WHERE community_name = '科技园1号' LIMIT 1), NOW()),
('科技园1号-C', 92000, 6440000, 70, (SELECT community_id FROM communities WHERE community_name = '科技园1号' LIMIT 1), NOW()),
('科技园1号-D', 95000, 7600000, 80, (SELECT community_id FROM communities WHERE community_name = '科技园1号' LIMIT 1), NOW()),
('科技园2号-A', 85000, 4250000, 50, (SELECT community_id FROM communities WHERE community_name = '科技园2号' LIMIT 1), NOW()),
('科技园2号-B', 87000, 5220000, 60, (SELECT community_id FROM communities WHERE community_name = '科技园2号' LIMIT 1), NOW()),
('科技园2号-C', 89000, 6230000, 70, (SELECT community_id FROM communities WHERE community_name = '科技园2号' LIMIT 1), NOW()),
('科技园3号-A', 91000, 4550000, 50, (SELECT community_id FROM communities WHERE community_name = '科技园3号' LIMIT 1), NOW()),
('科技园3号-B', 93000, 5580000, 60, (SELECT community_id FROM communities WHERE community_name = '科技园3号' LIMIT 1), NOW()),
('科技园3号-C', 95000, 6650000, 70, (SELECT community_id FROM communities WHERE community_name = '科技园3号' LIMIT 1), NOW()),
('后海湾1号-A', 78000, 3900000, 50, (SELECT community_id FROM communities WHERE community_name = '后海湾1号' LIMIT 1), NOW()),
('后海湾1号-B', 80000, 4800000, 60, (SELECT community_id FROM communities WHERE community_name = '后海湾1号' LIMIT 1), NOW()),
('后海湾1号-C', 82000, 5740000, 70, (SELECT community_id FROM communities WHERE community_name = '后海湾1号' LIMIT 1), NOW()),
('后海湾2号-A', 75000, 3750000, 50, (SELECT community_id FROM communities WHERE community_name = '后海湾2号' LIMIT 1), NOW()),
('后海湾2号-B', 77000, 4620000, 60, (SELECT community_id FROM communities WHERE community_name = '后海湾2号' LIMIT 1), NOW());

-- ========== 宝安区-中等价区（6-8万） ==========
INSERT INTO communities (community_name, district, address, location_info, created_at) VALUES
('宝安中心区1号', '宝安区', '深圳市宝安区新安街道', '{"latitude": 22.5550, "longitude": 113.8850, "type": "Point"}', NOW()),
('宝安中心区2号', '宝安区', '深圳市宝安区宝安大道', '{"latitude": 22.5570, "longitude": 113.8870, "type": "Point"}', NOW()),
('宝安中心区3号', '宝安区', '深圳市宝安区中心路', '{"latitude": 22.5540, "longitude": 113.8840, "type": "Point"}', NOW()),
('西乡片区1号', '宝安区', '深圳市宝安区西乡', '{"latitude": 22.5700, "longitude": 113.8700, "type": "Point"}', NOW()),
('西乡片区2号', '宝安区', '深圳市宝安区西乡大道', '{"latitude": 22.5720, "longitude": 113.8720, "type": "Point"}', NOW());

INSERT INTO properties (property_name, price, total_price, area, community_id, created_at) VALUES
('宝安中心1号-A', 68000, 3400000, 50, (SELECT community_id FROM communities WHERE community_name = '宝安中心区1号' LIMIT 1), NOW()),
('宝安中心1号-B', 70000, 4200000, 60, (SELECT community_id FROM communities WHERE community_name = '宝安中心区1号' LIMIT 1), NOW()),
('宝安中心1号-C', 72000, 5040000, 70, (SELECT community_id FROM communities WHERE community_name = '宝安中心区1号' LIMIT 1), NOW()),
('宝安中心2号-A', 65000, 3250000, 50, (SELECT community_id FROM communities WHERE community_name = '宝安中心区2号' LIMIT 1), NOW()),
('宝安中心2号-B', 67000, 4020000, 60, (SELECT community_id FROM communities WHERE community_name = '宝安中心区2号' LIMIT 1), NOW()),
('宝安中心2号-C', 69000, 4830000, 70, (SELECT community_id FROM communities WHERE community_name = '宝安中心区2号' LIMIT 1), NOW()),
('宝安中心3号-A', 70000, 3500000, 50, (SELECT community_id FROM communities WHERE community_name = '宝安中心区3号' LIMIT 1), NOW()),
('宝安中心3号-B', 72000, 4320000, 60, (SELECT community_id FROM communities WHERE community_name = '宝安中心区3号' LIMIT 1), NOW()),
('西乡1号-A', 62000, 3100000, 50, (SELECT community_id FROM communities WHERE community_name = '西乡片区1号' LIMIT 1), NOW()),
('西乡1号-B', 64000, 3840000, 60, (SELECT community_id FROM communities WHERE community_name = '西乡片区1号' LIMIT 1), NOW()),
('西乡1号-C', 66000, 4620000, 70, (SELECT community_id FROM communities WHERE community_name = '西乡片区1号' LIMIT 1), NOW()),
('西乡2号-A', 60000, 3000000, 50, (SELECT community_id FROM communities WHERE community_name = '西乡片区2号' LIMIT 1), NOW()),
('西乡2号-B', 62000, 3720000, 60, (SELECT community_id FROM communities WHERE community_name = '西乡片区2号' LIMIT 1), NOW());

-- ========== 龙华区-中低价区（5-7万） ==========
INSERT INTO communities (community_name, district, address, location_info, created_at) VALUES
('龙华中心1号', '龙华区', '深圳市龙华区龙华街道', '{"latitude": 22.6500, "longitude": 114.0300, "type": "Point"}', NOW()),
('龙华中心2号', '龙华区', '深圳市龙华区民治大道', '{"latitude": 22.6520, "longitude": 114.0320, "type": "Point"}', NOW()),
('民治片区1号', '龙华区', '深圳市龙华区民治', '{"latitude": 22.6150, "longitude": 114.0380, "type": "Point"}', NOW()),
('民治片区2号', '龙华区', '深圳市龙华区民旺路', '{"latitude": 22.6170, "longitude": 114.0400, "type": "Point"}', NOW());

INSERT INTO properties (property_name, price, total_price, area, community_id, created_at) VALUES
('龙华中心1号-A', 58000, 2900000, 50, (SELECT community_id FROM communities WHERE community_name = '龙华中心1号' LIMIT 1), NOW()),
('龙华中心1号-B', 60000, 3600000, 60, (SELECT community_id FROM communities WHERE community_name = '龙华中心1号' LIMIT 1), NOW()),
('龙华中心1号-C', 62000, 4340000, 70, (SELECT community_id FROM communities WHERE community_name = '龙华中心1号' LIMIT 1), NOW()),
('龙华中心1号-D', 64000, 5120000, 80, (SELECT community_id FROM communities WHERE community_name = '龙华中心1号' LIMIT 1), NOW()),
('龙华中心2号-A', 56000, 2800000, 50, (SELECT community_id FROM communities WHERE community_name = '龙华中心2号' LIMIT 1), NOW()),
('龙华中心2号-B', 58000, 3480000, 60, (SELECT community_id FROM communities WHERE community_name = '龙华中心2号' LIMIT 1), NOW()),
('龙华中心2号-C', 60000, 4200000, 70, (SELECT community_id FROM communities WHERE community_name = '龙华中心2号' LIMIT 1), NOW()),
('民治1号-A', 52000, 2600000, 50, (SELECT community_id FROM communities WHERE community_name = '民治片区1号' LIMIT 1), NOW()),
('民治1号-B', 54000, 3240000, 60, (SELECT community_id FROM communities WHERE community_name = '民治片区1号' LIMIT 1), NOW()),
('民治1号-C', 56000, 3920000, 70, (SELECT community_id FROM communities WHERE community_name = '民治片区1号' LIMIT 1), NOW()),
('民治2号-A', 50000, 2500000, 50, (SELECT community_id FROM communities WHERE community_name = '民治片区2号' LIMIT 1), NOW()),
('民治2号-B', 52000, 3120000, 60, (SELECT community_id FROM communities WHERE community_name = '民治片区2号' LIMIT 1), NOW());

-- ========== 罗湖区-中等偏低价区（5.5-7.5万） ==========
INSERT INTO communities (community_name, district, address, location_info, created_at) VALUES
('罗湖中心1号', '罗湖区', '深圳市罗湖区人民南路', '{"latitude": 22.5480, "longitude": 114.1320, "type": "Point"}', NOW()),
('罗湖中心2号', '罗湖区', '深圳市罗湖区深南东路', '{"latitude": 22.5500, "longitude": 114.1340, "type": "Point"}', NOW()),
('东门片区1号', '罗湖区', '深圳市罗湖区东门', '{"latitude": 22.5450, "longitude": 114.1280, "type": "Point"}', NOW());

INSERT INTO properties (property_name, price, total_price, area, community_id, created_at) VALUES
('罗湖中心1号-A', 65000, 3250000, 50, (SELECT community_id FROM communities WHERE community_name = '罗湖中心1号' LIMIT 1), NOW()),
('罗湖中心1号-B', 67000, 4020000, 60, (SELECT community_id FROM communities WHERE community_name = '罗湖中心1号' LIMIT 1), NOW()),
('罗湖中心1号-C', 69000, 4830000, 70, (SELECT community_id FROM communities WHERE community_name = '罗湖中心1号' LIMIT 1), NOW()),
('罗湖中心2号-A', 63000, 3150000, 50, (SELECT community_id FROM communities WHERE community_name = '罗湖中心2号' LIMIT 1), NOW()),
('罗湖中心2号-B', 65000, 3900000, 60, (SELECT community_id FROM communities WHERE community_name = '罗湖中心2号' LIMIT 1), NOW()),
('罗湖中心2号-C', 67000, 4690000, 70, (SELECT community_id FROM communities WHERE community_name = '罗湖中心2号' LIMIT 1), NOW()),
('东门1号-A', 55000, 2750000, 50, (SELECT community_id FROM communities WHERE community_name = '东门片区1号' LIMIT 1), NOW()),
('东门1号-B', 57000, 3420000, 60, (SELECT community_id FROM communities WHERE community_name = '东门片区1号' LIMIT 1), NOW()),
('东门1号-C', 59000, 4130000, 70, (SELECT community_id FROM communities WHERE community_name = '东门片区1号' LIMIT 1), NOW());

-- 统计插入的数据
SELECT '数据插入完成！' AS status;
SELECT COUNT(*) AS '新增小区数量' FROM communities WHERE community_name LIKE '%号' OR community_name LIKE '福田%' OR community_name LIKE '卓越%';
SELECT COUNT(*) AS '新增房源数量' FROM properties WHERE property_name LIKE '%号%' OR property_name LIKE '福田%' OR property_name LIKE '科技%' OR property_name LIKE '宝安%' OR property_name LIKE '龙华%' OR property_name LIKE '罗湖%' OR property_name LIKE '西乡%' OR property_name LIKE '民治%' OR property_name LIKE '东门%';
