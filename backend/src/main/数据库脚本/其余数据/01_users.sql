-- ============================================
-- 用户表数据注入脚本
-- ============================================

USE `bigdata`;

-- 扩展用户数据（假设已有3个用户，从user_id=4开始）
INSERT INTO `users` (`username`, `password`, `phone_number`, `user_profile`, `created_at`) VALUES
('赵六', 'password101', '13600136000', '{"budget": {"max": 600, "min": 350}, "family_structure": "couple", "preferred_locations": ["南山区", "罗湖区"]}', NOW()),
('孙七', 'password102', '13500135000', '{"budget": {"max": 900, "min": 500}, "family_structure": "family", "preferred_locations": ["福田区", "南山区"]}', NOW()),
('周八', 'password103', '13400134000', '{"budget": {"max": 450, "min": 250}, "family_structure": "single", "preferred_locations": ["宝安区"]}', NOW()),
('吴九', 'password104', '13300133000', '{"budget": {"max": 800, "min": 400}, "family_structure": "family", "preferred_locations": ["南山区", "福田区", "宝安区"]}', NOW()),
('郑十', 'password105', '13200132000', '{"budget": {"max": 550, "min": 300}, "family_structure": "couple", "preferred_locations": ["龙岗区", "罗湖区"]}', NOW()),
('钱一', 'password106', '13100131000', '{"budget": {"max": 1200, "min": 600}, "family_structure": "family", "preferred_locations": ["南山区"]}', NOW()),
('陈二', 'password107', '13000130000', '{"budget": {"max": 400, "min": 200}, "family_structure": "single", "preferred_locations": ["宝安区", "龙岗区"]}', NOW()),
('林三', 'password108', '15900159000', '{"budget": {"max": 700, "min": 400}, "family_structure": "couple", "preferred_locations": ["福田区"]}', NOW()),
('黄四', 'password109', '15800158000', '{"budget": {"max": 1000, "min": 500}, "family_structure": "family", "preferred_locations": ["南山区", "福田区"]}', NOW()),
('刘五', 'password110', '15700157000', '{"budget": {"max": 500, "min": 280}, "family_structure": "couple", "preferred_locations": ["罗湖区", "盐田区"]}', NOW());

