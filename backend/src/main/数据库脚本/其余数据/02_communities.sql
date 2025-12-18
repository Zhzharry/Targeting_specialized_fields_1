-- ============================================
-- 小区表数据注入脚本
-- ============================================

USE `bigdata`;

-- 扩展小区数据
INSERT INTO `communities` (`name`, `basic_info`, `location_info`, `facility_info`, `created_at`) VALUES
('保利香槟国际', '{"developer": "保利地产", "build_year": 2016, "total_size": 120000, "total_households": 2000, "management_company": "保利物业"}', '{"city": "深圳市", "address": "科技园南路789号", "district": "南山区", "latitude": 22.5321, "province": "广东省", "longitude": 114.0567}', '{"schools": ["南山外国语学校", "深圳大学附属中学"], "plot_ratio": 2.8, "green_ratio": 0.38, "management_fee": 3.8, "parking_spaces": 2500}', NOW()),
('招商海月花园', '{"developer": "招商地产", "build_year": 2014, "total_size": 95000, "total_households": 1500, "management_company": "招商物业"}', '{"city": "深圳市", "address": "后海大道321号", "district": "南山区", "latitude": 22.4987, "province": "广东省", "longitude": 113.9456}', '{"schools": ["育才学校", "南山实验学校"], "plot_ratio": 2.6, "green_ratio": 0.32, "management_fee": 3.2, "parking_spaces": 1800}', NOW()),
('中海翠景花园', '{"developer": "中海地产", "build_year": 2017, "total_size": 110000, "total_households": 1800, "management_company": "中海物业"}', '{"city": "深圳市", "address": "福田中心区456号", "district": "福田区", "latitude": 22.5432, "province": "广东省", "longitude": 114.0678}', '{"schools": ["福田外国语学校", "红岭中学"], "plot_ratio": 3.2, "green_ratio": 0.42, "management_fee": 4.2, "parking_spaces": 2200}', NOW()),
('金地梅陇镇', '{"developer": "金地集团", "build_year": 2013, "total_size": 85000, "total_households": 1200, "management_company": "金地物业"}', '{"city": "深圳市", "address": "宝安中心区654号", "district": "宝安区", "latitude": 22.5678, "province": "广东省", "longitude": 113.8901}', '{"schools": ["宝安实验学校", "新安中学"], "plot_ratio": 2.4, "green_ratio": 0.30, "management_fee": 2.8, "parking_spaces": 1500}', NOW()),
('绿景蓝湾半岛', '{"developer": "绿景集团", "build_year": 2019, "total_size": 130000, "total_households": 2200, "management_company": "绿景物业"}', '{"city": "深圳市", "address": "前海路987号", "district": "南山区", "latitude": 22.5123, "province": "广东省", "longitude": 113.9234}', '{"schools": ["前海学校", "南山第二实验学校"], "plot_ratio": 3.5, "green_ratio": 0.45, "management_fee": 4.5, "parking_spaces": 2800}', NOW()),
('龙光玖龙玺', '{"developer": "龙光地产", "build_year": 2020, "total_size": 150000, "total_households": 2500, "management_company": "龙光物业"}', '{"city": "深圳市", "address": "龙华新区123号", "district": "龙华区", "latitude": 22.6543, "province": "广东省", "longitude": 114.0123}', '{"schools": ["龙华实验学校", "观澜中学"], "plot_ratio": 3.0, "green_ratio": 0.40, "management_fee": 3.5, "parking_spaces": 3000}', NOW()),
('佳兆业城市广场', '{"developer": "佳兆业集团", "build_year": 2015, "total_size": 100000, "total_households": 1600, "management_company": "佳兆业物业"}', '{"city": "深圳市", "address": "龙岗中心城567号", "district": "龙岗区", "latitude": 22.7234, "province": "广东省", "longitude": 114.2345}', '{"schools": ["龙岗实验学校", "龙城高级中学"], "plot_ratio": 2.7, "green_ratio": 0.35, "management_fee": 3.0, "parking_spaces": 2000}', NOW()),
('星河湾', '{"developer": "星河湾集团", "build_year": 2012, "total_size": 90000, "total_households": 1400, "management_company": "星河湾物业"}', '{"city": "深圳市", "address": "罗湖中心区890号", "district": "罗湖区", "latitude": 22.5432, "province": "广东省", "longitude": 114.1456}', '{"schools": ["罗湖外国语学校", "翠园中学"], "plot_ratio": 2.5, "green_ratio": 0.33, "management_fee": 3.3, "parking_spaces": 1700}', NOW());

