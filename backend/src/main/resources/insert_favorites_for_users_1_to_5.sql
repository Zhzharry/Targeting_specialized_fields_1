-- ============================================
-- 为用户ID 1-5的5个用户插入收藏历史数据
-- 用途：测试用户相似度计算功能
-- ============================================

USE bigdata;

-- 1. 检查用户1-5是否存在
SELECT user_id, username, phone_number 
FROM users 
WHERE user_id IN (1, 2, 3, 4, 5)
ORDER BY user_id;

-- 2. 确保所有用户存在（如果不存在则创建）
-- 注意：先删除可能存在的用户名或手机号冲突
DELETE FROM users WHERE username IN ('user4', 'user5') AND user_id NOT IN (4, 5);
DELETE FROM users WHERE phone_number IN ('13800001004', '13800001005') AND user_id NOT IN (4, 5);

-- 插入用户4和5（如果不存在）
INSERT INTO users (user_id, username, password, phone_number, user_profile, created_at)
SELECT 4, 'user4', '123456', '13800001004', '{}', NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE user_id = 4);

INSERT INTO users (user_id, username, password, phone_number, user_profile, created_at)
SELECT 5, 'user5', '123456', '13800001005', '{}', NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE user_id = 5);

-- 验证所有用户是否存在
SELECT user_id, username, phone_number 
FROM users 
WHERE user_id IN (1, 2, 3, 4, 5)
ORDER BY user_id;

-- 3. 检查现有的房源ID范围
SELECT 
    MIN(property_id) as min_property_id,
    MAX(property_id) as max_property_id,
    COUNT(*) as total_properties
FROM properties;

-- 4. 为用户1插入收藏数据（15个收藏，确保超过5个）
INSERT INTO favorites (user_id, property_id, favorite_data, created_at) VALUES
-- 用户1的收藏
(1, 1, '{}', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1, 2, '{}', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1, 3, '{}', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(1, 4, '{}', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(1, 5, '{}', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(1, 6, '{}', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(1, 7, '{}', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(1, 8, '{}', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(1, 9, '{}', DATE_SUB(NOW(), INTERVAL 9 DAY)),
(1, 10, '{}', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(1, 11, '{}', DATE_SUB(NOW(), INTERVAL 11 DAY)),
(1, 12, '{}', DATE_SUB(NOW(), INTERVAL 12 DAY)),
(1, 13, '{}', DATE_SUB(NOW(), INTERVAL 13 DAY)),
(1, 14, '{}', DATE_SUB(NOW(), INTERVAL 14 DAY)),
(1, 15, '{}', DATE_SUB(NOW(), INTERVAL 15 DAY))
ON DUPLICATE KEY UPDATE 
    favorite_data = VALUES(favorite_data),
    created_at = VALUES(created_at);

-- 5. 为用户2插入收藏数据（与前5个有重叠，用于计算相似度）
INSERT INTO favorites (user_id, property_id, favorite_data, created_at) VALUES
-- 用户2的收藏（与用户1有5个共同收藏：1-5）
(2, 1, '{}', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 2, '{}', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 3, '{}', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(2, 4, '{}', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(2, 5, '{}', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2, 16, '{}', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(2, 17, '{}', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(2, 18, '{}', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(2, 19, '{}', DATE_SUB(NOW(), INTERVAL 9 DAY)),
(2, 20, '{}', DATE_SUB(NOW(), INTERVAL 10 DAY))
ON DUPLICATE KEY UPDATE 
    favorite_data = VALUES(favorite_data),
    created_at = VALUES(created_at);

-- 6. 为用户3插入收藏数据（与用户1、2有部分重叠）
INSERT INTO favorites (user_id, property_id, favorite_data, created_at) VALUES
-- 用户3的收藏（与用户1有3个共同收藏：1-3）
(3, 1, '{}', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(3, 2, '{}', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(3, 3, '{}', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(3, 21, '{}', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(3, 22, '{}', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(3, 23, '{}', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(3, 24, '{}', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(3, 25, '{}', DATE_SUB(NOW(), INTERVAL 8 DAY))
ON DUPLICATE KEY UPDATE 
    favorite_data = VALUES(favorite_data),
    created_at = VALUES(created_at);

-- 7. 为用户4插入收藏数据（与用户1有部分重叠）
INSERT INTO favorites (user_id, property_id, favorite_data, created_at) VALUES
-- 用户4的收藏（与用户1有2个共同收藏：1-2）
(4, 1, '{}', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(4, 2, '{}', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(4, 26, '{}', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(4, 27, '{}', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(4, 28, '{}', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(4, 29, '{}', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(4, 30, '{}', DATE_SUB(NOW(), INTERVAL 7 DAY))
ON DUPLICATE KEY UPDATE 
    favorite_data = VALUES(favorite_data),
    created_at = VALUES(created_at);

-- 8. 为用户5插入收藏数据（与用户1有部分重叠）
INSERT INTO favorites (user_id, property_id, favorite_data, created_at) VALUES
-- 用户5的收藏（与用户1有4个共同收藏：1-4）
(5, 1, '{}', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(5, 2, '{}', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(5, 3, '{}', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(5, 4, '{}', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(5, 31, '{}', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(5, 32, '{}', DATE_SUB(NOW(), INTERVAL 6 DAY)),
(5, 33, '{}', DATE_SUB(NOW(), INTERVAL 7 DAY)),
(5, 34, '{}', DATE_SUB(NOW(), INTERVAL 8 DAY)),
(5, 35, '{}', DATE_SUB(NOW(), INTERVAL 9 DAY))
ON DUPLICATE KEY UPDATE 
    favorite_data = VALUES(favorite_data),
    created_at = VALUES(created_at);

-- 9. 验证插入结果 - 统计每个用户的收藏数量
SELECT 
    user_id,
    COUNT(*) as favorite_count,
    MIN(created_at) as first_favorite,
    MAX(created_at) as last_favorite
FROM favorites
WHERE user_id IN (1, 2, 3, 4, 5)
GROUP BY user_id
ORDER BY user_id;

-- 10. 查看共同收藏情况（用于验证相似度计算）
SELECT 
    f1.user_id as user1,
    f2.user_id as user2,
    COUNT(*) as common_favorites
FROM favorites f1
INNER JOIN favorites f2 ON f1.property_id = f2.property_id
WHERE f1.user_id IN (1, 2, 3, 4, 5)
  AND f2.user_id IN (1, 2, 3, 4, 5)
  AND f1.user_id < f2.user_id
GROUP BY f1.user_id, f2.user_id
ORDER BY user1, user2;

-- 11. 查看详细的收藏列表（前20条）
SELECT 
    f.favorite_id,
    f.user_id,
    f.property_id,
    p.title as property_title,
    f.created_at
FROM favorites f
LEFT JOIN properties p ON f.property_id = p.property_id
WHERE f.user_id IN (1, 2, 3, 4, 5)
ORDER BY f.user_id, f.created_at DESC
LIMIT 20;

-- ============================================
-- 说明：
-- 1. 用户1：15个收藏（property_id 1-15）
-- 2. 用户2：10个收藏（property_id 1-5, 16-20，与用户1有5个共同收藏）
-- 3. 用户3：8个收藏（property_id 1-3, 21-25，与用户1有3个共同收藏）
-- 4. 用户4：7个收藏（property_id 1-2, 26-30，与用户1有2个共同收藏）
-- 5. 用户5：9个收藏（property_id 1-4, 31-35，与用户1有4个共同收藏）
-- 
-- 这样设计可以确保：
-- - 每个用户都有足够的收藏（超过5个，满足自动触发条件）
-- - 用户之间有共同的收藏，可以计算相似度
-- - 用户1与用户2的相似度应该最高（5个共同收藏）
-- ============================================

