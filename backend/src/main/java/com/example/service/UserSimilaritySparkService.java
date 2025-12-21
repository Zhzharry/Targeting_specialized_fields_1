// UserSimilaritySparkService.java
package com.example.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.broadcast.Broadcast;
import scala.Tuple2;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.*;
import java.util.stream.Collectors;
import java.sql.Timestamp;

/**
 * 使用Spark计算用户相似度服务
 * 
 * 功能：
 * 1. 基于用户偏好、浏览历史、收藏行为综合计算用户相似度
 *    - 用户偏好（user_profile）: 30%
 *    - 浏览历史（browsing_history）: 10%
 *    - 收藏（favorites）: 60%
 * 2. 使用Spark进行分布式计算
 * 3. 支持自动触发（用户收藏3个房源时）
 * 4. 支持手动触发
 * 5. 计算任意两个用户之间的相似度
 */
@Service
public class UserSimilaritySparkService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private SparkSession sparkSession;
    
    private static final double SIMILARITY_THRESHOLD = 0.1; // 相似度阈值
    private static final int BATCH_SIZE = 500; // 批量插入大小
    private static final int AUTO_TRIGGER_FAVORITE_COUNT = 3; // 自动触发收藏数量（用户收藏3个房源时触发）
    
    /**
     * 计算所有用户之间的相似度（使用Spark）
     */
    @Transactional
    public void calculateUserSimilarityFull() {
        System.out.println("========================================");
        System.out.println("开始使用Spark计算用户相似度...");
        System.out.println("时间: " + new Timestamp(System.currentTimeMillis()));
        System.out.println("========================================");
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 获取所有用户的多维度数据
            System.out.println("\n[步骤1/5] 获取用户数据...");
            Map<Integer, Set<Integer>> userFavorites = loadUserFavorites();
            Map<Integer, Map<String, Object>> userProfiles = loadUserProfiles();
            Map<Integer, Set<Integer>> userBrowsingHistory = loadUserBrowsingHistory();
            
            System.out.println("共获取 " + userFavorites.size() + " 个用户的收藏数据");
            System.out.println("共获取 " + userProfiles.size() + " 个用户的偏好数据");
            System.out.println("共获取 " + userBrowsingHistory.size() + " 个用户的浏览历史数据");
            
            // 打印每个用户的数据统计
            Set<Integer> allUserIds = new HashSet<>();
            allUserIds.addAll(userFavorites.keySet());
            allUserIds.addAll(userProfiles.keySet());
            allUserIds.addAll(userBrowsingHistory.keySet());
            
            for (Integer userId : allUserIds) {
                int favCount = userFavorites.getOrDefault(userId, Collections.emptySet()).size();
                int browseCount = userBrowsingHistory.getOrDefault(userId, Collections.emptySet()).size();
                boolean hasProfile = userProfiles.containsKey(userId);
                System.out.println("  用户 " + userId + ": 收藏" + favCount + "个, 浏览" + browseCount + "个, 偏好" + (hasProfile ? "有" : "无"));
            }
            
            if (allUserIds.size() < 2) {
                System.out.println("⚠️ 用户数量不足，无法计算相似度（需要至少2个用户）");
                return;
            }
            
            // 2. 使用Spark计算用户相似度（整合三部分数据）
            System.out.println("\n[步骤2/5] 使用Spark RDD计算用户相似度（偏好30% + 浏览10% + 收藏60%）...");
            JavaSparkContext jsc = JavaSparkContext.fromSparkContext(sparkSession.sparkContext());
            List<UserSimilarityResult> similarityResults = calculateSimilarityWithSpark(
                jsc, userFavorites, userProfiles, userBrowsingHistory);
            
            // 3. 保存相似度结果到数据库
            System.out.println("\n[步骤3/5] 保存相似度结果到数据库...");
            saveSimilarityResults(similarityResults);
            
            // 4. 统计信息
            System.out.println("\n[步骤4/5] 统计信息...");
            printStatistics(similarityResults);
            
            // 5. Calculate and print evaluation metrics
            System.out.println("\n[步骤5/5] Calculating evaluation metrics...");
            calculateAndPrintEvaluationMetrics(null, similarityResults, userFavorites);
            
            long endTime = System.currentTimeMillis();
            System.out.println("\n========================================");
            System.out.println("用户相似度计算完成！");
            System.out.println("总耗时: " + (endTime - startTime) / 1000.0 + " 秒");
            System.out.println("========================================");
            
        } catch (Exception e) {
            System.err.println("计算过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("用户相似度计算失败", e);
        }
    }
    
    /**
     * 检查用户收藏数量，如果达到阈值则自动触发增量计算
     * 只计算该用户与其他用户的相似度，减少计算量
     */
    public void checkAndTriggerAutoCalculation(int userId) {
        try {
            // 查询用户收藏数量
            String sql = "SELECT COUNT(*) FROM favorites WHERE user_id = ?";
            Integer favoriteCount = jdbcTemplate.queryForObject(sql, Integer.class, userId);
            
            if (favoriteCount != null && favoriteCount >= AUTO_TRIGGER_FAVORITE_COUNT) {
                System.out.println("用户 " + userId + " 收藏了 " + favoriteCount + " 个房源，达到阈值 " + AUTO_TRIGGER_FAVORITE_COUNT + "，自动触发增量相似度计算");
                // 异步执行，避免阻塞
                new Thread(() -> {
                    try {
                        calculateUserSimilarityIncremental(userId);
                    } catch (Exception e) {
                        System.err.println("自动触发增量相似度计算失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                }).start();
            }
        } catch (Exception e) {
            System.err.println("检查用户收藏数量失败: " + e.getMessage());
        }
    }
    
    /**
     * 增量计算：只计算指定用户与其他所有用户的相似度
     * 只更新与该用户相关的相似度记录，减少计算量
     */
    @Transactional
    public void calculateUserSimilarityIncremental(int userId) {
        System.out.println("========================================");
        System.out.println("开始增量计算用户相似度（用户ID: " + userId + "）...");
        System.out.println("时间: " + new Timestamp(System.currentTimeMillis()));
        System.out.println("========================================");
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 获取所有用户的多维度数据
            System.out.println("\n[步骤1/5] 获取用户数据...");
            Map<Integer, Set<Integer>> userFavorites = loadUserFavorites();
            Map<Integer, Map<String, Object>> userProfiles = loadUserProfiles();
            Map<Integer, Set<Integer>> userBrowsingHistory = loadUserBrowsingHistory();
            
            System.out.println("共获取 " + userFavorites.size() + " 个用户的收藏数据");
            System.out.println("共获取 " + userProfiles.size() + " 个用户的偏好数据");
            System.out.println("共获取 " + userBrowsingHistory.size() + " 个用户的浏览历史数据");
            
            // 检查目标用户是否有数据
            Set<Integer> targetUserFavorites = userFavorites.getOrDefault(userId, Collections.emptySet());
            Map<String, Object> targetUserProfile = userProfiles.get(userId);
            Set<Integer> targetUserBrowsing = userBrowsingHistory.getOrDefault(userId, Collections.emptySet());
            
            if (targetUserFavorites.isEmpty() && targetUserProfile == null && targetUserBrowsing.isEmpty()) {
                System.out.println("用户 " + userId + " 没有任何数据，无法计算相似度");
                return;
            }
            
            System.out.println("  目标用户 " + userId + ": 收藏" + targetUserFavorites.size() + "个, 浏览" + 
                             targetUserBrowsing.size() + "个, 偏好" + (targetUserProfile != null ? "有" : "无"));
            
            if (userFavorites.size() + userProfiles.size() + userBrowsingHistory.size() < 2) {
                System.out.println("⚠️ 用户数量不足，无法计算相似度（需要至少2个用户）");
                return;
            }
            
            // 2. 使用Spark计算该用户与其他所有用户的相似度（整合三部分数据）
            System.out.println("\n[步骤2/5] 使用Spark RDD计算增量相似度（偏好30% + 浏览10% + 收藏60%）...");
            JavaSparkContext jsc = JavaSparkContext.fromSparkContext(sparkSession.sparkContext());
            List<UserSimilarityResult> similarityResults = calculateSimilarityForSingleUser(
                jsc, userId, userFavorites, userProfiles, userBrowsingHistory);
            
            // 3. 增量更新相似度结果到数据库（只更新与该用户相关的记录）
            System.out.println("\n[步骤3/4] 增量更新相似度结果到数据库...");
            saveSimilarityResultsIncremental(userId, similarityResults);
            
            // 4. 统计信息和评估指标
            System.out.println("\n[步骤4/5] 统计信息...");
            printStatistics(similarityResults);
            
            // 5. Calculate and print evaluation metrics
            System.out.println("\n[Step 5/5] Calculating evaluation metrics...");
            calculateAndPrintEvaluationMetrics(userId, similarityResults, userFavorites);
            
            long endTime = System.currentTimeMillis();
            System.out.println("\n========================================");
            System.out.println("增量用户相似度计算完成！");
            System.out.println("总耗时: " + (endTime - startTime) / 1000.0 + " 秒");
            System.out.println("========================================");
            
        } catch (Exception e) {
            System.err.println("增量计算过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("增量用户相似度计算失败", e);
        }
    }
    
    /**
     * 计算单个用户与其他所有用户的相似度（使用Spark，整合三部分数据）
     */
    private List<UserSimilarityResult> calculateSimilarityForSingleUser(
            JavaSparkContext jsc, 
            int targetUserId,
            Map<Integer, Set<Integer>> userFavorites,
            Map<Integer, Map<String, Object>> userProfiles,
            Map<Integer, Set<Integer>> userBrowsingHistory) {
        
        // 获取所有其他用户ID
        Set<Integer> allUserIds = new HashSet<>();
        allUserIds.addAll(userFavorites.keySet());
        allUserIds.addAll(userProfiles.keySet());
        allUserIds.addAll(userBrowsingHistory.keySet());
        allUserIds.remove(targetUserId);
        
        if (allUserIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 生成该用户与其他所有用户的配对
        List<Tuple2<Integer, Integer>> userPairs = new ArrayList<>();
        for (Integer otherUserId : allUserIds) {
            // 确保 user_id1 < user_id2，以保持一致性
            if (targetUserId < otherUserId) {
                userPairs.add(new Tuple2<>(targetUserId, otherUserId));
            } else {
                userPairs.add(new Tuple2<>(otherUserId, targetUserId));
            }
        }
        
        System.out.println("  生成 " + userPairs.size() + " 对用户进行相似度计算");
        
        // 使用Spark Broadcast变量优化性能和序列化
        Broadcast<Map<Integer, Set<Integer>>> broadcastFavorites = jsc.broadcast(userFavorites);
        Broadcast<Map<Integer, Map<String, Object>>> broadcastProfiles = jsc.broadcast(userProfiles);
        Broadcast<Map<Integer, Set<Integer>>> broadcastBrowsing = jsc.broadcast(userBrowsingHistory);
        JavaRDD<Tuple2<Integer, Integer>> pairsRDD = jsc.parallelize(userPairs);
        
        // 使用Spark RDD的map操作计算相似度
        final double threshold = SIMILARITY_THRESHOLD;
        List<UserSimilarityResult> results = pairsRDD.map(pair -> {
            int userId1 = pair._1();
            int userId2 = pair._2();
            
            Map<Integer, Set<Integer>> favoritesMap = broadcastFavorites.value();
            Map<Integer, Map<String, Object>> profilesMap = broadcastProfiles.value();
            Map<Integer, Set<Integer>> browsingMap = broadcastBrowsing.value();
            
            // 1. 计算偏好相似度（30%）
            double profileSim = SimilarityCalculator.calculateProfileSimilarity(
                profilesMap.get(userId1), profilesMap.get(userId2));
            
            // 2. 计算浏览历史相似度（10%）
            Set<Integer> browsing1 = browsingMap.getOrDefault(userId1, Collections.emptySet());
            Set<Integer> browsing2 = browsingMap.getOrDefault(userId2, Collections.emptySet());
            double browsingSim = SimilarityCalculator.calculateJaccardSimilarity(browsing1, browsing2);
            
            // 3. 计算收藏相似度（60%）
            Set<Integer> favorites1 = favoritesMap.getOrDefault(userId1, Collections.emptySet());
            Set<Integer> favorites2 = favoritesMap.getOrDefault(userId2, Collections.emptySet());
            double favoriteJaccardSim = SimilarityCalculator.calculateJaccardSimilarity(favorites1, favorites2);
            double favoriteCosineSim = SimilarityCalculator.calculateCosineSimilarity(
                favorites1, favorites2, favoritesMap);
            double favoriteSim = 0.6 * favoriteJaccardSim + 0.4 * favoriteCosineSim;
            
            // 4. 综合相似度：偏好30% + 浏览10% + 收藏60%
            double finalSimilarity = 0.3 * profileSim + 0.1 * browsingSim + 0.6 * favoriteSim;
            
            if (finalSimilarity > threshold) {
                return new UserSimilarityResult(userId1, userId2, finalSimilarity, 
                    favoriteJaccardSim, favoriteCosineSim, profileSim, browsingSim);
            }
            
            return null;
        }).filter(result -> result != null).collect();
        
        // 释放Broadcast变量
        broadcastFavorites.destroy();
        broadcastProfiles.destroy();
        broadcastBrowsing.destroy();
        
        System.out.println("  使用阈值 " + threshold + " 过滤后，得到 " + results.size() + " 对有效相似度");
        
        return results;
    }
    
    /**
     * 从数据库加载用户收藏数据
     */
    private Map<Integer, Set<Integer>> loadUserFavorites() {
        String sql = "SELECT user_id, property_id FROM favorites ORDER BY user_id, property_id";
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        Map<Integer, Set<Integer>> userFavorites = new HashMap<>();
        
        for (Map<String, Object> row : rows) {
            Integer userId = ((Number) row.get("user_id")).intValue();
            Integer propertyId = ((Number) row.get("property_id")).intValue();
            
            userFavorites.computeIfAbsent(userId, k -> new HashSet<>()).add(propertyId);
        }
        
        return userFavorites;
    }
    
    /**
     * 从数据库加载用户偏好数据（user_profile）
     */
    private Map<Integer, Map<String, Object>> loadUserProfiles() {
        String sql = "SELECT user_id, user_profile FROM users WHERE user_profile IS NOT NULL AND user_profile != '{}'";
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        Map<Integer, Map<String, Object>> userProfiles = new HashMap<>();
        
        for (Map<String, Object> row : rows) {
            Integer userId = ((Number) row.get("user_id")).intValue();
            String profileJson = (String) row.get("user_profile");
            
            if (profileJson != null && !profileJson.isEmpty()) {
                try {
                    JSONObject profile = JSON.parseObject(profileJson);
                    Map<String, Object> profileMap = new HashMap<>();
                    
                    // 提取关键偏好信息
                    if (profile.containsKey("budget")) {
                        profileMap.put("budget", profile.get("budget"));
                    }
                    if (profile.containsKey("preferred_locations")) {
                        profileMap.put("preferred_locations", profile.get("preferred_locations"));
                    }
                    // 不再提取family_structure（已移除家庭结构相似度计算）
                    if (profile.containsKey("preferred_property_type")) {
                        profileMap.put("preferred_property_type", profile.get("preferred_property_type"));
                    }
                    
                    if (!profileMap.isEmpty()) {
                        userProfiles.put(userId, profileMap);
                    }
                } catch (Exception e) {
                    System.err.println("解析用户 " + userId + " 的偏好数据失败: " + e.getMessage());
                }
            }
        }
        
        return userProfiles;
    }
    
    /**
     * 从数据库加载用户浏览历史数据
     */
    private Map<Integer, Set<Integer>> loadUserBrowsingHistory() {
        // 只加载最近30天的浏览历史
        String sql = "SELECT DISTINCT user_id, property_id FROM browsing_history " +
                    "WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
                    "ORDER BY user_id, property_id";
        
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        Map<Integer, Set<Integer>> userBrowsingHistory = new HashMap<>();
        
        for (Map<String, Object> row : rows) {
            Integer userId = ((Number) row.get("user_id")).intValue();
            Integer propertyId = ((Number) row.get("property_id")).intValue();
            
            userBrowsingHistory.computeIfAbsent(userId, k -> new HashSet<>()).add(propertyId);
        }
        
        return userBrowsingHistory;
    }
    
    /**
     * 使用Spark RDD计算用户相似度（整合偏好、浏览历史、收藏三部分）
     */
    private List<UserSimilarityResult> calculateSimilarityWithSpark(
            JavaSparkContext jsc, 
            Map<Integer, Set<Integer>> userFavorites,
            Map<Integer, Map<String, Object>> userProfiles,
            Map<Integer, Set<Integer>> userBrowsingHistory) {
        
        // 获取所有用户ID（合并三个数据源）
        Set<Integer> allUserIds = new HashSet<>();
        allUserIds.addAll(userFavorites.keySet());
        allUserIds.addAll(userProfiles.keySet());
        allUserIds.addAll(userBrowsingHistory.keySet());
        
        // 生成所有用户对（避免重复和自比较）
        List<Tuple2<Integer, Integer>> userPairs = new ArrayList<>();
        List<Integer> userIds = new ArrayList<>(allUserIds);
        Collections.sort(userIds);
        
        for (int i = 0; i < userIds.size(); i++) {
            for (int j = i + 1; j < userIds.size(); j++) {
                userPairs.add(new Tuple2<>(userIds.get(i), userIds.get(j)));
            }
        }
        
        System.out.println("  生成 " + userPairs.size() + " 对用户进行相似度计算");
        
        // 使用Spark Broadcast变量优化性能和序列化
        Broadcast<Map<Integer, Set<Integer>>> broadcastFavorites = jsc.broadcast(userFavorites);
        Broadcast<Map<Integer, Map<String, Object>>> broadcastProfiles = jsc.broadcast(userProfiles);
        Broadcast<Map<Integer, Set<Integer>>> broadcastBrowsing = jsc.broadcast(userBrowsingHistory);
        JavaRDD<Tuple2<Integer, Integer>> pairsRDD = jsc.parallelize(userPairs);
        
        // 使用Spark RDD的map操作计算相似度
        final double threshold = SIMILARITY_THRESHOLD;
        List<UserSimilarityResult> results = pairsRDD.map(pair -> {
            int userId1 = pair._1();
            int userId2 = pair._2();
            
            Map<Integer, Set<Integer>> favoritesMap = broadcastFavorites.value();
            Map<Integer, Map<String, Object>> profilesMap = broadcastProfiles.value();
            Map<Integer, Set<Integer>> browsingMap = broadcastBrowsing.value();
            
            // 1. 计算偏好相似度（30%）
            double profileSim = SimilarityCalculator.calculateProfileSimilarity(
                profilesMap.get(userId1), profilesMap.get(userId2));
            
            // 2. 计算浏览历史相似度（10%）
            Set<Integer> browsing1 = browsingMap.getOrDefault(userId1, Collections.emptySet());
            Set<Integer> browsing2 = browsingMap.getOrDefault(userId2, Collections.emptySet());
            double browsingSim = SimilarityCalculator.calculateJaccardSimilarity(browsing1, browsing2);
            
            // 3. 计算收藏相似度（60%）
            Set<Integer> favorites1 = favoritesMap.getOrDefault(userId1, Collections.emptySet());
            Set<Integer> favorites2 = favoritesMap.getOrDefault(userId2, Collections.emptySet());
            double favoriteJaccardSim = SimilarityCalculator.calculateJaccardSimilarity(favorites1, favorites2);
            double favoriteCosineSim = SimilarityCalculator.calculateCosineSimilarity(
                favorites1, favorites2, favoritesMap);
            double favoriteSim = 0.6 * favoriteJaccardSim + 0.4 * favoriteCosineSim;
            
            // 4. 综合相似度：偏好30% + 浏览10% + 收藏60%
            double finalSimilarity = 0.3 * profileSim + 0.1 * browsingSim + 0.6 * favoriteSim;
            
            if (finalSimilarity > threshold) {
                return new UserSimilarityResult(userId1, userId2, finalSimilarity, 
                    favoriteJaccardSim, favoriteCosineSim, profileSim, browsingSim);
            }
            
            return null;
        }).filter(result -> result != null).collect();
        
        System.out.println("  使用阈值 " + threshold + " 过滤后，得到 " + results.size() + " 对有效相似度");
        
        // 释放Broadcast变量
        broadcastFavorites.destroy();
        broadcastProfiles.destroy();
        broadcastBrowsing.destroy();
        
        // 使用Spark RDD验证数据
        long count = pairsRDD.count();
        System.out.println("  使用Spark RDD处理了 " + count + " 对用户，计算出 " + results.size() + " 对有效相似度");
        
        return results;
    }
    
    /**
     * 静态内部类：用于计算相似度，避免序列化问题
     * 所有方法都是静态的，不依赖外部类的实例变量
     */
    private static class SimilarityCalculator {
        /**
         * 计算Jaccard相似度（静态方法）
         */
        static double calculateJaccardSimilarity(Set<Integer> set1, Set<Integer> set2) {
        Set<Integer> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<Integer> union = new HashSet<>(set1);
        union.addAll(set2);
        
        if (union.isEmpty()) {
            return 0.0;
        }
        
        return (double) intersection.size() / union.size();
    }
    
    /**
         * 计算余弦相似度（基于共同收藏的房源，静态方法）
     */
        static double calculateCosineSimilarity(
            Set<Integer> favorites1, 
            Set<Integer> favorites2,
            Map<Integer, Set<Integer>> allFavorites) {
        
        // 获取所有房源的集合
        Set<Integer> allProperties = new HashSet<>();
        allFavorites.values().forEach(allProperties::addAll);
        
        // 构建向量
        List<Integer> propertyList = new ArrayList<>(allProperties);
        Collections.sort(propertyList);
        
        double[] vec1 = new double[propertyList.size()];
        double[] vec2 = new double[propertyList.size()];
        
        for (int i = 0; i < propertyList.size(); i++) {
            int propertyId = propertyList.get(i);
            vec1[i] = favorites1.contains(propertyId) ? 1.0 : 0.0;
            vec2[i] = favorites2.contains(propertyId) ? 1.0 : 0.0;
        }
        
        // 计算余弦相似度
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }
        
        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    /**
         * 计算用户偏好相似度（基于user_profile，静态方法）
         */
        static double calculateProfileSimilarity(
                Map<String, Object> profile1, 
                Map<String, Object> profile2) {
            
            // 如果任一用户没有偏好数据，返回0
            if (profile1 == null || profile1.isEmpty() || profile2 == null || profile2.isEmpty()) {
                return 0.0;
            }
            
            double totalSim = 0.0;
            int weightCount = 0;
            
            // 1. 预算相似度（权重45%）
            if (profile1.containsKey("budget") && profile2.containsKey("budget")) {
                try {
                    Object budget1Obj = profile1.get("budget");
                    Object budget2Obj = profile2.get("budget");
                    
                    JSONObject budget1 = null;
                    JSONObject budget2 = null;
                    
                    if (budget1Obj instanceof JSONObject) {
                        budget1 = (JSONObject) budget1Obj;
                    } else if (budget1Obj instanceof Map) {
                        budget1 = new JSONObject((Map<String, Object>) budget1Obj);
                    } else if (budget1Obj instanceof String) {
                        budget1 = JSON.parseObject((String) budget1Obj);
                    }
                    
                    if (budget2Obj instanceof JSONObject) {
                        budget2 = (JSONObject) budget2Obj;
                    } else if (budget2Obj instanceof Map) {
                        budget2 = new JSONObject((Map<String, Object>) budget2Obj);
                    } else if (budget2Obj instanceof String) {
                        budget2 = JSON.parseObject((String) budget2Obj);
                    }
                    
                    if (budget1 != null && budget2 != null) {
                        double min1 = budget1.getDoubleValue("min");
                        double max1 = budget1.getDoubleValue("max");
                        double min2 = budget2.getDoubleValue("min");
                        double max2 = budget2.getDoubleValue("max");
                        
                        // 计算预算区间重叠度
                        double overlapMin = Math.max(min1, min2);
                        double overlapMax = Math.min(max1, max2);
                        double overlap = Math.max(0, overlapMax - overlapMin);
                        double union = Math.max(max1, max2) - Math.min(min1, min2);
                        
                        double budgetSim = union > 0 ? overlap / union : 0.0;
                        totalSim += 0.45 * budgetSim;
                        weightCount++;
                    }
                } catch (Exception e) {
                    // 忽略解析错误
                }
            }
            
            // 2. 偏好位置相似度（权重35%）
            if (profile1.containsKey("preferred_locations") && profile2.containsKey("preferred_locations")) {
                try {
                    Object loc1Obj = profile1.get("preferred_locations");
                    Object loc2Obj = profile2.get("preferred_locations");
                    
                    Set<String> set1 = new HashSet<>();
                    Set<String> set2 = new HashSet<>();
                    
                    // 处理不同的数据类型
                    if (loc1Obj instanceof List) {
                        for (Object item : (List<?>) loc1Obj) {
                            set1.add(item.toString());
                        }
                    } else if (loc1Obj instanceof String) {
                        set1.add((String) loc1Obj);
                    }
                    
                    if (loc2Obj instanceof List) {
                        for (Object item : (List<?>) loc2Obj) {
                            set2.add(item.toString());
                        }
                    } else if (loc2Obj instanceof String) {
                        set2.add((String) loc2Obj);
                    }
                    
                    if (!set1.isEmpty() && !set2.isEmpty()) {
                        // 直接使用字符串集合计算Jaccard相似度
                        Set<String> intersection = new HashSet<>(set1);
                        intersection.retainAll(set2);
                        Set<String> union = new HashSet<>(set1);
                        union.addAll(set2);
                        double locationSim = union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
                        totalSim += 0.35 * locationSim;
                        weightCount++;
                    }
                } catch (Exception e) {
                    // 忽略解析错误
                }
            }
            
            // 3. 偏好房源类型相似度（权重20%）
            if (profile1.containsKey("preferred_property_type") && profile2.containsKey("preferred_property_type")) {
                String type1 = profile1.get("preferred_property_type").toString();
                String type2 = profile2.get("preferred_property_type").toString();
                double typeSim = type1.equals(type2) ? 1.0 : 0.0;
                totalSim += 0.2 * typeSim;
                weightCount++;
            }
            
            // 如果没有任何可比较的字段，返回0
            if (weightCount == 0) {
                return 0.0;
            }
            
            // 归一化：如果某些字段缺失，按实际权重比例调整
            double totalWeight = 0.0;
            if (profile1.containsKey("budget") && profile2.containsKey("budget")) totalWeight += 0.45;
            if (profile1.containsKey("preferred_locations") && profile2.containsKey("preferred_locations")) totalWeight += 0.35;
            if (profile1.containsKey("preferred_property_type") && profile2.containsKey("preferred_property_type")) totalWeight += 0.2;
            
            return totalWeight > 0 ? totalSim / totalWeight : 0.0;
        }
    }
    
    /**
     * 增量保存相似度结果到数据库（只更新指定用户相关的记录）
     */
    private void saveSimilarityResultsIncremental(int userId, List<UserSimilarityResult> results) {
        System.out.println("  准备增量更新 " + results.size() + " 对用户相似度数据（用户ID: " + userId + "）");
        
        if (results == null || results.isEmpty()) {
            System.out.println("  ⚠️ 警告：没有计算出任何相似度数据！");
            // 即使没有结果，也要删除该用户相关的旧记录
            try {
                int deletedCount = jdbcTemplate.update(
                    "DELETE FROM user_similarity WHERE user_id1 = ? OR user_id2 = ?", userId, userId);
                System.out.println("  已删除用户 " + userId + " 的 " + deletedCount + " 条旧相似度记录");
            } catch (Exception e) {
                System.err.println("  删除旧记录失败: " + e.getMessage());
            }
            return;
        }
        
        try {
            // 删除该用户相关的旧记录（只删除与目标用户相关的，不影响其他用户的相似度）
            int deletedCount = jdbcTemplate.update(
                "DELETE FROM user_similarity WHERE user_id1 = ? OR user_id2 = ?", userId, userId);
            System.out.println("  已删除用户 " + userId + " 的 " + deletedCount + " 条旧相似度记录");
            
            String insertSql = "INSERT INTO user_similarity " +
                              "(user_id1, user_id2, similarity_data, created_at, updated_at) " +
                              "VALUES (?, ?, ?, NOW(), NOW())";
            
            List<Object[]> batchArgs = new ArrayList<>();
            int savedCount = 0;
            
            for (UserSimilarityResult result : results) {
                try {
                    JSONObject data = new JSONObject();
                    data.put("similarity_score", Math.round(result.similarity * 10000) / 10000.0);
                    data.put("jaccard_similarity", Math.round(result.jaccardSimilarity * 10000) / 10000.0);
                    data.put("cosine_similarity", Math.round(result.cosineSimilarity * 10000) / 10000.0);
                    data.put("profile_similarity", Math.round(result.profileSimilarity * 10000) / 10000.0);
                    data.put("browsing_similarity", Math.round(result.browsingSimilarity * 10000) / 10000.0);
                    data.put("algorithm", "spark_rdd_incremental_multi_source");
                    data.put("weights", "profile:30%, browsing:10%, favorite:60%");
                    data.put("calculated_at", new Timestamp(System.currentTimeMillis()).toString());
                    data.put("target_user_id", userId);
                    
                    batchArgs.add(new Object[]{
                        result.userId1, 
                        result.userId2, 
                        data.toJSONString()
                    });
                    
                    if (batchArgs.size() >= BATCH_SIZE) {
                        int[] updateCounts = jdbcTemplate.batchUpdate(insertSql, batchArgs);
                        savedCount += updateCounts.length;
                        System.out.println("  批量保存了 " + updateCounts.length + " 条数据（累计：" + savedCount + "）");
                        batchArgs.clear();
                    }
                } catch (Exception e) {
                    System.err.println("  保存单条记录失败 (user_id1=" + result.userId1 + ", user_id2=" + result.userId2 + "): " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            // 保存剩余的批次
            if (!batchArgs.isEmpty()) {
                int[] updateCounts = jdbcTemplate.batchUpdate(insertSql, batchArgs);
                savedCount += updateCounts.length;
                System.out.println("  最后批量保存了 " + updateCounts.length + " 条数据");
            }
            
            // 验证保存结果
            Integer actualCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_similarity WHERE user_id1 = ? OR user_id2 = ?", 
                Integer.class, userId, userId);
            
            System.out.println("  ✅ 增量更新完成！");
            System.out.println("    - 计算结果数量: " + results.size());
            System.out.println("    - 实际保存数量: " + savedCount);
            System.out.println("    - 数据库中用户 " + userId + " 相关的记录数: " + actualCount);
            
            if (savedCount != results.size()) {
                System.err.println("  ⚠️ 警告：保存数量与计算结果不一致！");
            }
            
        } catch (Exception e) {
            System.err.println("  ❌ 增量保存数据到数据库失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("增量保存相似度数据失败", e);
        }
    }
    
    /**
     * 保存相似度结果到数据库（全量更新）
     */
    private void saveSimilarityResults(List<UserSimilarityResult> results) {
        System.out.println("  准备保存 " + results.size() + " 对用户相似度数据");
        
        if (results == null || results.isEmpty()) {
            System.out.println("  ⚠️ 警告：没有计算出任何相似度数据！");
            System.out.println("  可能的原因：");
            System.out.println("    1. 用户数量不足（需要至少2个用户有收藏）");
            System.out.println("    2. 所有用户对的相似度都低于阈值（0.1）");
            System.out.println("    3. 用户之间没有共同的收藏");
            // 不清空现有数据，保留之前的计算结果
            return;
        }
        
        try {
            // 清空现有数据
            int deletedCount = jdbcTemplate.update("DELETE FROM user_similarity");
            System.out.println("  已清空 " + deletedCount + " 条旧数据");
        
        String insertSql = "INSERT INTO user_similarity " +
                          "(user_id1, user_id2, similarity_data, created_at, updated_at) " +
                          "VALUES (?, ?, ?, NOW(), NOW())";
        
        List<Object[]> batchArgs = new ArrayList<>();
            int savedCount = 0;
        
        for (UserSimilarityResult result : results) {
                try {
            JSONObject data = new JSONObject();
            data.put("similarity_score", Math.round(result.similarity * 10000) / 10000.0);
            data.put("jaccard_similarity", Math.round(result.jaccardSimilarity * 10000) / 10000.0);
            data.put("cosine_similarity", Math.round(result.cosineSimilarity * 10000) / 10000.0);
                    data.put("profile_similarity", Math.round(result.profileSimilarity * 10000) / 10000.0);
                    data.put("browsing_similarity", Math.round(result.browsingSimilarity * 10000) / 10000.0);
                    data.put("algorithm", "spark_rdd_multi_source");
                    data.put("weights", "profile:30%, browsing:10%, favorite:60%");
            data.put("calculated_at", new Timestamp(System.currentTimeMillis()).toString());
            
            batchArgs.add(new Object[]{
                result.userId1, 
                result.userId2, 
                data.toJSONString()
            });
            
            if (batchArgs.size() >= BATCH_SIZE) {
                        int[] updateCounts = jdbcTemplate.batchUpdate(insertSql, batchArgs);
                        savedCount += updateCounts.length;
                        System.out.println("  批量保存了 " + updateCounts.length + " 条数据（累计：" + savedCount + "）");
                batchArgs.clear();
                    }
                } catch (Exception e) {
                    System.err.println("  保存单条记录失败 (user_id1=" + result.userId1 + ", user_id2=" + result.userId2 + "): " + e.getMessage());
                    e.printStackTrace();
                }
        }
        
        // 保存剩余的批次
        if (!batchArgs.isEmpty()) {
                int[] updateCounts = jdbcTemplate.batchUpdate(insertSql, batchArgs);
                savedCount += updateCounts.length;
                System.out.println("  最后批量保存了 " + updateCounts.length + " 条数据");
            }
            
            // 验证保存结果
            Integer actualCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_similarity", Integer.class);
            
            System.out.println("  ✅ 保存完成！");
            System.out.println("    - 计算结果数量: " + results.size());
            System.out.println("    - 实际保存数量: " + savedCount);
            System.out.println("    - 数据库中记录数: " + actualCount);
            
            if (savedCount != results.size()) {
                System.err.println("  ⚠️ 警告：保存数量与计算结果不一致！");
            }
            
        } catch (Exception e) {
            System.err.println("  ❌ 保存数据到数据库失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("保存相似度数据失败", e);
        }
    }
    
    /**
     * 打印统计信息
     */
    private void printStatistics(List<UserSimilarityResult> results) {
        if (results.isEmpty()) {
            System.out.println("  没有计算出有效的相似度数据");
            return;
        }
        
        double maxSim = results.stream().mapToDouble(r -> r.similarity).max().orElse(0.0);
        double minSim = results.stream().mapToDouble(r -> r.similarity).min().orElse(0.0);
        double avgSim = results.stream().mapToDouble(r -> r.similarity).average().orElse(0.0);
        
        System.out.println("  统计信息:");
        System.out.println("    - 总相似度对数: " + results.size());
        System.out.println("    - 最大相似度: " + String.format("%.4f", maxSim));
        System.out.println("    - 最小相似度: " + String.format("%.4f", minSim));
        System.out.println("    - 平均相似度: " + String.format("%.4f", avgSim));
    }
    
    /**
     * 计算并打印评估指标（精确率、召回率、F1分数等）
     * 
     * @param targetUserId 目标用户ID（如果为null，则评估所有用户对）
     * @param predictedSimilarities 预测的相似用户对
     * @param userFavorites 所有用户的收藏数据
     */
    private void calculateAndPrintEvaluationMetrics(
            Integer targetUserId,
            List<UserSimilarityResult> predictedSimilarities,
            Map<Integer, Set<Integer>> userFavorites) {
        
        if (predictedSimilarities == null || predictedSimilarities.isEmpty()) {
            System.out.println("  [WARNING] No prediction results, cannot calculate evaluation metrics");
            return;
        }
        
        // Define ground truth: two users are truly similar if they have at least 2 common favorites
        final int MIN_COMMON_FAVORITES = 2;
        
        // Build ground truth set: truly similar user pairs
        Set<String> trueSimilarPairs = new HashSet<>();
        Set<String> allPossiblePairs = new HashSet<>();
        
        List<Integer> userIds = new ArrayList<>(userFavorites.keySet());
        Collections.sort(userIds);
        
        for (int i = 0; i < userIds.size(); i++) {
            for (int j = i + 1; j < userIds.size(); j++) {
                int userId1 = userIds.get(i);
                int userId2 = userIds.get(j);
                
                // If target user is specified, only evaluate pairs related to that user
                if (targetUserId != null && userId1 != targetUserId && userId2 != targetUserId) {
                    continue;
                }
                
                Set<Integer> favorites1 = userFavorites.get(userId1);
                Set<Integer> favorites2 = userFavorites.get(userId2);
                
                if (favorites1 == null || favorites2 == null || favorites1.isEmpty() || favorites2.isEmpty()) {
                    continue;
                }
                
                // Calculate common favorites count
                Set<Integer> intersection = new HashSet<>(favorites1);
                intersection.retainAll(favorites2);
                int commonCount = intersection.size();
                
                String pairKey = userId1 + "_" + userId2;
                allPossiblePairs.add(pairKey);
                
                // If common favorites >= threshold, consider as truly similar
                if (commonCount >= MIN_COMMON_FAVORITES) {
                    trueSimilarPairs.add(pairKey);
                }
            }
        }
        
        // Build predicted similar user pairs set
        Set<String> predictedSimilarPairs = new HashSet<>();
        for (UserSimilarityResult result : predictedSimilarities) {
            String pairKey = result.userId1 + "_" + result.userId2;
            predictedSimilarPairs.add(pairKey);
        }
        
        // Calculate TP, FP, FN, TN
        int truePositives = 0;  // Predicted similar, actually similar
        int falsePositives = 0; // Predicted similar, actually not similar
        int falseNegatives = 0; // Predicted not similar, actually similar
        int trueNegatives = 0;  // Predicted not similar, actually not similar
        
        for (String pair : allPossiblePairs) {
            boolean isTrueSimilar = trueSimilarPairs.contains(pair);
            boolean isPredictedSimilar = predictedSimilarPairs.contains(pair);
            
            if (isTrueSimilar && isPredictedSimilar) {
                truePositives++;
            } else if (!isTrueSimilar && isPredictedSimilar) {
                falsePositives++;
            } else if (isTrueSimilar && !isPredictedSimilar) {
                falseNegatives++;
            } else {
                trueNegatives++;
            }
        }
        
        // Calculate evaluation metrics
        double precision = 0.0;
        if (truePositives + falsePositives > 0) {
            precision = (double) truePositives / (truePositives + falsePositives);
        }
        
        double recall = 0.0;
        if (truePositives + falseNegatives > 0) {
            recall = (double) truePositives / (truePositives + falseNegatives);
        }
        
        double f1Score = 0.0;
        if (precision + recall > 0) {
            f1Score = 2.0 * (precision * recall) / (precision + recall);
        }
        
        double accuracy = 0.0;
        int total = truePositives + falsePositives + falseNegatives + trueNegatives;
        if (total > 0) {
            accuracy = (double) (truePositives + trueNegatives) / total;
        }
        
        // Calculate other metrics
        double specificity = 0.0; // True Negative Rate
        if (trueNegatives + falsePositives > 0) {
            specificity = (double) trueNegatives / (trueNegatives + falsePositives);
        }
        
        // Print evaluation metrics
        System.out.println("\n  ========== Evaluation Metrics ==========");
        System.out.println("  Confusion Matrix:");
        System.out.println("    - True Positives (TP):  " + truePositives + "  (Predicted similar, actually similar)");
        System.out.println("    - False Positives (FP): " + falsePositives + "  (Predicted similar, actually not similar)");
        System.out.println("    - False Negatives (FN): " + falseNegatives + "  (Predicted not similar, actually similar)");
        System.out.println("    - True Negatives (TN):  " + trueNegatives + "  (Predicted not similar, actually not similar)");
        System.out.println("    - Total User Pairs: " + total);
        System.out.println("\n  Core Metrics:");
        System.out.println("    - Precision: " + String.format("%.4f", precision) + 
                          "  (Proportion of predicted similar users that are actually similar)");
        System.out.println("    - Recall:    " + String.format("%.4f", recall) + 
                          "  (Proportion of actually similar users that are predicted as similar)");
        System.out.println("    - F1-Score:  " + String.format("%.4f", f1Score) + 
                          "  (Harmonic mean of precision and recall)");
        System.out.println("    - Accuracy:  " + String.format("%.4f", accuracy) + 
                          "  (Proportion of all predictions that are correct)");
        System.out.println("    - Specificity: " + String.format("%.4f", specificity) + 
                          "  (True Negative Rate, proportion of non-similar pairs correctly identified)");
        System.out.println("\n  Data Statistics:");
        System.out.println("    - Ground Truth Similar Pairs: " + trueSimilarPairs.size() + 
                          "  (Common favorites >= " + MIN_COMMON_FAVORITES + ")");
        System.out.println("    - Predicted Similar Pairs: " + predictedSimilarPairs.size());
        System.out.println("    - Evaluation Criteria: Common favorites >= " + MIN_COMMON_FAVORITES + " considered as truly similar");
        System.out.println("  =========================================\n");
    }
    
    /**
     * 用户相似度结果内部类
     * 实现Serializable接口以支持Spark序列化
     */
    private static class UserSimilarityResult implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        
        int userId1;
        int userId2;
        double similarity;
        double jaccardSimilarity;
        double cosineSimilarity;
        double profileSimilarity;
        double browsingSimilarity;
        
        // 兼容旧构造方法
        UserSimilarityResult(int userId1, int userId2, double similarity, 
                           double jaccardSimilarity, double cosineSimilarity) {
            this.userId1 = userId1;
            this.userId2 = userId2;
            this.similarity = similarity;
            this.jaccardSimilarity = jaccardSimilarity;
            this.cosineSimilarity = cosineSimilarity;
            this.profileSimilarity = 0.0;
            this.browsingSimilarity = 0.0;
        }
        
        // 新构造方法（包含偏好和浏览相似度）
        UserSimilarityResult(int userId1, int userId2, double similarity, 
                           double jaccardSimilarity, double cosineSimilarity,
                           double profileSimilarity, double browsingSimilarity) {
            this.userId1 = userId1;
            this.userId2 = userId2;
            this.similarity = similarity;
            this.jaccardSimilarity = jaccardSimilarity;
            this.cosineSimilarity = cosineSimilarity;
            this.profileSimilarity = profileSimilarity;
            this.browsingSimilarity = browsingSimilarity;
        }
    }
}
