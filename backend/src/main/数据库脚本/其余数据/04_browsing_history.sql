-- ============================================
-- 浏览历史表数据注入脚本
-- ============================================

USE `bigdata`;

-- 为不同用户生成浏览历史记录
INSERT INTO `browsing_history` (`user_id`, `property_id`, `behavior_data`, `created_at`) VALUES
-- 用户1的浏览记录
(1, 1, '{"duration_seconds": 120, "view_count": 3, "last_view_time": "2024-12-01 10:30:00"}', '2024-11-28 10:30:00'),
(1, 2, '{"duration_seconds": 180, "view_count": 5, "last_view_time": "2024-12-01 14:20:00"}', '2024-11-29 14:20:00'),
(1, 3, '{"duration_seconds": 90, "view_count": 2, "last_view_time": "2024-11-30 16:45:00"}', '2024-11-30 16:45:00'),
(1, 10, '{"duration_seconds": 240, "view_count": 4, "last_view_time": "2024-12-01 09:15:00"}', '2024-11-27 09:15:00'),
(1, 11, '{"duration_seconds": 150, "view_count": 3, "last_view_time": "2024-12-01 11:30:00"}', '2024-11-28 11:30:00'),

-- 用户2的浏览记录
(2, 3, '{"duration_seconds": 200, "view_count": 6, "last_view_time": "2024-12-01 15:00:00"}', '2024-11-25 15:00:00'),
(2, 4, '{"duration_seconds": 300, "view_count": 8, "last_view_time": "2024-12-01 16:30:00"}', '2024-11-26 16:30:00'),
(2, 12, '{"duration_seconds": 180, "view_count": 4, "last_view_time": "2024-12-01 10:00:00"}', '2024-11-27 10:00:00'),
(2, 13, '{"duration_seconds": 220, "view_count": 5, "last_view_time": "2024-12-01 13:20:00"}', '2024-11-28 13:20:00'),
(2, 14, '{"duration_seconds": 160, "view_count": 3, "last_view_time": "2024-12-01 17:45:00"}', '2024-11-29 17:45:00'),

-- 用户3的浏览记录
(3, 5, '{"duration_seconds": 100, "view_count": 2, "last_view_time": "2024-11-30 12:00:00"}', '2024-11-30 12:00:00'),
(3, 6, '{"duration_seconds": 140, "view_count": 3, "last_view_time": "2024-12-01 08:30:00"}', '2024-11-28 08:30:00'),
(3, 15, '{"duration_seconds": 190, "view_count": 4, "last_view_time": "2024-12-01 14:00:00"}', '2024-11-29 14:00:00'),
(3, 16, '{"duration_seconds": 110, "view_count": 2, "last_view_time": "2024-12-01 09:45:00"}', '2024-11-30 09:45:00'),

-- 用户4的浏览记录
(4, 7, '{"duration_seconds": 250, "view_count": 7, "last_view_time": "2024-12-01 11:00:00"}', '2024-11-26 11:00:00'),
(4, 8, '{"duration_seconds": 170, "view_count": 4, "last_view_time": "2024-12-01 15:30:00"}', '2024-11-27 15:30:00'),
(4, 17, '{"duration_seconds": 210, "view_count": 5, "last_view_time": "2024-12-01 10:20:00"}', '2024-11-28 10:20:00'),
(4, 18, '{"duration_seconds": 130, "view_count": 3, "last_view_time": "2024-12-01 16:00:00"}', '2024-11-29 16:00:00'),

-- 用户5的浏览记录
(5, 9, '{"duration_seconds": 280, "view_count": 9, "last_view_time": "2024-12-01 13:00:00"}', '2024-11-25 13:00:00'),
(5, 19, '{"duration_seconds": 200, "view_count": 6, "last_view_time": "2024-12-01 14:30:00"}', '2024-11-26 14:30:00'),
(5, 20, '{"duration_seconds": 150, "view_count": 3, "last_view_time": "2024-12-01 11:45:00"}', '2024-11-27 11:45:00'),

-- 用户6的浏览记录
(6, 10, '{"duration_seconds": 320, "view_count": 10, "last_view_time": "2024-12-01 09:00:00"}', '2024-11-24 09:00:00'),
(6, 11, '{"duration_seconds": 290, "view_count": 8, "last_view_time": "2024-12-01 15:00:00"}', '2024-11-25 15:00:00'),
(6, 21, '{"duration_seconds": 240, "view_count": 7, "last_view_time": "2024-12-01 12:30:00"}', '2024-11-26 12:30:00'),
(6, 22, '{"duration_seconds": 180, "view_count": 5, "last_view_time": "2024-12-01 17:00:00"}', '2024-11-27 17:00:00'),

-- 用户7的浏览记录
(7, 12, '{"duration_seconds": 120, "view_count": 3, "last_view_time": "2024-12-01 10:15:00"}', '2024-11-28 10:15:00'),
(7, 13, '{"duration_seconds": 160, "view_count": 4, "last_view_time": "2024-12-01 14:45:00"}', '2024-11-29 14:45:00'),
(7, 23, '{"duration_seconds": 140, "view_count": 3, "last_view_time": "2024-12-01 11:30:00"}', '2024-11-30 11:30:00'),

-- 用户8的浏览记录
(8, 14, '{"duration_seconds": 220, "view_count": 6, "last_view_time": "2024-12-01 13:15:00"}', '2024-11-26 13:15:00'),
(8, 15, '{"duration_seconds": 190, "view_count": 5, "last_view_time": "2024-12-01 16:30:00"}', '2024-11-27 16:30:00'),
(8, 24, '{"duration_seconds": 170, "view_count": 4, "last_view_time": "2024-12-01 09:30:00"}', '2024-11-28 09:30:00'),

-- 用户9的浏览记录
(9, 16, '{"duration_seconds": 260, "view_count": 7, "last_view_time": "2024-12-01 12:00:00"}', '2024-11-25 12:00:00'),
(9, 17, '{"duration_seconds": 230, "view_count": 6, "last_view_time": "2024-12-01 15:45:00"}', '2024-11-26 15:45:00'),
(9, 25, '{"duration_seconds": 200, "view_count": 5, "last_view_time": "2024-12-01 10:45:00"}', '2024-11-27 10:45:00'),

-- 用户10的浏览记录
(10, 18, '{"duration_seconds": 180, "view_count": 4, "last_view_time": "2024-12-01 14:15:00"}', '2024-11-28 14:15:00'),
(10, 19, '{"duration_seconds": 150, "view_count": 3, "last_view_time": "2024-12-01 17:30:00"}', '2024-11-29 17:30:00'),
(10, 26, '{"duration_seconds": 210, "view_count": 5, "last_view_time": "2024-12-01 11:00:00"}', '2024-11-30 11:00:00');

