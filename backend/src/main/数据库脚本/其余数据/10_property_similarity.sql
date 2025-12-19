-- ============================================
-- 房源相似度表数据注入脚本
-- ============================================

USE `bigdata`;

-- 生成房源之间的相似度数据（基于内容相似度算法）
INSERT INTO `property_similarity` (`property_id1`, `property_id2`, `similarity_data`, `created_at`, `updated_at`) VALUES
-- 相似房源对（基于位置、户型、价格等特征）
(1, 2, '{"similarity_score": 0.82, "feature_weights": {"location": 0.3, "layout": 0.25, "price": 0.2, "decoration": 0.15, "community": 0.1}, "algorithm_version": "v1.0"}', NOW(), NOW()),
(1, 10, '{"similarity_score": 0.75, "feature_weights": {"location": 0.3, "layout": 0.25, "price": 0.2, "decoration": 0.15, "community": 0.1}, "algorithm_version": "v1.0"}', NOW(), NOW()),
(1, 11, '{"similarity_score": 0.68, "feature_weights": {"location": 0.3, "layout": 0.25, "price": 0.2, "decoration": 0.15, "community": 0.1}, "algorithm_version": "v1.0"}', NOW(), NOW()),

(2, 12, '{"similarity_score": 0.79, "feature_weights": {"location": 0.3, "layout": 0.25, "price": 0.2, "decoration": 0.15, "community": 0.1}, "algorithm_version": "v1.0"}', NOW(), NOW()),
(2, 13, '{"similarity_score": 0.71, "feature_weights": {"location": 0.3, "layout": 0.25, "price": 0.2, "decoration": 0.15, "community": 0.1}, "algorithm_version": "v1.0"}', NOW(), NOW()),

(3, 4, '{"similarity_score": 0.88, "feature_weights": {"location": 0.3, "layout": 0.25, "price": 0.2, "decoration": 0.15, "community": 0.1}, "algorithm_version": "v1.0"}', NOW(), NOW()),
(3, 11, '{"similarity_score": 0.76, "feature_weights": {"location": 0.3, "layout": 0.25, "price": 0.2, "decoration": 0.15, "community": 0.1}, "algorithm_version": "v1.0"}', NOW(), NOW()),
(3, 19, '{"similarity_score": 0.73, "feature_weights": {"location": 0.3, "layout": 0.25, "price": 0.2, "decoration": 0.15, "community": 0.1}, "algorithm_version": "v1.0"}', NOW(), NOW()),

(4, 5, '{"similarity_score": 0.85, "feature_weights": {"location": 0.3, "layout": 0.25, "price": 0.2, "decoration": 0.15, "community": 0.1}, "algorithm_version": "v1.0"}', NOW(), NOW()),
(4, 12, '{"similarity_score": 0.77, "feature_weights": {"location": 0.3, "layout": 0.25, "price": 0.2, "decoration": 0.15, "community": 0.1}, "algorithm_version": "v1.0"}', NOW(), NOW()),

(5, 6, '{"similarity_score": 0.81, "feature_weights": {"location": 0.3, "layout": 0.25, "price": 0.2, "decoration": 0.15, "community": 0.1}, "algorithm_version": "v1.0"}', NOW(), NOW()),
(5, 13, '{"similarity_score": 0.74, "feature_weights": {"location": 0.3, "layout": 0.25, "price": 0.2, "decoration": 0.15, "community": 0.1}, "algorithm_version": "v1.0"}', NOW(), NOW()),

(7, 8, '{"similarity_score": 0.83, "feature_weights": {"location": 0.3, "layout": 0.25, "price": 0.2, "decoration": 0.15, "community": 0.1}, "algorithm_version": "v1.0"}', NOW(), NOW()),
(7, 17, '{"similarity_score": 0.72, "feature_weights": {"location": 0.3, "layout": 0.25, "price": 0.2, "decoration": 0.15, "community": 0.1}, "algorithm_version": "v1.0"}', NOW(), NOW()),

(9, 10, '{"similarity_score": 0.86, "feature_weights": {"location": 0.3, "layout": 0.25, "price": 0.2, "decoration": 0.15, "community": 0.1}, "algorithm_version": "v1.0"}', NOW(), NOW()),
(9, 20, '{"similarity_score": 0.78, "feature_weights": {"location": 0.3, "layout": 0.25, "price": 0.2, "decoration": 0.15, "community": 0.1}, "algorithm_version": "v1.0"}', NOW(), NOW()),

(11, 12, '{"similarity_score": 0.84, "feature_weights": {"location": 0.3, "layout": 0.25, "price": 0.2, "decoration": 0.15, "community": 0.1}, "algorithm_version": "v1.0"}', NOW(), NOW()),
(11, 21, '{"similarity_score": 0.75, "feature_weights": {"location": 0.3, "layout": 0.25, "price": 0.2, "decoration": 0.15, "community": 0.1}, "algorithm_version": "v1.0"}', NOW(), NOW()),

(13, 14, '{"similarity_score": 0.80, "feature_weights": {"location": 0.3, "layout": 0.25, "price": 0.2, "decoration": 0.15, "community": 0.1}, "algorithm_version": "v1.0"}', NOW(), NOW()),
(13, 23, '{"similarity_score": 0.69, "feature_weights": {"location": 0.3, "layout": 0.25, "price": 0.2, "decoration": 0.15, "community": 0.1}, "algorithm_version": "v1.0"}', NOW(), NOW()),

(15, 16, '{"similarity_score": 0.87, "feature_weights": {"location": 0.3, "layout": 0.25, "price": 0.2, "decoration": 0.15, "community": 0.1}, "algorithm_version": "v1.0"}', NOW(), NOW()),
(15, 24, '{"similarity_score": 0.76, "feature_weights": {"location": 0.3, "layout": 0.25, "price": 0.2, "decoration": 0.15, "community": 0.1}, "algorithm_version": "v1.0"}', NOW(), NOW()),

(17, 18, '{"similarity_score": 0.82, "feature_weights": {"location": 0.3, "layout": 0.25, "price": 0.2, "decoration": 0.15, "community": 0.1}, "algorithm_version": "v1.0"}', NOW(), NOW()),
(17, 25, '{"similarity_score": 0.73, "feature_weights": {"location": 0.3, "layout": 0.25, "price": 0.2, "decoration": 0.15, "community": 0.1}, "algorithm_version": "v1.0"}', NOW(), NOW()),

(19, 20, '{"similarity_score": 0.85, "feature_weights": {"location": 0.3, "layout": 0.25, "price": 0.2, "decoration": 0.15, "community": 0.1}, "algorithm_version": "v1.0"}', NOW(), NOW()),
(19, 26, '{"similarity_score": 0.74, "feature_weights": {"location": 0.3, "layout": 0.25, "price": 0.2, "decoration": 0.15, "community": 0.1}, "algorithm_version": "v1.0"}', NOW(), NOW()),

(21, 22, '{"similarity_score": 0.88, "feature_weights": {"location": 0.3, "layout": 0.25, "price": 0.2, "decoration": 0.15, "community": 0.1}, "algorithm_version": "v1.0"}', NOW(), NOW()),

(23, 24, '{"similarity_score": 0.81, "feature_weights": {"location": 0.3, "layout": 0.25, "price": 0.2, "decoration": 0.15, "community": 0.1}, "algorithm_version": "v1.0"}', NOW(), NOW()),

(25, 26, '{"similarity_score": 0.79, "feature_weights": {"location": 0.3, "layout": 0.25, "price": 0.2, "decoration": 0.15, "community": 0.1}, "algorithm_version": "v1.0"}', NOW(), NOW());

