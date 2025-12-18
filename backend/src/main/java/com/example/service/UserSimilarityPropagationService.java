// UserSimilarityPropagationService.java
package com.example.service;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Async;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.sql.Timestamp;

/**
 * 用户相似度计算服务 - 带相似度传递算法
 * 
 * ===============================================
 * 核心算法：相似度传递（Similarity Propagation）
 * ===============================================
 * 
 * 思想：如果 A 和 B 很相似，B 和 C 很相似，那么 A 和 C 也可能相似
 * 
 * 传递公式：
 *   propagated_sim(A, C) = Σ [ sim(A, B) × sim(B, C) ] / Σ sim(A, B)
 *                          对所有与A相似的中间用户B求和
 * 
 * 最终相似度：
 *   final_sim(A, C) = α × direct_sim + β × propagated_sim + γ × behavior_sim
 * 
 * 权重调整策略：
 *   - 直接相似度可计算时（数据充足）：α=0.5, β=0.3, γ=0.2
 *   - 直接相似度稀疏时（冷启动）：α=0.2, β=0.6, γ=0.2
 *   - 无传递路径时：α=0.7, β=0.0, γ=0.3
 * 
 * 传递深度控制：
 *   - 一度传递：A → B → C（权重 = sim(A,B) × sim(B,C)）
 *   - 二度传递：A → B → D → C（权重 = sim × sim × 0.5，衰减因子）
 *   - 默认只使用一度传递，避免误差累积
 * 
 * ===============================================
 * 运行时机
 * ===============================================
 * 
 * 1. 定时全量计算：每天凌晨2点
 * 2. 用户偏好变更：实时增量计算
 * 3. 用户退出登录：异步增量计算
 * 4. 浏览行为累积：达到阈值后批量计算
 * 
 * @version 4.0 - Similarity Propagation Edition
 */
@Service
public class UserSimilarityPropagationService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Value("${hadoop.fs.defaultFS:file:///}")
    private String hdfsUri;
    
    @Value("${hadoop.data.dir:/tmp/hadoop-user-similarity}")
    private String hadoopDataDir;
    
    // ==================== 配置参数 ====================
    
    // 行为权重
    private static final double WEIGHT_FAVORITE = 5.0;
    private static final double WEIGHT_LONG_VIEW = 3.0;
    private static final double WEIGHT_SHORT_VIEW = 1.0;
    
    // 时间衰减半衰期（天）
    private static final double TIME_DECAY_HALFLIFE = 7.0;
    
    // 相似度阈值
    private static final double SIMILARITY_THRESHOLD = 0.1;
    
    // 传递相似度的最小阈值（只有高于此值的关系才参与传递）
    private static final double PROPAGATION_THRESHOLD = 0.3;
    
    // 传递衰减因子（二度传递时乘以此系数）
    private static final double PROPAGATION_DECAY = 0.5;
    
    // 最大传递深度
    private static final int MAX_PROPAGATION_DEPTH = 1;
    
    // 参与传递的最大中间用户数
    private static final int MAX_INTERMEDIATE_USERS = 20;
    
    // 批处理大小
    private static final int BATCH_SIZE = 500;
    
    // 增量计算触发阈值
    private static final int INCREMENTAL_THRESHOLD = 10;
    
    // 用户行为计数器
    private final ConcurrentHashMap<Integer, AtomicInteger> userBehaviorCounters = new ConcurrentHashMap<>();
    
    // 相似度缓存（用于传递计算）
    private Map<String, Double> similarityCache = new ConcurrentHashMap<>();
    
    // ==================== 1. 定时全量计算 ====================
    
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void scheduledFullCalculation() {
        System.out.println("========================================");
        System.out.println("[定时任务] 开始全量用户相似度计算（含传递算法）");
        System.out.println("时间: " + new Timestamp(System.currentTimeMillis()));
        System.out.println("========================================");
        
        try {
            calculateUserSimilarityFull();
        } catch (Exception e) {
            System.err.println("[定时任务] 计算失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 全量计算用户相似度
     */
    public void calculateUserSimilarityFull() throws Exception {
        long startTime = System.currentTimeMillis();
        
        // 步骤1：构建用户画像
        System.out.println("\n[步骤1/5] 构建用户画像...");
        Map<Integer, UserProfile> profiles = buildUserProfiles();
        System.out.println("构建了 " + profiles.size() + " 个用户画像");
        
        if (profiles.size() < 2) {
            System.out.println("用户数量不足，跳过计算");
            return;
        }
        
        // 步骤2：计算直接相似度
        System.out.println("\n[步骤2/5] 计算直接相似度...");
        Map<String, Double> directSimilarities = calculateDirectSimilarities(profiles);
        System.out.println("计算了 " + directSimilarities.size() + " 对直接相似度");
        
        // 步骤3：构建相似度图（用于传递计算）
        System.out.println("\n[步骤3/5] 构建相似度传递图...");
        Map<Integer, Map<Integer, Double>> similarityGraph = buildSimilarityGraph(directSimilarities);
        
        // 步骤4：计算传递相似度
        System.out.println("\n[步骤4/5] 计算传递相似度...");
        Map<String, Double> propagatedSimilarities = calculatePropagatedSimilarities(profiles, similarityGraph);
        System.out.println("计算了 " + propagatedSimilarities.size() + " 对传递相似度");
        
        // 步骤5：融合并保存
        System.out.println("\n[步骤5/5] 融合相似度并保存到数据库...");
        mergeAndSaveSimilarities(profiles, directSimilarities, propagatedSimilarities);
        
        // 更新缓存
        similarityCache.clear();
        similarityCache.putAll(directSimilarities);
        
        long endTime = System.currentTimeMillis();
        System.out.println("\n========================================");
        System.out.println("全量计算完成！耗时: " + (endTime - startTime) / 1000.0 + " 秒");
        System.out.println("========================================");
    }
    
    // ==================== 2. 事件触发 ====================
    
    /**
     * 用户偏好变更后触发
     */
    @Async
    public void onUserPreferenceChanged(int userId) {
        System.out.println("[偏好变更] 用户 " + userId + " 触发增量计算...");
        calculateIncrementalSimilarityWithPropagation(userId);
    }
    
    /**
     * 用户退出登录时触发
     */
    @Async
    public void onUserLogout(int userId) {
        AtomicInteger counter = userBehaviorCounters.get(userId);
        int behaviorCount = counter != null ? counter.get() : 0;
        
        if (behaviorCount >= INCREMENTAL_THRESHOLD) {
            System.out.println("[用户退出] 用户 " + userId + " 行为数 " + behaviorCount + "，触发增量计算");
            calculateIncrementalSimilarityWithPropagation(userId);
        }
        
        userBehaviorCounters.remove(userId);
    }
    
    /**
     * 记录用户浏览行为
     */
    public void recordUserBrowsing(int userId, int propertyId) {
        AtomicInteger counter = userBehaviorCounters.computeIfAbsent(userId, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();
        
        if (count >= INCREMENTAL_THRESHOLD) {
            System.out.println("[浏览累积] 用户 " + userId + " 达到阈值，触发增量计算");
            calculateIncrementalSimilarityWithPropagationAsync(userId);
            counter.set(0);
        }
    }
    
    @Async
    private void calculateIncrementalSimilarityWithPropagationAsync(int userId) {
        calculateIncrementalSimilarityWithPropagation(userId);
    }
    
    // ==================== 核心算法：直接相似度计算 ====================
    
    /**
     * 计算所有用户对的直接相似度
     */
    private Map<String, Double> calculateDirectSimilarities(Map<Integer, UserProfile> profiles) {
        Map<String, Double> similarities = new HashMap<>();
        List<Integer> userIds = new ArrayList<>(profiles.keySet());
        Collections.sort(userIds);
        
        for (int i = 0; i < userIds.size(); i++) {
            for (int j = i + 1; j < userIds.size(); j++) {
                int u1 = userIds.get(i);
                int u2 = userIds.get(j);
                
                UserProfile p1 = profiles.get(u1);
                UserProfile p2 = profiles.get(u2);
                
                // 计算画像相似度
                double profileSim = calculateProfileSimilarity(p1, p2);
                
                // 计算行为相似度
                double behaviorSim = calculateBehaviorSimilarity(p1, p2);
                
                // 直接相似度 = 0.6 × 画像相似度 + 0.4 × 行为相似度
                double directSim = 0.6 * profileSim + 0.4 * behaviorSim;
                
                if (directSim > 0.05) { // 保留低阈值，用于传递计算
                    String key = u1 + "," + u2;
                    similarities.put(key, directSim);
                }
            }
        }
        
        return similarities;
    }
    
    /**
     * 计算画像相似度（余弦相似度）
     */
    private double calculateProfileSimilarity(UserProfile p1, UserProfile p2) {
        double[] vec1 = p1.toFeatureVector();
        double[] vec2 = p2.toFeatureVector();
        return cosineSimilarity(vec1, vec2);
    }
    
    /**
     * 计算行为相似度（Jaccard + 加权）
     */
    private double calculateBehaviorSimilarity(UserProfile p1, UserProfile p2) {
        // 浏览房源的加权Jaccard
        double viewedSim = weightedJaccardSimilarity(p1.viewedProperties, p2.viewedProperties);
        
        // 收藏房源的Jaccard
        double favoriteSim = jaccardSimilarity(p1.favoriteProperties, p2.favoriteProperties);
        
        // 搜索关键词的Jaccard
        double keywordSim = jaccardSimilarity(p1.searchKeywords.keySet(), p2.searchKeywords.keySet());
        
        // 偏好位置的Jaccard
        double locationSim = jaccardSimilarity(p1.prefLocations, p2.prefLocations);
        
        return 0.3 * viewedSim + 0.3 * favoriteSim + 0.2 * keywordSim + 0.2 * locationSim;
    }
    
    // ==================== 核心算法：相似度传递 ====================
    
    /**
     * 构建相似度图（邻接表形式）
     */
    private Map<Integer, Map<Integer, Double>> buildSimilarityGraph(Map<String, Double> directSimilarities) {
        Map<Integer, Map<Integer, Double>> graph = new HashMap<>();
        
        for (Map.Entry<String, Double> entry : directSimilarities.entrySet()) {
            if (entry.getValue() < PROPAGATION_THRESHOLD) continue; // 只保留高相似度边
            
            String[] ids = entry.getKey().split(",");
            int u1 = Integer.parseInt(ids[0]);
            int u2 = Integer.parseInt(ids[1]);
            double sim = entry.getValue();
            
            // 双向添加
            graph.computeIfAbsent(u1, k -> new HashMap<>()).put(u2, sim);
            graph.computeIfAbsent(u2, k -> new HashMap<>()).put(u1, sim);
        }
        
        System.out.println("相似度图包含 " + graph.size() + " 个节点");
        return graph;
    }
    
    /**
     * 计算传递相似度
     * 
     * 核心公式：
     * propagated_sim(A, C) = Σ [ sim(A, B) × sim(B, C) ] / Σ sim(A, B)
     *                        对所有与A相似的中间用户B求和（B也与C相似）
     */
    private Map<String, Double> calculatePropagatedSimilarities(
            Map<Integer, UserProfile> profiles,
            Map<Integer, Map<Integer, Double>> similarityGraph) {
        
        Map<String, Double> propagatedSimilarities = new HashMap<>();
        List<Integer> userIds = new ArrayList<>(profiles.keySet());
        Collections.sort(userIds);
        
        int propagatedCount = 0;
        
        for (int i = 0; i < userIds.size(); i++) {
            for (int j = i + 1; j < userIds.size(); j++) {
                int userA = userIds.get(i);
                int userC = userIds.get(j);
                
                // 计算 A 到 C 的传递相似度
                double propagatedSim = calculatePropagatedSimilarity(userA, userC, similarityGraph);
                
                if (propagatedSim > SIMILARITY_THRESHOLD) {
                    String key = userA + "," + userC;
                    propagatedSimilarities.put(key, propagatedSim);
                    propagatedCount++;
                }
            }
        }
        
        System.out.println("通过传递发现了 " + propagatedCount + " 对新的相似关系");
        return propagatedSimilarities;
    }
    
    /**
     * 计算单对用户的传递相似度
     * 
     * A ----sim(A,B)----> B ----sim(B,C)----> C
     * 
     * 传递相似度 = Σ [ sim(A,B) × sim(B,C) ] / Σ sim(A,B)
     */
    private double calculatePropagatedSimilarity(int userA, int userC, 
            Map<Integer, Map<Integer, Double>> similarityGraph) {
        
        // 获取A的相似用户（中间用户B的候选集）
        Map<Integer, Double> neighborsA = similarityGraph.get(userA);
        if (neighborsA == null || neighborsA.isEmpty()) return 0.0;
        
        // 获取C的相似用户
        Map<Integer, Double> neighborsC = similarityGraph.get(userC);
        if (neighborsC == null || neighborsC.isEmpty()) return 0.0;
        
        // 找共同的中间用户B
        Set<Integer> intermediateUsers = new HashSet<>(neighborsA.keySet());
        intermediateUsers.retainAll(neighborsC.keySet());
        intermediateUsers.remove(userA);
        intermediateUsers.remove(userC);
        
        if (intermediateUsers.isEmpty()) return 0.0;
        
        // 限制中间用户数量（取相似度最高的N个）
        List<Integer> topIntermediates = intermediateUsers.stream()
            .sorted((b1, b2) -> Double.compare(
                neighborsA.get(b2) + neighborsC.get(b2),
                neighborsA.get(b1) + neighborsC.get(b1)))
            .limit(MAX_INTERMEDIATE_USERS)
            .collect(Collectors.toList());
        
        // 计算传递相似度
        double numerator = 0.0;   // 分子：Σ [ sim(A,B) × sim(B,C) ]
        double denominator = 0.0; // 分母：Σ sim(A,B)
        
        for (int userB : topIntermediates) {
            double simAB = neighborsA.get(userB);
            double simBC = neighborsC.get(userB);
            
            numerator += simAB * simBC;
            denominator += simAB;
        }
        
        if (denominator == 0) return 0.0;
        
        double propagatedSim = numerator / denominator;
        
        // 应用衰减（传递的相似度不应该比直接相似度更高）
        return Math.min(propagatedSim, 0.95);
    }
    
    /**
     * 融合直接相似度和传递相似度，保存到数据库
     */
    private void mergeAndSaveSimilarities(
            Map<Integer, UserProfile> profiles,
            Map<String, Double> directSimilarities,
            Map<String, Double> propagatedSimilarities) {
        
        // 清理旧数据
        jdbcTemplate.update("DELETE FROM user_similarity");
        
        String insertSql = "INSERT INTO user_similarity " +
                          "(user_id1, user_id2, similarity_data, created_at, updated_at) " +
                          "VALUES (?, ?, ?, NOW(), NOW())";
        
        List<Object[]> batchArgs = new ArrayList<>();
        Set<String> allPairs = new HashSet<>();
        allPairs.addAll(directSimilarities.keySet());
        allPairs.addAll(propagatedSimilarities.keySet());
        
        int savedCount = 0;
        int propagatedOnlyCount = 0;
        
        for (String pair : allPairs) {
            String[] ids = pair.split(",");
            int u1 = Integer.parseInt(ids[0]);
            int u2 = Integer.parseInt(ids[1]);
            
            Double directSim = directSimilarities.get(pair);
            Double propagatedSim = propagatedSimilarities.get(pair);
            
            // 确定权重
            double alpha, beta;
            String algorithm;
            
            if (directSim != null && directSim > SIMILARITY_THRESHOLD) {
                if (propagatedSim != null && propagatedSim > SIMILARITY_THRESHOLD) {
                    // 两种相似度都有：融合
                    alpha = 0.6;
                    beta = 0.4;
                    algorithm = "hybrid_propagation";
                } else {
                    // 只有直接相似度
                    alpha = 1.0;
                    beta = 0.0;
                    algorithm = "direct_only";
                }
            } else {
                if (propagatedSim != null && propagatedSim > SIMILARITY_THRESHOLD) {
                    // 只有传递相似度（发现新关系！）
                    alpha = 0.0;
                    beta = 1.0;
                    algorithm = "propagation_only";
                    propagatedOnlyCount++;
                } else {
                    continue; // 两种都低于阈值，跳过
                }
            }
            
            // 计算最终相似度
            double finalSim = alpha * (directSim != null ? directSim : 0) + 
                             beta * (propagatedSim != null ? propagatedSim : 0);
            
            if (finalSim > SIMILARITY_THRESHOLD) {
                JSONObject data = new JSONObject();
                data.put("similarity_score", round(finalSim));
                data.put("direct_similarity", round(directSim != null ? directSim : 0));
                data.put("propagated_similarity", round(propagatedSim != null ? propagatedSim : 0));
                data.put("algorithm", algorithm);
                data.put("alpha", alpha);
                data.put("beta", beta);
                
                batchArgs.add(new Object[]{u1, u2, data.toJSONString()});
                savedCount++;
                
                if (batchArgs.size() >= BATCH_SIZE) {
                    jdbcTemplate.batchUpdate(insertSql, batchArgs);
                    batchArgs.clear();
                }
            }
        }
        
        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(insertSql, batchArgs);
        }
        
        System.out.println("保存了 " + savedCount + " 对相似度");
        System.out.println("其中通过传递发现的新关系: " + propagatedOnlyCount + " 对");
    }
    
    // ==================== 增量计算（带传递） ====================
    
    /**
     * 增量计算：只更新指定用户的相似度
     */
    private void calculateIncrementalSimilarityWithPropagation(int userId) {
        try {
            // 1. 构建目标用户画像
            UserProfile targetProfile = buildSingleUserProfile(userId);
            if (targetProfile == null) return;
            
            // 2. 获取所有用户画像
            Map<Integer, UserProfile> allProfiles = buildUserProfiles();
            allProfiles.remove(userId);
            if (allProfiles.isEmpty()) return;
            
            // 3. 加载现有相似度图
            Map<Integer, Map<Integer, Double>> similarityGraph = loadSimilarityGraphFromDB();
            
            // 4. 删除该用户的旧相似度
            jdbcTemplate.update("DELETE FROM user_similarity WHERE user_id1 = ? OR user_id2 = ?", userId, userId);
            
            // 5. 计算与所有其他用户的相似度
            String insertSql = "INSERT INTO user_similarity (user_id1, user_id2, similarity_data, created_at, updated_at) " +
                              "VALUES (?, ?, ?, NOW(), NOW())";
            
            List<Object[]> batchArgs = new ArrayList<>();
            
            for (Map.Entry<Integer, UserProfile> entry : allProfiles.entrySet()) {
                int otherUserId = entry.getKey();
                UserProfile otherProfile = entry.getValue();
                
                // 直接相似度
                double directSim = 0.6 * calculateProfileSimilarity(targetProfile, otherProfile) +
                                  0.4 * calculateBehaviorSimilarity(targetProfile, otherProfile);
                
                // 传递相似度
                double propagatedSim = calculatePropagatedSimilarity(userId, otherUserId, similarityGraph);
                
                // 融合
                double alpha = (directSim > SIMILARITY_THRESHOLD) ? 0.6 : 0.2;
                double beta = (propagatedSim > SIMILARITY_THRESHOLD) ? 0.4 : 0.0;
                
                if (alpha + beta == 0) {
                    alpha = 0.7;
                    beta = 0.3;
                }
                
                double finalSim = alpha * directSim + beta * propagatedSim;
                
                if (finalSim > SIMILARITY_THRESHOLD) {
                    int minId = Math.min(userId, otherUserId);
                    int maxId = Math.max(userId, otherUserId);
                    
                    JSONObject data = new JSONObject();
                    data.put("similarity_score", round(finalSim));
                    data.put("direct_similarity", round(directSim));
                    data.put("propagated_similarity", round(propagatedSim));
                    data.put("algorithm", "incremental_propagation");
                    
                    batchArgs.add(new Object[]{minId, maxId, data.toJSONString()});
                    
                    if (batchArgs.size() >= BATCH_SIZE) {
                        jdbcTemplate.batchUpdate(insertSql, batchArgs);
                        batchArgs.clear();
                    }
                }
            }
            
            if (!batchArgs.isEmpty()) {
                jdbcTemplate.batchUpdate(insertSql, batchArgs);
            }
            
            System.out.println("[增量计算] 用户 " + userId + " 完成");
            
        } catch (Exception e) {
            System.err.println("[增量计算] 失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 从数据库加载相似度图
     */
    private Map<Integer, Map<Integer, Double>> loadSimilarityGraphFromDB() {
        Map<Integer, Map<Integer, Double>> graph = new HashMap<>();
        
        try {
            String sql = "SELECT user_id1, user_id2, " +
                        "CAST(JSON_EXTRACT(similarity_data, '$.similarity_score') AS DECIMAL(8,4)) as score " +
                        "FROM user_similarity " +
                        "WHERE CAST(JSON_EXTRACT(similarity_data, '$.similarity_score') AS DECIMAL(8,4)) > ?";
            
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, PROPAGATION_THRESHOLD);
            
            for (Map<String, Object> row : rows) {
                int u1 = ((Number) row.get("user_id1")).intValue();
                int u2 = ((Number) row.get("user_id2")).intValue();
                double score = ((Number) row.get("score")).doubleValue();
                
                graph.computeIfAbsent(u1, k -> new HashMap<>()).put(u2, score);
                graph.computeIfAbsent(u2, k -> new HashMap<>()).put(u1, score);
            }
        } catch (Exception e) {
            System.out.println("加载相似度图失败（可能是首次运行）: " + e.getMessage());
        }
        
        return graph;
    }
    
    // ==================== 公共API ====================
    
    /**
     * 获取相似用户列表
     */
    public List<Map<String, Object>> getSimilarUsers(int userId, int limit) {
        String sql = "SELECT " +
                    "CASE WHEN user_id1 = ? THEN user_id2 ELSE user_id1 END as similar_user_id, " +
                    "similarity_data " +
                    "FROM user_similarity " +
                    "WHERE user_id1 = ? OR user_id2 = ? " +
                    "ORDER BY CAST(JSON_EXTRACT(similarity_data, '$.similarity_score') AS DECIMAL(8,4)) DESC " +
                    "LIMIT ?";
        return jdbcTemplate.queryForList(sql, userId, userId, userId, limit);
    }
    
    /**
     * 通过相似度传递推荐用户
     * 
     * 找到与目标用户相似的用户B，再找与B相似但与目标用户不直接相似的用户
     */
    public List<Integer> getRecommendedUsersByPropagation(int userId, int limit) {
        // 获取直接相似用户
        Set<Integer> directSimilar = new HashSet<>(
            jdbcTemplate.queryForList(
                "SELECT CASE WHEN user_id1 = ? THEN user_id2 ELSE user_id1 END " +
                "FROM user_similarity WHERE user_id1 = ? OR user_id2 = ?",
                Integer.class, userId, userId, userId
            )
        );
        
        // 获取二度相似用户（相似用户的相似用户）
        Map<Integer, Double> secondDegree = new HashMap<>();
        
        for (Integer similarUser : directSimilar) {
            List<Map<String, Object>> neighbors = jdbcTemplate.queryForList(
                "SELECT CASE WHEN user_id1 = ? THEN user_id2 ELSE user_id1 END as neighbor, " +
                "CAST(JSON_EXTRACT(similarity_data, '$.similarity_score') AS DECIMAL(8,4)) as score " +
                "FROM user_similarity WHERE user_id1 = ? OR user_id2 = ?",
                similarUser, similarUser, similarUser
            );
            
            for (Map<String, Object> row : neighbors) {
                int neighbor = ((Number) row.get("neighbor")).intValue();
                if (neighbor != userId && !directSimilar.contains(neighbor)) {
                    double score = ((Number) row.get("score")).doubleValue();
                    secondDegree.merge(neighbor, score, Double::sum);
                }
            }
        }
        
        // 按累计相似度排序
        return secondDegree.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取相似度统计信息
     */
    public Map<String, Object> getSimilarityStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("total_pairs", jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_similarity", Integer.class));
        
        stats.put("avg_similarity", jdbcTemplate.queryForObject(
            "SELECT AVG(CAST(JSON_EXTRACT(similarity_data, '$.similarity_score') AS DECIMAL(8,4))) FROM user_similarity",
            Double.class));
        
        stats.put("propagation_only_count", jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_similarity WHERE JSON_EXTRACT(similarity_data, '$.algorithm') = 'propagation_only'",
            Integer.class));
        
        stats.put("high_similarity_count", jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_similarity WHERE CAST(JSON_EXTRACT(similarity_data, '$.similarity_score') AS DECIMAL(8,4)) > 0.5",
            Integer.class));
        
        return stats;
    }
    
    // ==================== 辅助方法 ====================
    
    private double cosineSimilarity(double[] v1, double[] v2) {
        if (v1 == null || v2 == null || v1.length != v2.length) return 0.0;
        double dot = 0, n1 = 0, n2 = 0;
        for (int i = 0; i < v1.length; i++) {
            dot += v1[i] * v2[i];
            n1 += v1[i] * v1[i];
            n2 += v2[i] * v2[i];
        }
        return (n1 == 0 || n2 == 0) ? 0.0 : dot / (Math.sqrt(n1) * Math.sqrt(n2));
    }
    
    private double jaccardSimilarity(Set<?> s1, Set<?> s2) {
        if (s1 == null || s2 == null || (s1.isEmpty() && s2.isEmpty())) return 0.0;
        Set<Object> inter = new HashSet<>(s1); inter.retainAll(s2);
        Set<Object> union = new HashSet<>(s1); union.addAll(s2);
        return union.isEmpty() ? 0.0 : (double) inter.size() / union.size();
    }
    
    /**
     * 加权Jaccard相似度（考虑浏览权重）
     */
    private double weightedJaccardSimilarity(Map<Integer, Double> m1, Map<Integer, Double> m2) {
        if (m1 == null || m2 == null || (m1.isEmpty() && m2.isEmpty())) return 0.0;
        
        Set<Integer> allKeys = new HashSet<>(m1.keySet());
        allKeys.addAll(m2.keySet());
        
        double minSum = 0.0, maxSum = 0.0;
        for (Integer key : allKeys) {
            double v1 = m1.getOrDefault(key, 0.0);
            double v2 = m2.getOrDefault(key, 0.0);
            minSum += Math.min(v1, v2);
            maxSum += Math.max(v1, v2);
        }
        
        return maxSum == 0 ? 0.0 : minSum / maxSum;
    }
    
    private double round(double value) {
        return Math.round(value * 10000) / 10000.0;
    }
    
    // ==================== 用户画像构建 ====================
    
    private Map<Integer, UserProfile> buildUserProfiles() {
        Map<Integer, UserProfile> profiles = new HashMap<>();
        loadUserData(profiles);
        loadBehaviorData(profiles);
        for (UserProfile p : profiles.values()) p.calculateDerivedFeatures();
        return profiles;
    }
    
    private UserProfile buildSingleUserProfile(int userId) {
        String sql = "SELECT user_id, user_profile FROM users WHERE user_id = ?";
        List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, userId);
        if (users.isEmpty()) return null;
        
        UserProfile profile = new UserProfile(userId);
        String json = (String) users.get(0).get("user_profile");
        if (json != null) parseUserProfileJson(profile, json);
        
        loadSingleUserPreference(profile);
        loadSingleUserBehavior(profile);
        profile.calculateDerivedFeatures();
        
        return profile;
    }
    
    private void loadUserData(Map<Integer, UserProfile> profiles) {
        String sql = "SELECT u.user_id, u.user_profile, up.preference_data FROM users u " +
                    "LEFT JOIN user_preferences up ON u.user_id = up.user_id";
        
        for (Map<String, Object> row : jdbcTemplate.queryForList(sql)) {
            Integer userId = ((Number) row.get("user_id")).intValue();
            UserProfile profile = new UserProfile(userId);
            
            String userJson = (String) row.get("user_profile");
            if (userJson != null) parseUserProfileJson(profile, userJson);
            
            String prefJson = (String) row.get("preference_data");
            if (prefJson != null) parsePreferenceJson(profile, prefJson);
            
            profiles.put(userId, profile);
        }
    }
    
    private void parseUserProfileJson(UserProfile profile, String json) {
        try {
            JSONObject obj = JSON.parseObject(json);
            JSONObject budget = obj.getJSONObject("budget");
            if (budget != null) {
                profile.budgetMin = budget.getDoubleValue("min");
                profile.budgetMax = budget.getDoubleValue("max");
            }
            profile.familyStructure = obj.getString("family_structure");
        } catch (Exception e) {}
    }
    
    private void parsePreferenceJson(UserProfile profile, String json) {
        try {
            JSONObject obj = JSON.parseObject(json);
            
            JSONObject price = obj.getJSONObject("price_range");
            if (price != null) {
                profile.prefPriceMin = price.getDoubleValue("min");
                profile.prefPriceMax = price.getDoubleValue("max");
            }
            
            JSONObject area = obj.getJSONObject("area_range");
            if (area != null) {
                profile.prefAreaMin = area.getDoubleValue("min");
                profile.prefAreaMax = area.getDoubleValue("max");
            }
            
            JSONObject bedroom = obj.getJSONObject("bedroom_range");
            if (bedroom != null) {
                profile.prefBedroomMin = bedroom.getIntValue("min");
                profile.prefBedroomMax = bedroom.getIntValue("max");
            }
            
            if (obj.containsKey("locations")) {
                for (Object loc : obj.getJSONArray("locations")) {
                    profile.prefLocations.add(loc.toString());
                }
            }
        } catch (Exception e) {}
    }
    
    private void loadSingleUserPreference(UserProfile profile) {
        String sql = "SELECT preference_data FROM user_preferences WHERE user_id = ?";
        List<Map<String, Object>> prefs = jdbcTemplate.queryForList(sql, profile.userId);
        if (!prefs.isEmpty()) {
            String json = (String) prefs.get(0).get("preference_data");
            if (json != null) parsePreferenceJson(profile, json);
        }
    }
    
    private void loadBehaviorData(Map<Integer, UserProfile> profiles) {
        long now = System.currentTimeMillis();
        
        // 浏览历史
        String browsingSql = "SELECT user_id, property_id, behavior_data, created_at FROM browsing_history " +
                            "WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
        
        for (Map<String, Object> row : jdbcTemplate.queryForList(browsingSql)) {
            Integer userId = ((Number) row.get("user_id")).intValue();
            UserProfile profile = profiles.get(userId);
            if (profile == null) continue;
            
            Integer propertyId = ((Number) row.get("property_id")).intValue();
            Timestamp createdAt = (Timestamp) row.get("created_at");
            double daysDiff = (now - createdAt.getTime()) / (1000.0 * 60 * 60 * 24);
            double timeDecay = Math.pow(0.5, daysDiff / TIME_DECAY_HALFLIFE);
            
            double duration = 60.0;
            String data = (String) row.get("behavior_data");
            if (data != null) {
                try { duration = JSON.parseObject(data).getDoubleValue("duration"); } catch (Exception e) {}
            }
            
            double weight = duration > 60 ? WEIGHT_LONG_VIEW : WEIGHT_SHORT_VIEW;
            profile.viewedProperties.merge(propertyId, weight * timeDecay, Double::sum);
            profile.totalViewCount++;
        }
        
        // 收藏
        for (Map<String, Object> row : jdbcTemplate.queryForList("SELECT user_id, property_id FROM favorites")) {
            Integer userId = ((Number) row.get("user_id")).intValue();
            UserProfile profile = profiles.get(userId);
            if (profile != null) {
                profile.favoriteProperties.add(((Number) row.get("property_id")).intValue());
            }
        }
        
        // 搜索
        String searchSql = "SELECT user_id, search_data FROM search_history WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
        for (Map<String, Object> row : jdbcTemplate.queryForList(searchSql)) {
            Integer userId = ((Number) row.get("user_id")).intValue();
            UserProfile profile = profiles.get(userId);
            if (profile == null) continue;
            
            String data = (String) row.get("search_data");
            if (data != null) {
                try {
                    String keyword = JSON.parseObject(data).getString("keyword");
                    if (keyword != null) profile.searchKeywords.merge(keyword, 1, Integer::sum);
                } catch (Exception e) {}
            }
            profile.searchCount++;
        }
    }
    
    private void loadSingleUserBehavior(UserProfile profile) {
        long now = System.currentTimeMillis();
        
        String browsingSql = "SELECT property_id, behavior_data, created_at FROM browsing_history " +
                            "WHERE user_id = ? AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
        
        for (Map<String, Object> row : jdbcTemplate.queryForList(browsingSql, profile.userId)) {
            Integer propertyId = ((Number) row.get("property_id")).intValue();
            Timestamp createdAt = (Timestamp) row.get("created_at");
            double daysDiff = (now - createdAt.getTime()) / (1000.0 * 60 * 60 * 24);
            double timeDecay = Math.pow(0.5, daysDiff / TIME_DECAY_HALFLIFE);
            
            double duration = 60.0;
            String data = (String) row.get("behavior_data");
            if (data != null) {
                try { duration = JSON.parseObject(data).getDoubleValue("duration"); } catch (Exception e) {}
            }
            
            double weight = duration > 60 ? WEIGHT_LONG_VIEW : WEIGHT_SHORT_VIEW;
            profile.viewedProperties.merge(propertyId, weight * timeDecay, Double::sum);
            profile.totalViewCount++;
        }
        
        profile.favoriteProperties.addAll(
            jdbcTemplate.queryForList("SELECT property_id FROM favorites WHERE user_id = ?", Integer.class, profile.userId)
        );
        
        String searchSql = "SELECT search_data FROM search_history WHERE user_id = ? AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
        for (Map<String, Object> row : jdbcTemplate.queryForList(searchSql, profile.userId)) {
            String data = (String) row.get("search_data");
            if (data != null) {
                try {
                    String keyword = JSON.parseObject(data).getString("keyword");
                    if (keyword != null) profile.searchKeywords.merge(keyword, 1, Integer::sum);
                } catch (Exception e) {}
            }
            profile.searchCount++;
        }
    }
    
    // ==================== 用户画像内部类 ====================
    
    private static class UserProfile {
        int userId;
        
        // 基本信息
        double budgetMin = 0, budgetMax = 0;
        String familyStructure = "unknown";
        
        // 偏好
        double prefPriceMin = 0, prefPriceMax = 0;
        double prefAreaMin = 0, prefAreaMax = 0;
        int prefBedroomMin = 0, prefBedroomMax = 0;
        Set<String> prefLocations = new HashSet<>();
        
        // 行为
        Map<Integer, Double> viewedProperties = new HashMap<>();
        Set<Integer> favoriteProperties = new HashSet<>();
        Map<String, Integer> searchKeywords = new HashMap<>();
        int totalViewCount = 0;
        int searchCount = 0;
        
        // 衍生特征
        double activityScore = 0;
        double preferenceClarity = 0;
        
        UserProfile(int userId) { this.userId = userId; }
        
        void calculateDerivedFeatures() {
            activityScore = Math.log1p(totalViewCount) * 0.4 + 
                           Math.log1p(favoriteProperties.size()) * 0.4 + 
                           Math.log1p(searchCount) * 0.2;
            
            int prefCount = 0;
            if (prefPriceMax > 0) prefCount++;
            if (prefAreaMax > 0) prefCount++;
            if (prefBedroomMax > 0) prefCount++;
            if (!prefLocations.isEmpty()) prefCount++;
            preferenceClarity = prefCount / 4.0;
        }
        
        double[] toFeatureVector() {
            return new double[]{
                budgetMin / 1000.0,
                budgetMax / 1000.0,
                prefPriceMin / 1000.0,
                prefPriceMax / 1000.0,
                prefAreaMin / 100.0,
                prefAreaMax / 100.0,
                prefBedroomMin,
                prefBedroomMax,
                Math.log1p(totalViewCount),
                Math.log1p(favoriteProperties.size()),
                Math.log1p(searchCount),
                encodeFamilyStructure(familyStructure),
                activityScore,
                preferenceClarity,
                prefLocations.size() / 5.0
            };
        }
        
        private double encodeFamilyStructure(String s) {
            if (s == null) return 0.5;
            switch (s.toLowerCase()) {
                case "single": return 0.2;
                case "couple": return 0.5;
                case "family": return 0.8;
                default: return 0.5;
            }
        }
    }
}