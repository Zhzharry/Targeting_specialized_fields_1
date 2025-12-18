-- ============================================
-- 用户相似度表数据注入脚本
-- ============================================

USE `bigdata`;

-- 生成用户之间的相似度数据（基于协同过滤算法）
INSERT INTO `user_similarity` (`user_id1`, `user_id2`, `similarity_data`, `created_at`, `updated_at`) VALUES
-- 用户1与其他用户的相似度
(1, 2, '{"similarity_score": 0.65, "algorithm_version": "v1.0", "common_items": 5, "calculation_method": "cosine"}', NOW(), NOW()),
(1, 4, '{"similarity_score": 0.72, "algorithm_version": "v1.0", "common_items": 6, "calculation_method": "cosine"}', NOW(), NOW()),
(1, 8, '{"similarity_score": 0.58, "algorithm_version": "v1.0", "common_items": 4, "calculation_method": "cosine"}', NOW(), NOW()),

-- 用户2与其他用户的相似度
(2, 3, '{"similarity_score": 0.55, "algorithm_version": "v1.0", "common_items": 3, "calculation_method": "cosine"}', NOW(), NOW()),
(2, 5, '{"similarity_score": 0.78, "algorithm_version": "v1.0", "common_items": 7, "calculation_method": "cosine"}', NOW(), NOW()),
(2, 9, '{"similarity_score": 0.68, "algorithm_version": "v1.0", "common_items": 6, "calculation_method": "cosine"}', NOW(), NOW()),

-- 用户3与其他用户的相似度
(3, 7, '{"similarity_score": 0.82, "algorithm_version": "v1.0", "common_items": 5, "calculation_method": "cosine"}', NOW(), NOW()),
(3, 10, '{"similarity_score": 0.75, "algorithm_version": "v1.0", "common_items": 4, "calculation_method": "cosine"}', NOW(), NOW()),

-- 用户4与其他用户的相似度
(4, 5, '{"similarity_score": 0.61, "algorithm_version": "v1.0", "common_items": 5, "calculation_method": "cosine"}', NOW(), NOW()),
(4, 8, '{"similarity_score": 0.69, "algorithm_version": "v1.0", "common_items": 6, "calculation_method": "cosine"}', NOW(), NOW()),

-- 用户5与其他用户的相似度
(5, 6, '{"similarity_score": 0.71, "algorithm_version": "v1.0", "common_items": 6, "calculation_method": "cosine"}', NOW(), NOW()),
(5, 9, '{"similarity_score": 0.85, "algorithm_version": "v1.0", "common_items": 8, "calculation_method": "cosine"}', NOW(), NOW()),

-- 用户6与其他用户的相似度
(6, 9, '{"similarity_score": 0.66, "algorithm_version": "v1.0", "common_items": 5, "calculation_method": "cosine"}', NOW(), NOW()),

-- 用户7与其他用户的相似度
(7, 10, '{"similarity_score": 0.88, "algorithm_version": "v1.0", "common_items": 6, "calculation_method": "cosine"}', NOW(), NOW()),

-- 用户8与其他用户的相似度
(8, 9, '{"similarity_score": 0.59, "algorithm_version": "v1.0", "common_items": 4, "calculation_method": "cosine"}', NOW(), NOW()),

-- 用户9与其他用户的相似度
(9, 10, '{"similarity_score": 0.52, "algorithm_version": "v1.0", "common_items": 3, "calculation_method": "cosine"}', NOW(), NOW());

