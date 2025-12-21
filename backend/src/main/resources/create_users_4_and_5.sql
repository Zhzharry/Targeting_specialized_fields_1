-- ============================================
-- 快速修复：创建用户4和5
-- ============================================

USE bigdata;

-- 创建用户4（如果不存在）
INSERT INTO users (user_id, username, password, phone_number, user_profile, created_at)
SELECT 4, 'user4', '123456', '13800001004', '{}', NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE user_id = 4);

-- 创建用户5（如果不存在）
INSERT INTO users (user_id, username, password, phone_number, user_profile, created_at)
SELECT 5, 'user5', '123456', '13800001005', '{}', NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE user_id = 5);

-- 验证用户是否存在
SELECT user_id, username, phone_number 
FROM users 
WHERE user_id IN (4, 5)
ORDER BY user_id;

