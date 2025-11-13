-- ====================================
-- 二手房项目数据库创建脚本
-- 版本: 1.0
-- 日期: 2025-11-13
-- ====================================

-- 创建数据库
DROP DATABASE IF EXISTS second_hand_housing;
CREATE DATABASE second_hand_housing DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE second_hand_housing;

-- ====================================
-- 1. 核心用户表
-- ====================================

-- 用户表
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（明文存储）',
    phone_number VARCHAR(20) UNIQUE NOT NULL COMMENT '手机号',
    user_profile JSON COMMENT '用户扩展信息（预算、家庭结构等）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_username (username),
    INDEX idx_phone (phone_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 用户偏好表
CREATE TABLE user_preferences (
    preference_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '偏好ID',
    user_id INT NOT NULL COMMENT '用户ID',
    preference_data JSON COMMENT '偏好详细数据',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户偏好表';

-- ====================================
-- 2. 房源相关表
-- ====================================

-- 小区信息表
CREATE TABLE communities (
    community_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '小区ID',
    name VARCHAR(200) NOT NULL COMMENT '小区名称',
    basic_info JSON COMMENT '基本信息（建成年份、开发商）',
    location_info JSON COMMENT '位置信息（省市区、坐标）',
    facility_info JSON COMMENT '设施信息（物业费、学校）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_name (name),
    INDEX idx_city ((CAST(location_info->>'$.city' AS CHAR(50)))),
    INDEX idx_district ((CAST(location_info->>'$.district' AS CHAR(50))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小区信息表';

-- 房源表
CREATE TABLE properties (
    property_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '房源ID',
    community_id INT NOT NULL COMMENT '小区ID',
    title VARCHAR(200) NOT NULL COMMENT '房源标题',
    basic_info JSON COMMENT '基本信息（房型、建造年份、装修）',
    price_info JSON COMMENT '价格信息（总价、单价、历史价格）',
    layout_info JSON COMMENT '户型信息（几室几厅、面积、楼层、朝向）',
    status ENUM('for_sale', 'sold') DEFAULT 'for_sale' COMMENT '房源状态',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    favorite_count INT DEFAULT 0 COMMENT '收藏次数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (community_id) REFERENCES communities(community_id) ON DELETE CASCADE,
    INDEX idx_community_id (community_id),
    INDEX idx_status (status),
    INDEX idx_price ((CAST(price_info->>'$.total_price' AS DECIMAL(12,2)))),
    INDEX idx_bedroom ((CAST(layout_info->>'$.bedroom_count' AS UNSIGNED)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='房源表';

-- ====================================
-- 3. 用户行为表
-- ====================================

-- 浏览历史表
CREATE TABLE browsing_history (
    history_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '历史ID',
    user_id INT NOT NULL COMMENT '用户ID',
    property_id INT NOT NULL COMMENT '房源ID',
    behavior_data JSON COMMENT '行为数据（浏览时长、次数）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (property_id) REFERENCES properties(property_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_property_id (property_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='浏览历史表';

-- 搜索历史表
CREATE TABLE search_history (
    search_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '搜索ID',
    user_id INT NOT NULL COMMENT '用户ID',
    search_data JSON COMMENT '搜索数据（关键词、筛选条件）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间',
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='搜索历史表';

-- 收藏表
CREATE TABLE favorites (
    favorite_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '收藏ID',
    user_id INT NOT NULL COMMENT '用户ID',
    property_id INT NOT NULL COMMENT '房源ID',
    favorite_data JSON COMMENT '收藏备注信息',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (property_id) REFERENCES properties(property_id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_property (user_id, property_id),
    INDEX idx_user_id (user_id),
    INDEX idx_property_id (property_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- ====================================
-- 4. 推荐系统表
-- ====================================

-- 用户相似度表
CREATE TABLE user_similarity (
    similarity_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '相似度ID',
    user_id1 INT NOT NULL COMMENT '用户1ID',
    user_id2 INT NOT NULL COMMENT '用户2ID',
    similarity_data JSON COMMENT '相似度数据（分数、算法版本）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '计算时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id1) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id2) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_pair (user_id1, user_id2),
    INDEX idx_user1 (user_id1),
    INDEX idx_user2 (user_id2),
    INDEX idx_score ((CAST(similarity_data->>'$.similarity_score' AS DECIMAL(5,4))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户相似度表';

-- 房源相似度表
CREATE TABLE property_similarity (
    similarity_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '相似度ID',
    property_id1 INT NOT NULL COMMENT '房源1ID',
    property_id2 INT NOT NULL COMMENT '房源2ID',
    similarity_data JSON COMMENT '相似度数据（分数、特征权重）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '计算时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (property_id1) REFERENCES properties(property_id) ON DELETE CASCADE,
    FOREIGN KEY (property_id2) REFERENCES properties(property_id) ON DELETE CASCADE,
    UNIQUE KEY uk_property_pair (property_id1, property_id2),
    INDEX idx_property1 (property_id1),
    INDEX idx_property2 (property_id2),
    INDEX idx_score ((CAST(similarity_data->>'$.similarity_score' AS DECIMAL(5,4))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='房源相似度表';

-- 用户推荐表
CREATE TABLE user_recommendations (
    recommendation_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '推荐ID',
    user_id INT NOT NULL COMMENT '用户ID',
    property_id INT NOT NULL COMMENT '房源ID',
    recommendation_data JSON COMMENT '推荐数据（类型、分数、原因）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '推荐时间',
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (property_id) REFERENCES properties(property_id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_property_id (property_id),
    INDEX idx_score ((CAST(recommendation_data->>'$.score' AS DECIMAL(5,4)))),
    INDEX idx_viewed ((CAST(recommendation_data->>'$.is_viewed' AS UNSIGNED)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户推荐表';

-- ====================================
-- 5. 交易记录表
-- ====================================

-- 成交记录表
CREATE TABLE transaction_records (
    record_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    property_id INT COMMENT '房源ID',
    community_id INT COMMENT '小区ID',
    transaction_data JSON COMMENT '成交数据（日期、价格等）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (property_id) REFERENCES properties(property_id) ON DELETE SET NULL,
    FOREIGN KEY (community_id) REFERENCES communities(community_id) ON DELETE SET NULL,
    INDEX idx_property_id (property_id),
    INDEX idx_community_id (community_id),
    INDEX idx_transaction_date ((CAST(transaction_data->>'$.transaction_date' AS DATE))),
    INDEX idx_city ((CAST(transaction_data->>'$.city' AS CHAR(50)))),
    INDEX idx_district ((CAST(transaction_data->>'$.district' AS CHAR(50))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成交记录表';

-- ====================================
-- 6. 附加功能表
-- ====================================

-- 房源特征表
CREATE TABLE property_features (
    feature_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '特征ID',
    property_id INT NOT NULL COMMENT '房源ID',
    feature_data JSON COMMENT '特征数据',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (property_id) REFERENCES properties(property_id) ON DELETE CASCADE,
    INDEX idx_property_id (property_id),
    INDEX idx_feature_type ((CAST(feature_data->>'$.feature_type' AS CHAR(50))))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='房源特征表';

-- ====================================
-- 插入测试数据
-- ====================================

-- 插入测试用户
INSERT INTO users (username, password, phone_number, user_profile) VALUES
('张三', 'password123', '13800138000', '{"budget": {"min": 300, "max": 500}, "preferred_locations": ["南山区", "福田区"], "family_structure": "couple"}'),
('李四', 'password456', '13900139000', '{"budget": {"min": 400, "max": 700}, "preferred_locations": ["宝安区"], "family_structure": "family"}'),
('王五', 'password789', '13700137000', '{"budget": {"min": 200, "max": 400}, "preferred_locations": ["龙岗区"], "family_structure": "single"}');

-- 插入测试小区
INSERT INTO communities (name, basic_info, location_info, facility_info) VALUES
('万科城市花园', 
 '{"build_year": 2015, "total_size": 50000, "total_households": 800, "developer": "万科", "management_company": "万科物业"}',
 '{"province": "广东省", "city": "深圳市", "district": "南山区", "address": "科技园路123号", "longitude": 114.123456, "latitude": 22.543210}',
 '{"management_fee": 3.5, "parking_spaces": 1000, "plot_ratio": 2.5, "green_ratio": 0.35, "schools": ["深圳小学", "实验中学"]}'),
('华润城', 
 '{"build_year": 2018, "total_size": 80000, "total_households": 1200, "developer": "华润", "management_company": "华润物业"}',
 '{"province": "广东省", "city": "深圳市", "district": "福田区", "address": "深南大道456号", "longitude": 114.056789, "latitude": 22.543000}',
 '{"management_fee": 4.0, "parking_spaces": 1500, "plot_ratio": 3.0, "green_ratio": 0.40, "schools": ["福田小学", "红岭中学"]}');

-- 插入测试房源
INSERT INTO properties (community_id, title, basic_info, price_info, layout_info, status) VALUES
(1, '万科城市花园 精装三房 南向采光好', 
 '{"property_type": "apartment", "build_year": 2018, "decoration": "hard"}',
 '{"total_price": 650.5, "unit_price": 85000, "price_history": [{"date": "2024-01-01", "price": 680}]}',
 '{"bedroom_count": 3, "living_room_count": 2, "bathroom_count": 2, "area": 89.5, "floor": 15, "total_floors": 28, "orientation": "south"}',
 'for_sale'),
(1, '万科城市花园 豪华两房 地铁口', 
 '{"property_type": "apartment", "build_year": 2018, "decoration": "luxury"}',
 '{"total_price": 480.0, "unit_price": 82000, "price_history": [{"date": "2024-02-01", "price": 490}]}',
 '{"bedroom_count": 2, "living_room_count": 1, "bathroom_count": 1, "area": 68.0, "floor": 10, "total_floors": 28, "orientation": "southeast"}',
 'for_sale'),
(2, '华润城 四房大户型 学区房', 
 '{"property_type": "apartment", "build_year": 2019, "decoration": "hard"}',
 '{"total_price": 980.0, "unit_price": 90000, "price_history": [{"date": "2024-03-01", "price": 1000}]}',
 '{"bedroom_count": 4, "living_room_count": 2, "bathroom_count": 3, "area": 128.0, "floor": 20, "total_floors": 35, "orientation": "south"}',
 'for_sale');

-- 插入用户偏好
INSERT INTO user_preferences (user_id, preference_data) VALUES
(1, '{"price_range": {"min": 300, "max": 600}, "area_range": {"min": 80, "max": 120}, "locations": ["南山区", "福田区"], "house_types": ["apartment"], "bedroom_range": {"min": 2, "max": 4}, "orientations": ["south", "southeast"], "decorations": ["hard", "luxury"], "keywords": ["学区", "地铁", "新房"]}'),
(2, '{"price_range": {"min": 400, "max": 800}, "area_range": {"min": 100, "max": 150}, "locations": ["宝安区", "福田区"], "house_types": ["apartment"], "bedroom_range": {"min": 3, "max": 4}, "orientations": ["south"], "decorations": ["luxury"], "keywords": ["学区", "公园"]}');

-- 插入成交记录
INSERT INTO transaction_records (property_id, community_id, transaction_data) VALUES
(1, 1, '{"transaction_date": "2024-01-20", "city": "深圳市", "district": "南山区", "community_name": "万科城市花园", "layout": "3室2厅2卫", "orientation": "南", "floor_info": "中层/共28层", "area": 89.5, "total_price": 620.5, "unit_price": 69300, "source_url": "https://example.com/property/123"}'),
(2, 1, '{"transaction_date": "2024-02-15", "city": "深圳市", "district": "南山区", "community_name": "万科城市花园", "layout": "2室1厅1卫", "orientation": "东南", "floor_info": "中层/共28层", "area": 68.0, "total_price": 475.0, "unit_price": 69850, "source_url": "https://example.com/property/124"}');

-- 插入房源特征
INSERT INTO property_features (property_id, feature_data) VALUES
(1, '{"feature_type": "facility", "feature_name": "near_subway", "feature_value": true, "feature_weight": 0.8, "description": "靠近地铁站"}'),
(1, '{"feature_type": "facility", "feature_name": "near_school", "feature_value": true, "feature_weight": 0.9, "description": "学区房"}'),
(3, '{"feature_type": "facility", "feature_name": "near_school", "feature_value": true, "feature_weight": 0.9, "description": "重点学区房"}');

-- ====================================
-- 创建完成提示
-- ====================================

SELECT '数据库创建完成！' AS message;
SELECT CONCAT('共创建 ', COUNT(*), ' 张表') AS summary FROM information_schema.tables WHERE table_schema = 'second_hand_housing';
SELECT table_name AS '表名', table_comment AS '说明' FROM information_schema.tables WHERE table_schema = 'second_hand_housing' ORDER BY table_name;