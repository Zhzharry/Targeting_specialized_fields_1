-- ============================================
-- 房源特征表数据注入脚本
-- ============================================

USE `bigdata`;

-- 为新增房源添加特征
INSERT INTO `property_features` (`property_id`, `feature_data`, `created_at`) VALUES
-- 保利香槟国际房源特征
(10, '{"description": "靠近地铁站", "feature_name": "near_subway", "feature_type": "facility", "feature_value": true, "feature_weight": 0.8}', NOW()),
(10, '{"description": "学区房", "feature_name": "near_school", "feature_type": "facility", "feature_value": true, "feature_weight": 0.9}', NOW()),
(11, '{"description": "南北通透", "feature_name": "good_ventilation", "feature_type": "layout", "feature_value": true, "feature_weight": 0.7}', NOW()),
(11, '{"description": "豪华装修", "feature_name": "luxury_decoration", "feature_type": "decoration", "feature_value": true, "feature_weight": 0.85}', NOW()),
(12, '{"description": "靠近地铁站", "feature_name": "near_subway", "feature_type": "facility", "feature_value": true, "feature_weight": 0.8}', NOW()),

-- 招商海月花园房源特征
(13, '{"description": "学区房", "feature_name": "near_school", "feature_type": "facility", "feature_value": true, "feature_weight": 0.9}', NOW()),
(13, '{"description": "南向采光好", "feature_name": "good_lighting", "feature_type": "layout", "feature_value": true, "feature_weight": 0.75}', NOW()),
(14, '{"description": "海景房", "feature_name": "sea_view", "feature_type": "view", "feature_value": true, "feature_weight": 0.95}', NOW()),
(14, '{"description": "豪华装修", "feature_name": "luxury_decoration", "feature_type": "decoration", "feature_value": true, "feature_weight": 0.85}', NOW()),

-- 中海翠景花园房源特征
(15, '{"description": "中心区", "feature_name": "city_center", "feature_type": "location", "feature_value": true, "feature_weight": 0.9}', NOW()),
(15, '{"description": "靠近地铁站", "feature_name": "near_subway", "feature_type": "facility", "feature_value": true, "feature_weight": 0.8}', NOW()),
(16, '{"description": "地铁口", "feature_name": "near_subway", "feature_type": "facility", "feature_value": true, "feature_weight": 0.85}', NOW()),

-- 金地梅陇镇房源特征
(17, '{"description": "地铁口", "feature_name": "near_subway", "feature_type": "facility", "feature_value": true, "feature_weight": 0.85}', NOW()),
(17, '{"description": "精装修", "feature_name": "hard_decoration", "feature_type": "decoration", "feature_value": true, "feature_weight": 0.75}', NOW()),
(18, '{"description": "学区房", "feature_name": "near_school", "feature_type": "facility", "feature_value": true, "feature_weight": 0.9}', NOW()),
(18, '{"description": "大户型", "feature_name": "large_apartment", "feature_type": "layout", "feature_value": true, "feature_weight": 0.8}', NOW()),

-- 绿景蓝湾半岛房源特征
(19, '{"description": "海景房", "feature_name": "sea_view", "feature_type": "view", "feature_value": true, "feature_weight": 0.95}', NOW()),
(19, '{"description": "豪华装修", "feature_name": "luxury_decoration", "feature_type": "decoration", "feature_value": true, "feature_weight": 0.85}', NOW()),
(19, '{"description": "南向", "feature_name": "south_facing", "feature_type": "layout", "feature_value": true, "feature_weight": 0.7}', NOW()),
(20, '{"description": "南北通透", "feature_name": "good_ventilation", "feature_type": "layout", "feature_value": true, "feature_weight": 0.7}', NOW()),
(20, '{"description": "豪华装修", "feature_name": "luxury_decoration", "feature_type": "decoration", "feature_value": true, "feature_weight": 0.85}', NOW()),

-- 龙光玖龙玺房源特征
(21, '{"description": "地铁口", "feature_name": "near_subway", "feature_type": "facility", "feature_value": true, "feature_weight": 0.85}', NOW()),
(21, '{"description": "新房", "feature_name": "new_property", "feature_type": "property", "feature_value": true, "feature_weight": 0.8}', NOW()),
(22, '{"description": "新房", "feature_name": "new_property", "feature_type": "property", "feature_value": true, "feature_weight": 0.8}', NOW()),
(22, '{"description": "精装修", "feature_name": "hard_decoration", "feature_type": "decoration", "feature_value": true, "feature_weight": 0.75}', NOW()),

-- 佳兆业城市广场房源特征
(23, '{"description": "精装修", "feature_name": "hard_decoration", "feature_type": "decoration", "feature_value": true, "feature_weight": 0.75}', NOW()),
(24, '{"description": "大户型", "feature_name": "large_apartment", "feature_type": "layout", "feature_value": true, "feature_weight": 0.8}', NOW()),

-- 星河湾房源特征
(25, '{"description": "中心区", "feature_name": "city_center", "feature_type": "location", "feature_value": true, "feature_weight": 0.9}', NOW()),
(25, '{"description": "精装修", "feature_name": "hard_decoration", "feature_type": "decoration", "feature_value": true, "feature_weight": 0.75}', NOW()),
(26, '{"description": "地铁口", "feature_name": "near_subway", "feature_type": "facility", "feature_value": true, "feature_weight": 0.85}', NOW()),
(26, '{"description": "精装修", "feature_name": "hard_decoration", "feature_type": "decoration", "feature_value": true, "feature_weight": 0.75}', NOW());

