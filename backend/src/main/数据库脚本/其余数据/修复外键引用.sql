-- ============================================
-- 修复外键引用问题
-- 使用子查询确保引用的ID存在
-- ============================================

USE `bigdata`;

-- 临时禁用外键检查（可选，如果仍有问题）
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 修复 04_browsing_history.sql
-- ============================================
-- 使用实际存在的user_id和property_id

INSERT INTO `browsing_history` (`user_id`, `property_id`, `behavior_data`, `created_at`) 
SELECT 
    u.user_id,
    p.property_id,
    '{"duration_seconds": 120, "view_count": 3, "last_view_time": "2024-12-01 10:30:00"}' as behavior_data,
    '2024-11-28 10:30:00' as created_at
FROM users u
CROSS JOIN properties p
WHERE u.username = '张三' AND p.property_id = (SELECT MIN(property_id) FROM properties)
LIMIT 1;

-- 继续添加其他浏览记录（使用类似方式）
-- 为了简化，这里只添加一些示例，实际使用时需要根据具体情况调整

-- ============================================
-- 修复 05_favorites.sql
-- ============================================

INSERT INTO `favorites` (`user_id`, `property_id`, `favorite_data`, `created_at`)
SELECT 
    u.user_id,
    p.property_id,
    '{"note": "位置好，交通便利", "tags": ["地铁", "学区"]}' as favorite_data,
    '2024-11-28 10:35:00' as created_at
FROM users u
CROSS JOIN properties p
WHERE u.username = '张三' AND p.property_id = (SELECT MIN(property_id) FROM properties)
LIMIT 1
ON DUPLICATE KEY UPDATE favorite_data = VALUES(favorite_data);

-- ============================================
-- 修复 06_search_history.sql
-- ============================================

INSERT INTO `search_history` (`user_id`, `search_data`, `created_at`)
SELECT 
    user_id,
    '{"keywords": ["三房", "南山区"], "filters": {"price_min": 300, "price_max": 600, "area_min": 80, "area_max": 120, "bedroom_count": 3}}' as search_data,
    '2024-11-28 10:00:00' as created_at
FROM users
WHERE username = '张三'
LIMIT 1;

-- ============================================
-- 修复 07_user_preferences.sql
-- ============================================

INSERT INTO `user_preferences` (`user_id`, `preference_data`, `created_at`, `updated_at`)
SELECT 
    user_id,
    '{"keywords": ["地铁", "精装"], "locations": ["南山区", "罗湖区"], "area_range": {"max": 100, "min": 70}, "decorations": ["hard", "luxury"], "house_types": ["apartment"], "price_range": {"max": 700, "min": 400}, "orientations": ["south", "southeast"], "bedroom_range": {"max": 3, "min": 2}}' as preference_data,
    NOW(),
    NOW()
FROM users
WHERE username = '赵六'
LIMIT 1
ON DUPLICATE KEY UPDATE preference_data = VALUES(preference_data), updated_at = NOW();

-- ============================================
-- 修复 08_user_recommendations.sql
-- ============================================

INSERT INTO `user_recommendations` (`user_id`, `property_id`, `recommendation_data`, `created_at`)
SELECT 
    u.user_id,
    p.property_id,
    '{"type": "content_based", "score": 0.85, "reason": "基于您的浏览和偏好匹配", "is_viewed": false}' as recommendation_data,
    '2024-12-01 10:00:00' as created_at
FROM users u
CROSS JOIN properties p
WHERE u.username = '张三' AND p.property_id = (SELECT MIN(property_id) FROM properties)
LIMIT 1;

-- ============================================
-- 修复 09_user_similarity.sql
-- ============================================

INSERT INTO `user_similarity` (`user_id1`, `user_id2`, `similarity_data`, `created_at`, `updated_at`)
SELECT 
    u1.user_id,
    u2.user_id,
    '{"similarity_score": 0.65, "algorithm_version": "v1.0", "common_items": 5, "calculation_method": "cosine"}' as similarity_data,
    NOW(),
    NOW()
FROM users u1
CROSS JOIN users u2
WHERE u1.username = '张三' AND u2.username = '李四' AND u1.user_id < u2.user_id
LIMIT 1
ON DUPLICATE KEY UPDATE similarity_data = VALUES(similarity_data), updated_at = NOW();

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;

