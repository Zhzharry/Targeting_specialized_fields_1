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
 * 1. 基于用户收藏行为计算用户相似度
 * 2. 使用Spark进行分布式计算
 * 3. 支持自动触发（用户收藏5个房源时）
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
            // 1. 获取所有用户的收藏数据
            System.out.println("\n[步骤1/4] 获取用户收藏数据...");
            Map<Integer, Set<Integer>> userFavorites = loadUserFavorites();
            System.out.println("共获取 " + userFavorites.size() + " 个用户的收藏数据");
            
            // 打印每个用户的收藏数量
            for (Map.Entry<Integer, Set<Integer>> entry : userFavorites.entrySet()) {
                System.out.println("  用户 " + entry.getKey() + " 有 " + entry.getValue().size() + " 个收藏");
            }
            
            if (userFavorites.size() < 2) {
                System.out.println("⚠️ 用户数量不足，无法计算相似度（需要至少2个用户有收藏）");
                return;
            }
            
            // 2. 使用Spark计算用户相似度
            System.out.println("\n[步骤2/4] 使用Spark RDD计算用户相似度...");
            JavaSparkContext jsc = JavaSparkContext.fromSparkContext(sparkSession.sparkContext());
            List<UserSimilarityResult> similarityResults = calculateSimilarityWithSpark(jsc, userFavorites);
            
            // 3. 保存相似度结果到数据库
            System.out.println("\n[步骤3/5] 保存相似度结果到数据库...");
            saveSimilarityResults(similarityResults);
            
            // 4. 统计信息
            System.out.println("\n[步骤4/5] 统计信息...");
            printStatistics(similarityResults);
            
            // 5. Calculate and print evaluation metrics
            System.out.println("\n[Step 5/5] Calculating evaluation metrics...");
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
            // 1. 获取所有用户的收藏数据
            System.out.println("\n[步骤1/4] 获取用户收藏数据...");
            Map<Integer, Set<Integer>> userFavorites = loadUserFavorites();
            System.out.println("共获取 " + userFavorites.size() + " 个用户的收藏数据");
            
            // 检查目标用户是否有收藏数据
            Set<Integer> targetUserFavorites = userFavorites.get(userId);
            if (targetUserFavorites == null || targetUserFavorites.isEmpty()) {
                System.out.println("用户 " + userId + " 没有收藏数据，无法计算相似度");
                return;
            }
            
            System.out.println("  目标用户 " + userId + " 有 " + targetUserFavorites.size() + " 个收藏");
            
            if (userFavorites.size() < 2) {
                System.out.println("⚠️ 用户数量不足，无法计算相似度（需要至少2个用户有收藏）");
                return;
            }
            
            // 2. 使用Spark计算该用户与其他所有用户的相似度
            System.out.println("\n[步骤2/4] 使用Spark RDD计算增量相似度...");
            JavaSparkContext jsc = JavaSparkContext.fromSparkContext(sparkSession.sparkContext());
            List<UserSimilarityResult> similarityResults = calculateSimilarityForSingleUser(jsc, userId, userFavorites);
            
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
     * 计算单个用户与其他所有用户的相似度（使用Spark）
     */
    private List<UserSimilarityResult> calculateSimilarityForSingleUser(
            JavaSparkContext jsc, 
            int targetUserId,
            Map<Integer, Set<Integer>> userFavorites) {
        
        Set<Integer> targetFavorites = userFavorites.get(targetUserId);
        if (targetFavorites == null || targetFavorites.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 生成该用户与其他所有用户的配对
        List<Tuple2<Integer, Integer>> userPairs = new ArrayList<>();
        for (Integer otherUserId : userFavorites.keySet()) {
            if (otherUserId != targetUserId) {
                // 确保 user_id1 < user_id2，以保持一致性
                if (targetUserId < otherUserId) {
                    userPairs.add(new Tuple2<>(targetUserId, otherUserId));
                } else {
                    userPairs.add(new Tuple2<>(otherUserId, targetUserId));
                }
            }
        }
        
        System.out.println("  生成 " + userPairs.size() + " 对用户进行相似度计算");
        
        // 使用Spark Broadcast变量优化性能和序列化
        Broadcast<Map<Integer, Set<Integer>>> broadcastFavorites = jsc.broadcast(userFavorites);
        JavaRDD<Tuple2<Integer, Integer>> pairsRDD = jsc.parallelize(userPairs);
        
        // 使用Spark RDD的map操作计算相似度
        final double threshold = SIMILARITY_THRESHOLD;
        List<UserSimilarityResult> results = pairsRDD.map(pair -> {
            int userId1 = pair._1();
            int userId2 = pair._2();
            
            Map<Integer, Set<Integer>> favoritesMap = broadcastFavorites.value();
            Set<Integer> favorites1 = favoritesMap.get(userId1);
            Set<Integer> favorites2 = favoritesMap.get(userId2);
            
            if (favorites1 == null || favorites2 == null || favorites1.isEmpty() || favorites2.isEmpty()) {
                return null;
            }
            
            // 计算Jaccard相似度（使用静态方法避免序列化问题）
            double jaccardSim = SimilarityCalculator.calculateJaccardSimilarity(favorites1, favorites2);
            
            // 计算余弦相似度（使用静态方法避免序列化问题）
            double cosineSim = SimilarityCalculator.calculateCosineSimilarity(favorites1, favorites2, favoritesMap);
            
            // 综合相似度
            double finalSimilarity = 0.6 * jaccardSim + 0.4 * cosineSim;
            
            if (finalSimilarity > threshold) {
                return new UserSimilarityResult(userId1, userId2, finalSimilarity, jaccardSim, cosineSim);
            }
            
            return null;
        }).filter(result -> result != null).collect();
        
        // 释放Broadcast变量
        broadcastFavorites.destroy();
        
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
     * 使用Spark RDD计算用户相似度
     */
    private List<UserSimilarityResult> calculateSimilarityWithSpark(
            JavaSparkContext jsc, 
            Map<Integer, Set<Integer>> userFavorites) {
        
        // 将用户收藏数据转换为Spark RDD
        List<Tuple2<Integer, Set<Integer>>> userFavoriteList = userFavorites.entrySet().stream()
            .map(e -> new Tuple2<>(e.getKey(), e.getValue()))
            .collect(Collectors.toList());
        
        JavaPairRDD<Integer, Set<Integer>> userFavoritesRDD = jsc.parallelizePairs(userFavoriteList);
        
        // 生成所有用户对（避免重复和自比较）
        List<Tuple2<Integer, Integer>> userPairs = new ArrayList<>();
        List<Integer> userIds = new ArrayList<>(userFavorites.keySet());
        Collections.sort(userIds);
        
        for (int i = 0; i < userIds.size(); i++) {
            for (int j = i + 1; j < userIds.size(); j++) {
                userPairs.add(new Tuple2<>(userIds.get(i), userIds.get(j)));
            }
        }
        
        System.out.println("  生成 " + userPairs.size() + " 对用户进行相似度计算");
        
        // 使用Spark Broadcast变量优化性能和序列化
        Broadcast<Map<Integer, Set<Integer>>> broadcastFavorites = jsc.broadcast(userFavorites);
        JavaRDD<Tuple2<Integer, Integer>> pairsRDD = jsc.parallelize(userPairs);
        
            // 使用Spark RDD的map操作计算相似度
        final double threshold = SIMILARITY_THRESHOLD; // 使用类常量
        List<UserSimilarityResult> results = pairsRDD.map(pair -> {
            int userId1 = pair._1();
            int userId2 = pair._2();
            
            Map<Integer, Set<Integer>> favoritesMap = broadcastFavorites.value();
            Set<Integer> favorites1 = favoritesMap.get(userId1);
            Set<Integer> favorites2 = favoritesMap.get(userId2);
            
            if (favorites1 == null || favorites2 == null || favorites1.isEmpty() || favorites2.isEmpty()) {
                return null;
            }
            
            // 计算Jaccard相似度（使用静态方法避免序列化问题）
            double jaccardSim = SimilarityCalculator.calculateJaccardSimilarity(favorites1, favorites2);
            
            // 计算余弦相似度（使用静态方法避免序列化问题）
            double cosineSim = SimilarityCalculator.calculateCosineSimilarity(favorites1, favorites2, favoritesMap);
            
            // 综合相似度
            double finalSimilarity = 0.6 * jaccardSim + 0.4 * cosineSim;
            
            // 使用常量值避免捕获实例变量
            if (finalSimilarity > threshold) {
                return new UserSimilarityResult(userId1, userId2, finalSimilarity, jaccardSim, cosineSim);
            }
            
            return null;
        }).filter(result -> result != null).collect();
        
        System.out.println("  使用阈值 " + threshold + " 过滤后，得到 " + results.size() + " 对有效相似度");
        
        // 释放Broadcast变量
        broadcastFavorites.destroy();
        
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
                    data.put("algorithm", "spark_rdd_incremental");
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
                    data.put("algorithm", "spark_rdd");
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
        
        UserSimilarityResult(int userId1, int userId2, double similarity, 
                           double jaccardSimilarity, double cosineSimilarity) {
            this.userId1 = userId1;
            this.userId2 = userId2;
            this.similarity = similarity;
            this.jaccardSimilarity = jaccardSimilarity;
            this.cosineSimilarity = cosineSimilarity;
        }
    }
}
