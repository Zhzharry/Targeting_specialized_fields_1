// SimilarityController.java
package com.example.controller;

import com.example.service.PropertySimilarityService;
import com.example.service.PropertySimilarityCFService;
import com.example.service.UserSimilarityPropagationService;
import com.example.service.UserSimilaritySparkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 相似度计算REST API控制器
 * 
 * 包含：
 * 1. 房源相似度接口（原有）
 * 2. 用户相似度接口（新增）
 */
@RestController
@RequestMapping("/api/similarity")
@CrossOrigin(origins = "*")
public class SimilarityController {

    @Autowired
    private PropertySimilarityService similarityService;

    @Autowired
    private PropertySimilarityCFService cfService;

    @Autowired
    private UserSimilarityPropagationService userSimilarityService;

    @Autowired
    private UserSimilaritySparkService userSimilaritySparkService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ==================== 房源相似度接口（原有） ====================

    /**
     * 手动触发完整的房源相似度计算
     * POST /api/similarity/calculate
     */
    @PostMapping("/calculate")
    public ResponseEntity<Map<String, Object>> calculateSimilarity() {
        Map<String, Object> result = new HashMap<>();

        try {
            CompletableFuture.runAsync(() -> {
                similarityService.calculatePropertySimilarityFull();
                cfService.calculatePropertySimilarityCF();
                cfService.mergeSimilarityResults();
            });

            result.put("success", true);
            result.put("message", "相似度计算任务已启动，请稍后查看结果");
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "计算失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 只执行基于内容的相似度计算
     * POST /api/similarity/calculate/content
     */
    @PostMapping("/calculate/content")
    public ResponseEntity<Map<String, Object>> calculateContentSimilarity() {
        Map<String, Object> result = new HashMap<>();

        try {
            long startTime = System.currentTimeMillis();
            similarityService.calculatePropertySimilarityFull();
            long endTime = System.currentTimeMillis();

            result.put("success", true);
            result.put("message", "基于内容的相似度计算完成");
            result.put("duration_ms", endTime - startTime);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "计算失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 计算指定ID范围内的房源相似度（只计算property_id < maxPropertyId的房源）
     * POST /api/similarity/calculate/properties/range?maxPropertyId=10000
     */
    @PostMapping("/calculate/properties/range")
    public ResponseEntity<Map<String, Object>> calculatePropertySimilarityByRange(
            @RequestParam(required = true) Integer maxPropertyId) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (maxPropertyId == null || maxPropertyId <= 0) {
                result.put("success", false);
                result.put("message", "maxPropertyId必须大于0");
                return ResponseEntity.badRequest().body(result);
            }

            long startTime = System.currentTimeMillis();
            similarityService.calculatePropertySimilarityByRange(maxPropertyId);
            long endTime = System.currentTimeMillis();

            result.put("success", true);
            result.put("message", "房源相似度计算完成（property_id < " + maxPropertyId + "）");
            result.put("max_property_id", maxPropertyId);
            result.put("duration_ms", endTime - startTime);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "计算失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 只执行协同过滤相似度计算
     * POST /api/similarity/calculate/cf
     */
    @PostMapping("/calculate/cf")
    public ResponseEntity<Map<String, Object>> calculateCFSimilarity() {
        Map<String, Object> result = new HashMap<>();

        try {
            long startTime = System.currentTimeMillis();
            cfService.calculatePropertySimilarityCF();
            long endTime = System.currentTimeMillis();

            result.put("success", true);
            result.put("message", "协同过滤相似度计算完成");
            result.put("duration_ms", endTime - startTime);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "计算失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 使用Hadoop MapReduce计算相似度（大数据量场景）
     * POST /api/similarity/calculate/mapreduce
     */
    @PostMapping("/calculate/mapreduce")
    public ResponseEntity<Map<String, Object>> calculateWithMapReduce() {
        Map<String, Object> result = new HashMap<>();

        try {
            long startTime = System.currentTimeMillis();
            cfService.calculateSimilarityWithMapReduce();
            long endTime = System.currentTimeMillis();

            result.put("success", true);
            result.put("message", "MapReduce相似度计算完成");
            result.put("duration_ms", endTime - startTime);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "计算失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 获取指定房源的相似房源列表
     * GET /api/similarity/properties/{propertyId}?limit=10
     */
    @GetMapping("/properties/{propertyId}")
    public ResponseEntity<Map<String, Object>> getSimilarProperties(
            @PathVariable int propertyId,
            @RequestParam(defaultValue = "10") int limit) {

        Map<String, Object> result = new HashMap<>();

        try {
            List<Map<String, Object>> similarProperties = similarityService.getSimilarProperties(propertyId, limit);

            result.put("success", true);
            result.put("property_id", propertyId);
            result.put("similar_properties", similarProperties);
            result.put("count", similarProperties.size());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 融合多种相似度结果
     * POST /api/similarity/merge
     */
    @PostMapping("/merge")
    public ResponseEntity<Map<String, Object>> mergeSimilarityResults() {
        Map<String, Object> result = new HashMap<>();

        try {
            cfService.mergeSimilarityResults();

            result.put("success", true);
            result.put("message", "相似度结果融合完成");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "融合失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 获取房源相似度计算统计信息
     * GET /api/similarity/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> result = new HashMap<>();

        try {
            Integer totalPairs = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM property_similarity", Integer.class);

            Double avgSimilarity = jdbcTemplate.queryForObject(
                    "SELECT AVG(CAST(JSON_EXTRACT(similarity_data, '$.similarity_score') AS DECIMAL(8,4))) " +
                            "FROM property_similarity",
                    Double.class);

            Integer sameClusterPairs = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM property_similarity " +
                            "WHERE JSON_EXTRACT(similarity_data, '$.same_cluster') = true",
                    Integer.class);

            Integer cfPairs = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM property_similarity " +
                            "WHERE JSON_EXTRACT(similarity_data, '$.cf_similarity') IS NOT NULL",
                    Integer.class);

            result.put("success", true);
            result.put("total_similarity_pairs", totalPairs);
            result.put("average_similarity_score", avgSimilarity);
            result.put("same_cluster_pairs", sameClusterPairs);
            result.put("cf_similarity_pairs", cfPairs);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取统计信息失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    // ==================== 用户相似度接口（新增） ====================

    /**
     * 触发全量用户相似度计算（带传递算法）
     * POST /api/similarity/user/calculate
     */
    @PostMapping("/user/calculate")
    public ResponseEntity<Map<String, Object>> calculateUserSimilarity() {
        Map<String, Object> result = new HashMap<>();

        try {
            long startTime = System.currentTimeMillis();
            userSimilarityService.calculateUserSimilarityFull();
            long endTime = System.currentTimeMillis();

            result.put("success", true);
            result.put("message", "用户相似度计算完成（含传递算法）");
            result.put("duration_ms", endTime - startTime);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "计算失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 使用Spark手动触发全量用户相似度计算
     * POST /api/similarity/user/calculate/spark
     */
    @PostMapping("/user/calculate/spark")
    public ResponseEntity<Map<String, Object>> calculateUserSimilarityWithSpark() {
        Map<String, Object> result = new HashMap<>();

        try {
            long startTime = System.currentTimeMillis();
            userSimilaritySparkService.calculateUserSimilarityFull();
            long endTime = System.currentTimeMillis();

            result.put("success", true);
            result.put("message", "用户相似度计算完成（使用Spark，全量计算）");
            result.put("duration_ms", endTime - startTime);
            result.put("algorithm", "spark_full");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "计算失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(result);
        }
    }
    
    /**
     * 使用Spark手动触发增量用户相似度计算（只计算指定用户与其他用户的相似度）
     * POST /api/similarity/user/{userId}/calculate/spark/incremental
     */
    @PostMapping("/user/{userId}/calculate/spark/incremental")
    public ResponseEntity<Map<String, Object>> calculateUserSimilarityIncrementalWithSpark(
            @PathVariable int userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            long startTime = System.currentTimeMillis();
            userSimilaritySparkService.calculateUserSimilarityIncremental(userId);
            long endTime = System.currentTimeMillis();

            result.put("success", true);
            result.put("message", "用户相似度增量计算完成（使用Spark，用户ID: " + userId + "）");
            result.put("duration_ms", endTime - startTime);
            result.put("algorithm", "spark_incremental");
            result.put("user_id", userId);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "增量计算失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 触发单个用户的增量相似度计算
     * POST /api/similarity/user/{userId}/calculate
     */
    @PostMapping("/user/{userId}/calculate")
    public ResponseEntity<Map<String, Object>> calculateUserSimilarityIncremental(
            @PathVariable int userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            userSimilarityService.onUserPreferenceChanged(userId);

            result.put("success", true);
            result.put("message", "用户 " + userId + " 的增量计算已触发（异步执行）");
            result.put("user_id", userId);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "计算失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 获取与指定用户相似的用户列表
     * GET /api/similarity/user/{userId}/similar?limit=10
     */
    @GetMapping("/user/{userId}/similar")
    public ResponseEntity<Map<String, Object>> getSimilarUsers(
            @PathVariable int userId,
            @RequestParam(defaultValue = "10") int limit) {

        Map<String, Object> result = new HashMap<>();

        try {
            List<Map<String, Object>> similarUsers = userSimilarityService.getSimilarUsers(userId, limit);

            result.put("success", true);
            result.put("user_id", userId);
            result.put("similar_users", similarUsers);
            result.put("count", similarUsers.size());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 查询用户相似度
     * GET /api/similarity/user-similarity?userId=1&topN=5
     */
    @GetMapping("/user-similarity")
    public ResponseEntity<Map<String, Object>> getUserSimilarity(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "5") int topN) {

        Map<String, Object> result = new HashMap<>();

        try {
            // 验证参数
            if (userId == null || userId <= 0) {
                result.put("message", "userId不能为空");
                return ResponseEntity.badRequest().body(result);
            }

            // 获取相似用户
            List<Map<String, Object>> similarUsers = userSimilarityService.getSimilarUsers(userId.intValue(), topN);

            // 转换格式以匹配接口文档
            List<Map<String, Object>> items = new ArrayList<>();
            for (Map<String, Object> user : similarUsers) {
                Map<String, Object> item = new HashMap<>();
                item.put("userId", user.get("user_id"));
                item.put("score", user.get("score"));
                items.add(item);
            }

            result.put("items", items);
            result.put("message", "查询成功");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("message", "查询失败");
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 通过相似度传递推荐用户（发现间接相似的用户）
     * GET /api/similarity/user/{userId}/propagation?limit=10
     * 
     * 算法说明：
     * 如果 A 和 B 相似度高，B 和 C 相似度高，
     * 则推断 A 和 C 也可能相似（即使 A 和 C 直接相似度不高）
     */
    @GetMapping("/user/{userId}/propagation")
    public ResponseEntity<Map<String, Object>> getRecommendedUsersByPropagation(
            @PathVariable int userId,
            @RequestParam(defaultValue = "10") int limit) {

        Map<String, Object> result = new HashMap<>();

        try {
            List<Integer> recommendedUsers = userSimilarityService.getRecommendedUsersByPropagation(userId, limit);

            result.put("success", true);
            result.put("user_id", userId);
            result.put("recommended_users", recommendedUsers);
            result.put("count", recommendedUsers.size());
            result.put("algorithm", "similarity_propagation");
            result.put("description", "通过相似度传递发现的间接相似用户");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 获取用户相似度统计信息
     * GET /api/similarity/user/stats
     */
    @GetMapping("/user/stats")
    public ResponseEntity<Map<String, Object>> getUserSimilarityStats() {
        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, Object> stats = userSimilarityService.getSimilarityStats();
            result.put("success", true);
            result.putAll(stats);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 记录用户浏览行为（会累积触发增量计算）
     * POST /api/similarity/user/{userId}/browse/{propertyId}
     */
    @PostMapping("/user/{userId}/browse/{propertyId}")
    public ResponseEntity<Map<String, Object>> recordUserBrowsing(
            @PathVariable int userId,
            @PathVariable int propertyId) {

        Map<String, Object> result = new HashMap<>();

        try {
            userSimilarityService.recordUserBrowsing(userId, propertyId);

            result.put("success", true);
            result.put("message", "浏览行为已记录");
            result.put("user_id", userId);
            result.put("property_id", propertyId);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "记录失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 通知用户退出（触发相似度更新检查）
     * POST /api/similarity/user/{userId}/logout
     */
    @PostMapping("/user/{userId}/logout")
    public ResponseEntity<Map<String, Object>> notifyUserLogout(@PathVariable int userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            userSimilarityService.onUserLogout(userId);

            result.put("success", true);
            result.put("message", "用户退出通知已处理");
            result.put("user_id", userId);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "处理失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    // ==================== 综合接口（新增） ====================

    /**
     * 触发全量计算（房源 + 用户相似度）
     * POST /api/similarity/calculate/all
     */
    @PostMapping("/calculate/all")
    public ResponseEntity<Map<String, Object>> calculateAllSimilarities() {
        Map<String, Object> result = new HashMap<>();
        List<String> messages = new ArrayList<>();
        boolean allSuccess = true;

        long startTime = System.currentTimeMillis();

        // 1. 计算房源相似度
        try {
            similarityService.calculatePropertySimilarityFull();
            cfService.calculatePropertySimilarityCF();
            cfService.mergeSimilarityResults();
            messages.add("房源相似度计算完成");
        } catch (Exception e) {
            messages.add("房源相似度计算失败: " + e.getMessage());
            allSuccess = false;
        }

        // 2. 计算用户相似度
        try {
            userSimilarityService.calculateUserSimilarityFull();
            messages.add("用户相似度计算完成");
        } catch (Exception e) {
            messages.add("用户相似度计算失败: " + e.getMessage());
            allSuccess = false;
        }

        long endTime = System.currentTimeMillis();

        result.put("success", allSuccess);
        result.put("messages", messages);
        result.put("duration_ms", endTime - startTime);

        return ResponseEntity.status(allSuccess ? 200 : 500).body(result);
    }

    /**
     * 获取综合统计信息（房源 + 用户相似度）
     * GET /api/similarity/stats/all
     */
    @GetMapping("/stats/all")
    public ResponseEntity<Map<String, Object>> getAllStats() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 房源相似度统计
            Map<String, Object> propertyStats = new HashMap<>();
            propertyStats.put("total_pairs", jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM property_similarity", Integer.class));
            propertyStats.put("avg_similarity", jdbcTemplate.queryForObject(
                    "SELECT AVG(CAST(JSON_EXTRACT(similarity_data, '$.similarity_score') AS DECIMAL(8,4))) " +
                            "FROM property_similarity",
                    Double.class));

            // 用户相似度统计
            Map<String, Object> userStats = userSimilarityService.getSimilarityStats();

            result.put("success", true);
            result.put("property_similarity", propertyStats);
            result.put("user_similarity", userStats);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
}