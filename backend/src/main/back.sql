/*
SQLyog Ultimate v12.09 (64 bit)
MySQL - 8.0.39 : Database - bigdata
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
CREATE DATABASE /*!32312 IF NOT EXISTS*/`bigdata` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `bigdata`;

/*Table structure for table `browsing_history` */

DROP TABLE IF EXISTS `browsing_history`;

CREATE TABLE `browsing_history` (
  `history_id` int NOT NULL AUTO_INCREMENT COMMENT '历史ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `property_id` int NOT NULL COMMENT '房源ID',
  `behavior_data` json DEFAULT NULL COMMENT '行为数据（浏览时长、次数）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
  PRIMARY KEY (`history_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_property_id` (`property_id`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `browsing_history_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `browsing_history_ibfk_2` FOREIGN KEY (`property_id`) REFERENCES `properties` (`property_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='浏览历史表';

/*Data for the table `browsing_history` */

/*Table structure for table `communities` */

DROP TABLE IF EXISTS `communities`;

CREATE TABLE `communities` (
  `community_id` int NOT NULL AUTO_INCREMENT COMMENT '小区ID',
  `name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '小区名称',
  `basic_info` json DEFAULT NULL COMMENT '基本信息（建成年份、开发商）',
  `location_info` json DEFAULT NULL COMMENT '位置信息（省市区、坐标）',
  `facility_info` json DEFAULT NULL COMMENT '设施信息（物业费、学校）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`community_id`),
  KEY `idx_name` (`name`),
  KEY `idx_city` ((cast(json_unquote(json_extract(`location_info`,_utf8mb3'$.city')) as char(50) charset utf8mb3))),
  KEY `idx_district` ((cast(json_unquote(json_extract(`location_info`,_utf8mb3'$.district')) as char(50) charset utf8mb3)))
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小区信息表';

/*Data for the table `communities` */

insert  into `communities`(`community_id`,`name`,`basic_info`,`location_info`,`facility_info`,`created_at`) values (1,'万科城市花园','{\"developer\": \"万科\", \"build_year\": 2015, \"total_size\": 50000, \"total_households\": 800, \"management_company\": \"万科物业\"}','{\"city\": \"深圳市\", \"address\": \"科技园路123号\", \"district\": \"南山区\", \"latitude\": 22.54321, \"province\": \"广东省\", \"longitude\": 114.123456}','{\"schools\": [\"深圳小学\", \"实验中学\"], \"plot_ratio\": 2.5, \"green_ratio\": 0.35, \"management_fee\": 3.5, \"parking_spaces\": 1000}','2025-12-01 20:26:18'),(2,'华润城','{\"developer\": \"华润\", \"build_year\": 2018, \"total_size\": 80000, \"total_households\": 1200, \"management_company\": \"华润物业\"}','{\"city\": \"深圳市\", \"address\": \"深南大道456号\", \"district\": \"福田区\", \"latitude\": 22.543, \"province\": \"广东省\", \"longitude\": 114.056789}','{\"schools\": [\"福田小学\", \"红岭中学\"], \"plot_ratio\": 3.0, \"green_ratio\": 0.4, \"management_fee\": 4.0, \"parking_spaces\": 1500}','2025-12-01 20:26:18'),(3,'万科城市花园','{\"developer\": \"万科\", \"build_year\": 2015, \"total_size\": 50000, \"total_households\": 800, \"management_company\": \"万科物业\"}','{\"city\": \"深圳市\", \"address\": \"科技园路123号\", \"district\": \"南山区\", \"latitude\": 22.54321, \"province\": \"广东省\", \"longitude\": 114.123456}','{\"schools\": [\"深圳小学\", \"实验中学\"], \"plot_ratio\": 2.5, \"green_ratio\": 0.35, \"management_fee\": 3.5, \"parking_spaces\": 1000}','2025-12-01 20:27:21'),(4,'华润城','{\"developer\": \"华润\", \"build_year\": 2018, \"total_size\": 80000, \"total_households\": 1200, \"management_company\": \"华润物业\"}','{\"city\": \"深圳市\", \"address\": \"深南大道456号\", \"district\": \"福田区\", \"latitude\": 22.543, \"province\": \"广东省\", \"longitude\": 114.056789}','{\"schools\": [\"福田小学\", \"红岭中学\"], \"plot_ratio\": 3.0, \"green_ratio\": 0.4, \"management_fee\": 4.0, \"parking_spaces\": 1500}','2025-12-01 20:27:21'),(5,'万科城市花园','{\"developer\": \"万科\", \"build_year\": 2015, \"total_size\": 50000, \"total_households\": 800, \"management_company\": \"万科物业\"}','{\"city\": \"深圳市\", \"address\": \"科技园路123号\", \"district\": \"南山区\", \"latitude\": 22.54321, \"province\": \"广东省\", \"longitude\": 114.123456}','{\"schools\": [\"深圳小学\", \"实验中学\"], \"plot_ratio\": 2.5, \"green_ratio\": 0.35, \"management_fee\": 3.5, \"parking_spaces\": 1000}','2025-12-01 20:27:51'),(6,'华润城','{\"developer\": \"华润\", \"build_year\": 2018, \"total_size\": 80000, \"total_households\": 1200, \"management_company\": \"华润物业\"}','{\"city\": \"深圳市\", \"address\": \"深南大道456号\", \"district\": \"福田区\", \"latitude\": 22.543, \"province\": \"广东省\", \"longitude\": 114.056789}','{\"schools\": [\"福田小学\", \"红岭中学\"], \"plot_ratio\": 3.0, \"green_ratio\": 0.4, \"management_fee\": 4.0, \"parking_spaces\": 1500}','2025-12-01 20:27:51');

/*Table structure for table `favorites` */

DROP TABLE IF EXISTS `favorites`;

CREATE TABLE `favorites` (
  `favorite_id` int NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `property_id` int NOT NULL COMMENT '房源ID',
  `favorite_data` json DEFAULT NULL COMMENT '收藏备注信息',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`favorite_id`),
  UNIQUE KEY `uk_user_property` (`user_id`,`property_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_property_id` (`property_id`),
  CONSTRAINT `favorites_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `favorites_ibfk_2` FOREIGN KEY (`property_id`) REFERENCES `properties` (`property_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

/*Data for the table `favorites` */

/*Table structure for table `properties` */

DROP TABLE IF EXISTS `properties`;

CREATE TABLE `properties` (
  `property_id` int NOT NULL AUTO_INCREMENT COMMENT '房源ID',
  `community_id` int NOT NULL COMMENT '小区ID',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '房源标题',
  `basic_info` json DEFAULT NULL COMMENT '基本信息（房型、建造年份、装修）',
  `price_info` json DEFAULT NULL COMMENT '价格信息（总价、单价、历史价格）',
  `layout_info` json DEFAULT NULL COMMENT '户型信息（几室几厅、面积、楼层、朝向）',
  `status` enum('for_sale','sold') COLLATE utf8mb4_unicode_ci DEFAULT 'for_sale' COMMENT '房源状态',
  `view_count` int DEFAULT '0' COMMENT '浏览次数',
  `favorite_count` int DEFAULT '0' COMMENT '收藏次数',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`property_id`),
  KEY `idx_community_id` (`community_id`),
  KEY `idx_status` (`status`),
  KEY `idx_price` ((cast(json_unquote(json_extract(`price_info`,_utf8mb3'$.total_price')) as decimal(12,2)))),
  KEY `idx_bedroom` ((cast(json_unquote(json_extract(`layout_info`,_utf8mb3'$.bedroom_count')) as unsigned))),
  CONSTRAINT `properties_ibfk_1` FOREIGN KEY (`community_id`) REFERENCES `communities` (`community_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='房源表';

/*Data for the table `properties` */

insert  into `properties`(`property_id`,`community_id`,`title`,`basic_info`,`price_info`,`layout_info`,`status`,`view_count`,`favorite_count`,`created_at`,`updated_at`) values (1,1,'万科城市花园 精装三房 南向采光好','{\"build_year\": 2018, \"decoration\": \"hard\", \"property_type\": \"apartment\"}','{\"unit_price\": 85000, \"total_price\": 650.5, \"price_history\": [{\"date\": \"2024-01-01\", \"price\": 680}]}','{\"area\": 89.5, \"floor\": 15, \"orientation\": \"south\", \"total_floors\": 28, \"bedroom_count\": 3, \"bathroom_count\": 2, \"living_room_count\": 2}','for_sale',0,0,'2025-12-01 20:26:18','2025-12-01 20:26:18'),(2,1,'万科城市花园 豪华两房 地铁口','{\"build_year\": 2018, \"decoration\": \"luxury\", \"property_type\": \"apartment\"}','{\"unit_price\": 82000, \"total_price\": 480.0, \"price_history\": [{\"date\": \"2024-02-01\", \"price\": 490}]}','{\"area\": 68.0, \"floor\": 10, \"orientation\": \"southeast\", \"total_floors\": 28, \"bedroom_count\": 2, \"bathroom_count\": 1, \"living_room_count\": 1}','for_sale',0,0,'2025-12-01 20:26:18','2025-12-01 20:26:18'),(3,2,'华润城 四房大户型 学区房','{\"build_year\": 2019, \"decoration\": \"hard\", \"property_type\": \"apartment\"}','{\"unit_price\": 90000, \"total_price\": 980.0, \"price_history\": [{\"date\": \"2024-03-01\", \"price\": 1000}]}','{\"area\": 128.0, \"floor\": 20, \"orientation\": \"south\", \"total_floors\": 35, \"bedroom_count\": 4, \"bathroom_count\": 3, \"living_room_count\": 2}','for_sale',0,0,'2025-12-01 20:26:18','2025-12-01 20:26:18'),(4,1,'万科城市花园 精装三房 南向采光好','{\"build_year\": 2018, \"decoration\": \"hard\", \"property_type\": \"apartment\"}','{\"unit_price\": 85000, \"total_price\": 650.5, \"price_history\": [{\"date\": \"2024-01-01\", \"price\": 680}]}','{\"area\": 89.5, \"floor\": 15, \"orientation\": \"south\", \"total_floors\": 28, \"bedroom_count\": 3, \"bathroom_count\": 2, \"living_room_count\": 2}','for_sale',0,0,'2025-12-01 20:27:21','2025-12-01 20:27:21'),(5,1,'万科城市花园 豪华两房 地铁口','{\"build_year\": 2018, \"decoration\": \"luxury\", \"property_type\": \"apartment\"}','{\"unit_price\": 82000, \"total_price\": 480.0, \"price_history\": [{\"date\": \"2024-02-01\", \"price\": 490}]}','{\"area\": 68.0, \"floor\": 10, \"orientation\": \"southeast\", \"total_floors\": 28, \"bedroom_count\": 2, \"bathroom_count\": 1, \"living_room_count\": 1}','for_sale',0,0,'2025-12-01 20:27:21','2025-12-01 20:27:21'),(6,2,'华润城 四房大户型 学区房','{\"build_year\": 2019, \"decoration\": \"hard\", \"property_type\": \"apartment\"}','{\"unit_price\": 90000, \"total_price\": 980.0, \"price_history\": [{\"date\": \"2024-03-01\", \"price\": 1000}]}','{\"area\": 128.0, \"floor\": 20, \"orientation\": \"south\", \"total_floors\": 35, \"bedroom_count\": 4, \"bathroom_count\": 3, \"living_room_count\": 2}','for_sale',0,0,'2025-12-01 20:27:21','2025-12-01 20:27:21'),(7,1,'万科城市花园 精装三房 南向采光好','{\"build_year\": 2018, \"decoration\": \"hard\", \"property_type\": \"apartment\"}','{\"unit_price\": 85000, \"total_price\": 650.5, \"price_history\": [{\"date\": \"2024-01-01\", \"price\": 680}]}','{\"area\": 89.5, \"floor\": 15, \"orientation\": \"south\", \"total_floors\": 28, \"bedroom_count\": 3, \"bathroom_count\": 2, \"living_room_count\": 2}','for_sale',0,0,'2025-12-01 20:27:51','2025-12-01 20:27:51'),(8,1,'万科城市花园 豪华两房 地铁口','{\"build_year\": 2018, \"decoration\": \"luxury\", \"property_type\": \"apartment\"}','{\"unit_price\": 82000, \"total_price\": 480.0, \"price_history\": [{\"date\": \"2024-02-01\", \"price\": 490}]}','{\"area\": 68.0, \"floor\": 10, \"orientation\": \"southeast\", \"total_floors\": 28, \"bedroom_count\": 2, \"bathroom_count\": 1, \"living_room_count\": 1}','for_sale',0,0,'2025-12-01 20:27:51','2025-12-01 20:27:51'),(9,2,'华润城 四房大户型 学区房','{\"build_year\": 2019, \"decoration\": \"hard\", \"property_type\": \"apartment\"}','{\"unit_price\": 90000, \"total_price\": 980.0, \"price_history\": [{\"date\": \"2024-03-01\", \"price\": 1000}]}','{\"area\": 128.0, \"floor\": 20, \"orientation\": \"south\", \"total_floors\": 35, \"bedroom_count\": 4, \"bathroom_count\": 3, \"living_room_count\": 2}','for_sale',0,0,'2025-12-01 20:27:51','2025-12-01 20:27:51');

/*Table structure for table `property_features` */

DROP TABLE IF EXISTS `property_features`;

CREATE TABLE `property_features` (
  `feature_id` int NOT NULL AUTO_INCREMENT COMMENT '特征ID',
  `property_id` int NOT NULL COMMENT '房源ID',
  `feature_data` json DEFAULT NULL COMMENT '特征数据',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`feature_id`),
  KEY `idx_property_id` (`property_id`),
  KEY `idx_feature_type` ((cast(json_unquote(json_extract(`feature_data`,_utf8mb3'$.feature_type')) as char(50) charset utf8mb3))),
  CONSTRAINT `property_features_ibfk_1` FOREIGN KEY (`property_id`) REFERENCES `properties` (`property_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='房源特征表';

/*Data for the table `property_features` */

insert  into `property_features`(`feature_id`,`property_id`,`feature_data`,`created_at`) values (1,1,'{\"description\": \"靠近地铁站\", \"feature_name\": \"near_subway\", \"feature_type\": \"facility\", \"feature_value\": true, \"feature_weight\": 0.8}','2025-12-01 20:26:18'),(2,1,'{\"description\": \"学区房\", \"feature_name\": \"near_school\", \"feature_type\": \"facility\", \"feature_value\": true, \"feature_weight\": 0.9}','2025-12-01 20:26:18'),(3,3,'{\"description\": \"重点学区房\", \"feature_name\": \"near_school\", \"feature_type\": \"facility\", \"feature_value\": true, \"feature_weight\": 0.9}','2025-12-01 20:26:18'),(4,1,'{\"description\": \"靠近地铁站\", \"feature_name\": \"near_subway\", \"feature_type\": \"facility\", \"feature_value\": true, \"feature_weight\": 0.8}','2025-12-01 20:27:21'),(5,1,'{\"description\": \"学区房\", \"feature_name\": \"near_school\", \"feature_type\": \"facility\", \"feature_value\": true, \"feature_weight\": 0.9}','2025-12-01 20:27:21'),(6,3,'{\"description\": \"重点学区房\", \"feature_name\": \"near_school\", \"feature_type\": \"facility\", \"feature_value\": true, \"feature_weight\": 0.9}','2025-12-01 20:27:21'),(7,1,'{\"description\": \"靠近地铁站\", \"feature_name\": \"near_subway\", \"feature_type\": \"facility\", \"feature_value\": true, \"feature_weight\": 0.8}','2025-12-01 20:27:51'),(8,1,'{\"description\": \"学区房\", \"feature_name\": \"near_school\", \"feature_type\": \"facility\", \"feature_value\": true, \"feature_weight\": 0.9}','2025-12-01 20:27:51'),(9,3,'{\"description\": \"重点学区房\", \"feature_name\": \"near_school\", \"feature_type\": \"facility\", \"feature_value\": true, \"feature_weight\": 0.9}','2025-12-01 20:27:51');

/*Table structure for table `property_similarity` */

DROP TABLE IF EXISTS `property_similarity`;

CREATE TABLE `property_similarity` (
  `similarity_id` int NOT NULL AUTO_INCREMENT COMMENT '相似度ID',
  `property_id1` int NOT NULL COMMENT '房源1ID',
  `property_id2` int NOT NULL COMMENT '房源2ID',
  `similarity_data` json DEFAULT NULL COMMENT '相似度数据（分数、特征权重）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '计算时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`similarity_id`),
  UNIQUE KEY `uk_property_pair` (`property_id1`,`property_id2`),
  KEY `idx_property1` (`property_id1`),
  KEY `idx_property2` (`property_id2`),
  KEY `idx_score` ((cast(json_unquote(json_extract(`similarity_data`,_utf8mb3'$.similarity_score')) as decimal(5,4)))),
  CONSTRAINT `property_similarity_ibfk_1` FOREIGN KEY (`property_id1`) REFERENCES `properties` (`property_id`) ON DELETE CASCADE,
  CONSTRAINT `property_similarity_ibfk_2` FOREIGN KEY (`property_id2`) REFERENCES `properties` (`property_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='房源相似度表';

/*Data for the table `property_similarity` */

/*Table structure for table `search_history` */

DROP TABLE IF EXISTS `search_history`;

CREATE TABLE `search_history` (
  `search_id` int NOT NULL AUTO_INCREMENT COMMENT '搜索ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `search_data` json DEFAULT NULL COMMENT '搜索数据（关键词、筛选条件）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间',
  PRIMARY KEY (`search_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `search_history_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='搜索历史表';

/*Data for the table `search_history` */

/*Table structure for table `transaction_records` */

DROP TABLE IF EXISTS `transaction_records`;

CREATE TABLE `transaction_records` (
  `record_id` int NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `property_id` int DEFAULT NULL COMMENT '房源ID',
  `community_id` int DEFAULT NULL COMMENT '小区ID',
  `transaction_data` json DEFAULT NULL COMMENT '成交数据（日期、价格等）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_property_id` (`property_id`),
  KEY `idx_community_id` (`community_id`),
  KEY `idx_transaction_date` ((cast(json_unquote(json_extract(`transaction_data`,_utf8mb3'$.transaction_date')) as date))),
  KEY `idx_city` ((cast(json_unquote(json_extract(`transaction_data`,_utf8mb3'$.city')) as char(50) charset utf8mb3))),
  KEY `idx_district` ((cast(json_unquote(json_extract(`transaction_data`,_utf8mb3'$.district')) as char(50) charset utf8mb3))),
  CONSTRAINT `transaction_records_ibfk_1` FOREIGN KEY (`property_id`) REFERENCES `properties` (`property_id`) ON DELETE SET NULL,
  CONSTRAINT `transaction_records_ibfk_2` FOREIGN KEY (`community_id`) REFERENCES `communities` (`community_id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成交记录表';

/*Data for the table `transaction_records` */

insert  into `transaction_records`(`record_id`,`property_id`,`community_id`,`transaction_data`,`created_at`) values (1,1,1,'{\"area\": 89.5, \"city\": \"深圳市\", \"layout\": \"3室2厅2卫\", \"district\": \"南山区\", \"floor_info\": \"中层/共28层\", \"source_url\": \"https://example.com/property/123\", \"unit_price\": 69300, \"orientation\": \"南\", \"total_price\": 620.5, \"community_name\": \"万科城市花园\", \"transaction_date\": \"2024-01-20\"}','2025-12-01 20:26:18'),(2,2,1,'{\"area\": 68.0, \"city\": \"深圳市\", \"layout\": \"2室1厅1卫\", \"district\": \"南山区\", \"floor_info\": \"中层/共28层\", \"source_url\": \"https://example.com/property/124\", \"unit_price\": 69850, \"orientation\": \"东南\", \"total_price\": 475.0, \"community_name\": \"万科城市花园\", \"transaction_date\": \"2024-02-15\"}','2025-12-01 20:26:18'),(3,1,1,'{\"area\": 89.5, \"city\": \"深圳市\", \"layout\": \"3室2厅2卫\", \"district\": \"南山区\", \"floor_info\": \"中层/共28层\", \"source_url\": \"https://example.com/property/123\", \"unit_price\": 69300, \"orientation\": \"南\", \"total_price\": 620.5, \"community_name\": \"万科城市花园\", \"transaction_date\": \"2024-01-20\"}','2025-12-01 20:27:21'),(4,2,1,'{\"area\": 68.0, \"city\": \"深圳市\", \"layout\": \"2室1厅1卫\", \"district\": \"南山区\", \"floor_info\": \"中层/共28层\", \"source_url\": \"https://example.com/property/124\", \"unit_price\": 69850, \"orientation\": \"东南\", \"total_price\": 475.0, \"community_name\": \"万科城市花园\", \"transaction_date\": \"2024-02-15\"}','2025-12-01 20:27:21'),(5,1,1,'{\"area\": 89.5, \"city\": \"深圳市\", \"layout\": \"3室2厅2卫\", \"district\": \"南山区\", \"floor_info\": \"中层/共28层\", \"source_url\": \"https://example.com/property/123\", \"unit_price\": 69300, \"orientation\": \"南\", \"total_price\": 620.5, \"community_name\": \"万科城市花园\", \"transaction_date\": \"2024-01-20\"}','2025-12-01 20:27:51'),(6,2,1,'{\"area\": 68.0, \"city\": \"深圳市\", \"layout\": \"2室1厅1卫\", \"district\": \"南山区\", \"floor_info\": \"中层/共28层\", \"source_url\": \"https://example.com/property/124\", \"unit_price\": 69850, \"orientation\": \"东南\", \"total_price\": 475.0, \"community_name\": \"万科城市花园\", \"transaction_date\": \"2024-02-15\"}','2025-12-01 20:27:51');

/*Table structure for table `user_preferences` */

DROP TABLE IF EXISTS `user_preferences`;

CREATE TABLE `user_preferences` (
  `preference_id` int NOT NULL AUTO_INCREMENT COMMENT '偏好ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `preference_data` json DEFAULT NULL COMMENT '偏好详细数据',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`preference_id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `user_preferences_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户偏好表';

/*Data for the table `user_preferences` */

insert  into `user_preferences`(`preference_id`,`user_id`,`preference_data`,`created_at`,`updated_at`) values (1,1,'{\"keywords\": [\"学区\", \"地铁\", \"新房\"], \"locations\": [\"南山区\", \"福田区\"], \"area_range\": {\"max\": 120, \"min\": 80}, \"decorations\": [\"hard\", \"luxury\"], \"house_types\": [\"apartment\"], \"price_range\": {\"max\": 600, \"min\": 300}, \"orientations\": [\"south\", \"southeast\"], \"bedroom_range\": {\"max\": 4, \"min\": 2}}','2025-12-01 20:26:18','2025-12-01 20:26:18'),(2,2,'{\"keywords\": [\"学区\", \"公园\"], \"locations\": [\"宝安区\", \"福田区\"], \"area_range\": {\"max\": 150, \"min\": 100}, \"decorations\": [\"luxury\"], \"house_types\": [\"apartment\"], \"price_range\": {\"max\": 800, \"min\": 400}, \"orientations\": [\"south\"], \"bedroom_range\": {\"max\": 4, \"min\": 3}}','2025-12-01 20:26:18','2025-12-01 20:26:18'),(3,1,'{\"keywords\": [\"学区\", \"地铁\", \"新房\"], \"locations\": [\"南山区\", \"福田区\"], \"area_range\": {\"max\": 120, \"min\": 80}, \"decorations\": [\"hard\", \"luxury\"], \"house_types\": [\"apartment\"], \"price_range\": {\"max\": 600, \"min\": 300}, \"orientations\": [\"south\", \"southeast\"], \"bedroom_range\": {\"max\": 4, \"min\": 2}}','2025-12-01 20:27:21','2025-12-01 20:27:21'),(4,2,'{\"keywords\": [\"学区\", \"公园\"], \"locations\": [\"宝安区\", \"福田区\"], \"area_range\": {\"max\": 150, \"min\": 100}, \"decorations\": [\"luxury\"], \"house_types\": [\"apartment\"], \"price_range\": {\"max\": 800, \"min\": 400}, \"orientations\": [\"south\"], \"bedroom_range\": {\"max\": 4, \"min\": 3}}','2025-12-01 20:27:21','2025-12-01 20:27:21'),(5,1,'{\"keywords\": [\"学区\", \"地铁\", \"新房\"], \"locations\": [\"南山区\", \"福田区\"], \"area_range\": {\"max\": 120, \"min\": 80}, \"decorations\": [\"hard\", \"luxury\"], \"house_types\": [\"apartment\"], \"price_range\": {\"max\": 600, \"min\": 300}, \"orientations\": [\"south\", \"southeast\"], \"bedroom_range\": {\"max\": 4, \"min\": 2}}','2025-12-01 20:27:51','2025-12-01 20:27:51'),(6,2,'{\"keywords\": [\"学区\", \"公园\"], \"locations\": [\"宝安区\", \"福田区\"], \"area_range\": {\"max\": 150, \"min\": 100}, \"decorations\": [\"luxury\"], \"house_types\": [\"apartment\"], \"price_range\": {\"max\": 800, \"min\": 400}, \"orientations\": [\"south\"], \"bedroom_range\": {\"max\": 4, \"min\": 3}}','2025-12-01 20:27:51','2025-12-01 20:27:51');

/*Table structure for table `user_recommendations` */

DROP TABLE IF EXISTS `user_recommendations`;

CREATE TABLE `user_recommendations` (
  `recommendation_id` int NOT NULL AUTO_INCREMENT COMMENT '推荐ID',
  `user_id` int NOT NULL COMMENT '用户ID',
  `property_id` int NOT NULL COMMENT '房源ID',
  `recommendation_data` json DEFAULT NULL COMMENT '推荐数据（类型、分数、原因）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '推荐时间',
  PRIMARY KEY (`recommendation_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_property_id` (`property_id`),
  KEY `idx_score` ((cast(json_unquote(json_extract(`recommendation_data`,_utf8mb3'$.score')) as decimal(5,4)))),
  KEY `idx_viewed` ((cast(json_unquote(json_extract(`recommendation_data`,_utf8mb3'$.is_viewed')) as unsigned))),
  CONSTRAINT `user_recommendations_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `user_recommendations_ibfk_2` FOREIGN KEY (`property_id`) REFERENCES `properties` (`property_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户推荐表';

/*Data for the table `user_recommendations` */

/*Table structure for table `user_similarity` */

DROP TABLE IF EXISTS `user_similarity`;

CREATE TABLE `user_similarity` (
  `similarity_id` int NOT NULL AUTO_INCREMENT COMMENT '相似度ID',
  `user_id1` int NOT NULL COMMENT '用户1ID',
  `user_id2` int NOT NULL COMMENT '用户2ID',
  `similarity_data` json DEFAULT NULL COMMENT '相似度数据（分数、算法版本）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '计算时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`similarity_id`),
  UNIQUE KEY `uk_user_pair` (`user_id1`,`user_id2`),
  KEY `idx_user1` (`user_id1`),
  KEY `idx_user2` (`user_id2`),
  KEY `idx_score` ((cast(json_unquote(json_extract(`similarity_data`,_utf8mb3'$.similarity_score')) as decimal(5,4)))),
  CONSTRAINT `user_similarity_ibfk_1` FOREIGN KEY (`user_id1`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `user_similarity_ibfk_2` FOREIGN KEY (`user_id2`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户相似度表';

/*Data for the table `user_similarity` */

/*Table structure for table `users` */

DROP TABLE IF EXISTS `users`;

CREATE TABLE `users` (
  `user_id` int NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码（明文存储）',
  `phone_number` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '手机号',
  `user_profile` json DEFAULT NULL COMMENT '用户扩展信息（预算、家庭结构等）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `phone_number` (`phone_number`),
  KEY `idx_username` (`username`),
  KEY `idx_phone` (`phone_number`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

/*Data for the table `users` */

insert  into `users`(`user_id`,`username`,`password`,`phone_number`,`user_profile`,`created_at`) values (1,'张三','password123','13800138000','{\"budget\": {\"max\": 500, \"min\": 300}, \"family_structure\": \"couple\", \"preferred_locations\": [\"南山区\", \"福田区\"]}','2025-12-01 20:26:18'),(2,'李四','password456','13900139000','{\"budget\": {\"max\": 700, \"min\": 400}, \"family_structure\": \"family\", \"preferred_locations\": [\"宝安区\"]}','2025-12-01 20:26:18'),(3,'王五','password789','13700137000','{\"budget\": {\"max\": 400, \"min\": 200}, \"family_structure\": \"single\", \"preferred_locations\": [\"龙岗区\"]}','2025-12-01 20:26:18');

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
