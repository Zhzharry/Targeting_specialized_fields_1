-- ============================================
-- 插入用户ID 19-30的用户数据
-- ============================================

USE bigdata;

-- 插入用户19-30
INSERT INTO users (user_id, username, password, phone_number, user_profile, created_at) VALUES
(19, 'user19', 'password123', '13800001019', '{"budget": {"max": 500, "min": 300}, "family_structure": "couple", "preferred_locations": ["南山区", "福田区"]}', '2025-12-01 20:26:18'),
(20, 'user20', 'password123', '13800001020', '{"budget": {"max": 600, "min": 400}, "family_structure": "family", "preferred_locations": ["罗湖区", "龙岗区"]}', '2025-12-01 20:26:18'),
(21, 'user21', 'password123', '13800001021', '{"budget": {"max": 450, "min": 250}, "family_structure": "single", "preferred_locations": ["南山区"]}', '2025-12-01 20:26:18'),
(22, 'user22', 'password123', '13800001022', '{"budget": {"max": 700, "min": 500}, "family_structure": "couple", "preferred_locations": ["福田区", "南山区", "宝安区"]}', '2025-12-01 20:26:18'),
(23, 'user23', 'password123', '13800001023', '{"budget": {"max": 550, "min": 350}, "family_structure": "family", "preferred_locations": ["龙岗区", "龙华区"]}', '2025-12-01 20:26:18'),
(24, 'user24', 'password123', '13800001024', '{"budget": {"max": 400, "min": 200}, "family_structure": "single", "preferred_locations": ["南山区", "福田区"]}', '2025-12-01 20:26:18'),
(25, 'user25', 'password123', '13800001025', '{"budget": {"max": 650, "min": 450}, "family_structure": "couple", "preferred_locations": ["罗湖区"]}', '2025-12-01 20:26:18'),
(26, 'user26', 'password123', '13800001026', '{"budget": {"max": 500, "min": 300}, "family_structure": "family", "preferred_locations": ["福田区", "南山区"]}', '2025-12-01 20:26:18'),
(27, 'user27', 'password123', '13800001027', '{"budget": {"max": 350, "min": 200}, "family_structure": "single", "preferred_locations": ["宝安区"]}', '2025-12-01 20:26:18'),
(28, 'user28', 'password123', '13800001028', '{"budget": {"max": 600, "min": 400}, "family_structure": "couple", "preferred_locations": ["南山区", "福田区", "罗湖区"]}', '2025-12-01 20:26:18'),
(29, 'user29', 'password123', '13800001029', '{"budget": {"max": 480, "min": 280}, "family_structure": "family", "preferred_locations": ["龙岗区"]}', '2025-12-01 20:26:18'),
(30, 'user30', 'password123', '13800001030', '{"budget": {"max": 550, "min": 350}, "family_structure": "single", "preferred_locations": ["南山区", "宝安区"]}', '2025-12-01 20:26:18')
ON DUPLICATE KEY UPDATE 
    username = VALUES(username),
    password = VALUES(password),
    phone_number = VALUES(phone_number),
    user_profile = VALUES(user_profile);

-- 验证插入结果
SELECT 
    user_id,
    username,
    phone_number,
    user_profile,
    created_at
FROM users
WHERE user_id BETWEEN 19 AND 30
ORDER BY user_id;

-- 统计信息
SELECT 
    COUNT(*) as total_users,
    MIN(user_id) as min_user_id,
    MAX(user_id) as max_user_id
FROM users
WHERE user_id BETWEEN 19 AND 30;

