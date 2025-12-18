-- ============================================
-- 用户推荐表数据注入脚本
-- ============================================

USE `bigdata`;

-- 为不同用户生成推荐记录
INSERT INTO `user_recommendations` (`user_id`, `property_id`, `recommendation_data`, `created_at`) VALUES
-- 用户1的推荐
(1, 10, '{"type": "content_based", "score": 0.85, "reason": "基于您的浏览和偏好匹配", "is_viewed": false}', '2024-12-01 10:00:00'),
(1, 11, '{"type": "collaborative", "score": 0.78, "reason": "相似用户也喜欢", "is_viewed": false}', '2024-12-01 10:00:00'),
(1, 12, '{"type": "popular", "score": 0.72, "reason": "热门房源", "is_viewed": false}', '2024-12-01 10:00:00'),

-- 用户2的推荐
(2, 3, '{"type": "content_based", "score": 0.92, "reason": "完美匹配您的偏好", "is_viewed": false}', '2024-12-01 11:00:00'),
(2, 13, '{"type": "collaborative", "score": 0.81, "reason": "相似用户推荐", "is_viewed": false}', '2024-12-01 11:00:00'),
(2, 14, '{"type": "content_based", "score": 0.75, "reason": "符合您的搜索条件", "is_viewed": false}', '2024-12-01 11:00:00'),
(2, 15, '{"type": "popular", "score": 0.68, "reason": "热门推荐", "is_viewed": false}', '2024-12-01 11:00:00'),

-- 用户3的推荐
(3, 5, '{"type": "content_based", "score": 0.88, "reason": "价格和户型符合需求", "is_viewed": false}', '2024-12-01 12:00:00'),
(3, 15, '{"type": "collaborative", "score": 0.76, "reason": "相似用户选择", "is_viewed": false}', '2024-12-01 12:00:00'),
(3, 16, '{"type": "content_based", "score": 0.71, "reason": "性价比高", "is_viewed": false}', '2024-12-01 12:00:00'),

-- 用户4的推荐
(4, 7, '{"type": "content_based", "score": 0.86, "reason": "精装修符合要求", "is_viewed": false}', '2024-12-01 13:00:00'),
(4, 17, '{"type": "collaborative", "score": 0.79, "reason": "相似用户推荐", "is_viewed": false}', '2024-12-01 13:00:00'),
(4, 18, '{"type": "content_based", "score": 0.73, "reason": "位置和价格合适", "is_viewed": false}', '2024-12-01 13:00:00'),

-- 用户5的推荐
(5, 9, '{"type": "content_based", "score": 0.91, "reason": "大户型学区房", "is_viewed": false}', '2024-12-01 14:00:00'),
(5, 19, '{"type": "collaborative", "score": 0.84, "reason": "相似用户也关注", "is_viewed": false}', '2024-12-01 14:00:00'),
(5, 20, '{"type": "content_based", "score": 0.77, "reason": "新房精装修", "is_viewed": false}', '2024-12-01 14:00:00'),

-- 用户6的推荐
(6, 10, '{"type": "content_based", "score": 0.93, "reason": "豪华装修海景房", "is_viewed": false}', '2024-12-01 15:00:00'),
(6, 11, '{"type": "collaborative", "score": 0.87, "reason": "高端用户推荐", "is_viewed": false}', '2024-12-01 15:00:00'),
(6, 21, '{"type": "content_based", "score": 0.82, "reason": "位置优越", "is_viewed": false}', '2024-12-01 15:00:00'),
(6, 22, '{"type": "popular", "score": 0.75, "reason": "热门高端房源", "is_viewed": false}', '2024-12-01 15:00:00'),

-- 用户7的推荐
(7, 12, '{"type": "content_based", "score": 0.83, "reason": "地铁口小户型", "is_viewed": false}', '2024-12-01 16:00:00'),
(7, 13, '{"type": "collaborative", "score": 0.74, "reason": "相似用户选择", "is_viewed": false}', '2024-12-01 16:00:00'),
(7, 23, '{"type": "content_based", "score": 0.69, "reason": "价格合适", "is_viewed": false}', '2024-12-01 16:00:00'),

-- 用户8的推荐
(8, 14, '{"type": "content_based", "score": 0.88, "reason": "中心区精装修", "is_viewed": false}', '2024-12-01 17:00:00'),
(8, 15, '{"type": "collaborative", "score": 0.80, "reason": "相似用户推荐", "is_viewed": false}', '2024-12-01 17:00:00'),
(8, 24, '{"type": "content_based", "score": 0.72, "reason": "性价比高", "is_viewed": false}', '2024-12-01 17:00:00'),

-- 用户9的推荐
(9, 16, '{"type": "content_based", "score": 0.90, "reason": "大户型学区房", "is_viewed": false}', '2024-12-01 18:00:00'),
(9, 17, '{"type": "collaborative", "score": 0.83, "reason": "相似用户也喜欢", "is_viewed": false}', '2024-12-01 18:00:00'),
(9, 25, '{"type": "content_based", "score": 0.76, "reason": "新房符合需求", "is_viewed": false}', '2024-12-01 18:00:00'),

-- 用户10的推荐
(10, 18, '{"type": "content_based", "score": 0.81, "reason": "价格便宜", "is_viewed": false}', '2024-12-01 19:00:00'),
(10, 19, '{"type": "collaborative", "score": 0.73, "reason": "相似用户选择", "is_viewed": false}', '2024-12-01 19:00:00'),
(10, 26, '{"type": "content_based", "score": 0.67, "reason": "性价比高", "is_viewed": false}', '2024-12-01 19:00:00');

