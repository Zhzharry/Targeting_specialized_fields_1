package com.example.controller;

import com.example.service.hadoop.HadoopRecommendationService;
import com.example.service.hadoop.HadoopRecommendationService.RecommendationResult;
import com.example.service.hadoop.HadoopRecommendationService.SimilarUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 其他用户也在看接口 - Hadoop版本 (Java 8 兼容版本)
 * 
 * 位置：backend/src/main/java/com/example/controller/OthersAlsoViewedController.java
 * 
 * 该Controller使用Hadoop MapReduce计算的用户相似度和推荐数据
 */
@RestController
@RequestMapping("/api/recommendation")
@CrossOrigin(origins = "*")
public class OthersAlsoViewedController {

    private static final Logger logger = LoggerFactory.getLogger(OthersAlsoViewedController.class);

    @Autowired
    private HadoopRecommendationService hadoopService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 其他用户也在看接口：基于Hadoop计算的用户相似度，推荐相似用户看过或收藏的房源
     * 
     * @param userId        当前用户ID
     * @param limit         返回数量限制，默认5
     * @param excludeViewed 是否排除当前用户已浏览的房源，默认true
     * @param useCache      是否使用Hadoop缓存，默认true
     */
    @GetMapping("/others-also-viewed")
    public ResponseEntity<Map<String, Object>> getOthersAlsoViewed(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "limit", defaultValue = "5") Integer limit,
            @RequestParam(value = "excludeViewed", defaultValue = "true") Boolean excludeViewed,
            @RequestParam(value = "useCache", defaultValue = "true") Boolean useCache) {

        if (userId == null) {
            return buildErrorResponse("userId不能为空", HttpStatus.BAD_REQUEST);
        }

        try {
            List<Map<String, Object>> recommendations;

            // 优先使用Hadoop缓存数据
            if (useCache && hadoopService.isCacheAvailable()) {
                logger.info("使用Hadoop缓存数据获取推荐，用户ID: {}", userId);
                recommendations = getRecommendationsFromHadoopCache(userId, limit, excludeViewed);
            } else {
                // 降级到数据库查询
                logger.info("Hadoop缓存不可用，降级到数据库查询，用户ID: {}", userId);
                recommendations = getRecommendationsFromDatabase(userId, limit, excludeViewed);
            }

            // 如果推荐不足，用热门房源补足
            if (recommendations.size() < limit) {
                int remaining = limit - recommendations.size();
                Set<Long> existingPropertyIds = new HashSet<Long>();
                for (Map<String, Object> r : recommendations) {
                    existingPropertyIds.add((Long) r.get("propertyId"));
                }
                
                List<Map<String, Object>> popularProperties = getPopularProperties(userId, remaining, excludeViewed, existingPropertyIds);
                recommendations.addAll(popularProperties);
            }

            // 构建响应
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("items", recommendations);
            response.put("count", recommendations.size());
            response.put("userId", userId);
            response.put("dataSource", hadoopService.isCacheAvailable() ? "hadoop_cache" : "database");
            response.put("lastHadoopUpdate", hadoopService.getLastUpdateTime());
            response.put("message", "其他用户也在看数据获取成功");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("获取其他用户也在看数据失败", e);
            return buildErrorResponse("获取其他用户也在看数据失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 手动触发Hadoop推荐计算
     */
    @PostMapping("/trigger-hadoop-calculation")
    public ResponseEntity<Map<String, Object>> triggerHadoopCalculation() {
        try {
            logger.info("手动触发Hadoop推荐计算");
            hadoopService.runFullRecommendationPipeline();
            
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("message", "Hadoop推荐计算已完成");
            response.put("lastUpdateTime", hadoopService.getLastUpdateTime());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Hadoop推荐计算失败", e);
            return buildErrorResponse("Hadoop推荐计算失败: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 获取Hadoop计算状态
     */
    @GetMapping("/hadoop-status")
    public ResponseEntity<Map<String, Object>> getHadoopStatus() {
        Map<String, Object> status = new HashMap<String, Object>();
        status.put("cacheAvailable", hadoopService.isCacheAvailable());
        status.put("lastUpdateTime", hadoopService.getLastUpdateTime());
        return ResponseEntity.ok(status);
    }

    /**
     * 从Hadoop缓存获取推荐
     */
    private List<Map<String, Object>> getRecommendationsFromHadoopCache(
            Long userId, int limit, boolean excludeViewed) {

        // 获取用户已浏览的房源（用于排除）
        Set<Long> viewedPropertyIds = excludeViewed ? getUserViewedProperties(userId) : Collections.<Long>emptySet();

        // 从Hadoop缓存获取推荐
        List<RecommendationResult> cacheResults = hadoopService.getRecommendationsFromCache(userId, limit * 2);

        List<Map<String, Object>> recommendations = new ArrayList<Map<String, Object>>();
        
        for (RecommendationResult result : cacheResults) {
            if (recommendations.size() >= limit) break;
            
            if (excludeViewed && viewedPropertyIds.contains(result.propertyId)) {
                continue;
            }

            // 查询房源详细信息
            Map<String, Object> propertyInfo = getPropertyDetails(result.propertyId);
            if (propertyInfo != null) {
                propertyInfo.put("recommendationScore", result.score);
                propertyInfo.put("reason", result.reason);
                propertyInfo.put("source", "hadoop");
                recommendations.add(propertyInfo);
            }
        }

        return recommendations;
    }

    /**
     * 从数据库获取推荐（降级方案）
     */
    private List<Map<String, Object>> getRecommendationsFromDatabase(
            Long userId, int limit, boolean excludeViewed) {

        // 1. 获取相似用户
        List<SimilarUser> similarUsers = getSimilarUsersFromDatabase(userId, 10);
        
        if (similarUsers.isEmpty()) {
            return new ArrayList<Map<String, Object>>();
        }

        // 2. 获取相似用户浏览/收藏的房源
        List<Long> similarUserIds = new ArrayList<Long>();
        for (SimilarUser u : similarUsers) {
            similarUserIds.add(u.userId);
        }
        
        Map<Long, Double> userSimilarityMap = new HashMap<Long, Double>();
        for (SimilarUser u : similarUsers) {
            userSimilarityMap.put(u.userId, u.similarity);
        }

        // 获取用户已浏览的房源
        Set<Long> viewedPropertyIds = excludeViewed ? getUserViewedProperties(userId) : Collections.<Long>emptySet();

        // 构建SQL查询相似用户的行为
        String placeholders = String.join(",", Collections.nCopies(similarUserIds.size(), "?"));
        
        String sql = String.format(
            "SELECT " +
            "p.property_id, " +
            "p.title, " +
            "p.price_info, " +
            "p.layout_info, " +
            "p.basic_info, " +
            "c.name as community_name, " +
            "c.location_info, " +
            "GROUP_CONCAT(DISTINCT bh.user_id) as viewer_ids, " +
            "GROUP_CONCAT(DISTINCT f.user_id) as favorite_ids, " +
            "COUNT(DISTINCT bh.user_id) as view_count, " +
            "COUNT(DISTINCT f.user_id) as favorite_count " +
            "FROM properties p " +
            "LEFT JOIN communities c ON p.community_id = c.community_id " +
            "LEFT JOIN browsing_history bh ON p.property_id = bh.property_id " +
            "AND bh.user_id IN (%s) " +
            "LEFT JOIN favorites f ON p.property_id = f.property_id " +
            "AND f.user_id IN (%s) " +
            "WHERE p.status = 'for_sale' " +
            "AND (bh.user_id IS NOT NULL OR f.user_id IS NOT NULL) " +
            "GROUP BY p.property_id " +
            "HAVING view_count > 0 OR favorite_count > 0 " +
            "ORDER BY (view_count * 1.0 + favorite_count * 2.0) DESC " +
            "LIMIT ?", placeholders, placeholders);

        List<Object> params = new ArrayList<Object>(similarUserIds);
        params.addAll(similarUserIds);
        params.add(limit * 3); // 多查一些用于筛选

        List<Map<String, Object>> recommendations = new ArrayList<Map<String, Object>>();
        final Set<Long> finalViewedPropertyIds = viewedPropertyIds;

        jdbcTemplate.query(sql, params.toArray(), rs -> {
            if (recommendations.size() >= limit) return;
            
            Long propertyId = rs.getLong("property_id");
            
            if (excludeViewed && finalViewedPropertyIds.contains(propertyId)) {
                return;
            }

            Map<String, Object> item = buildRecommendationItem(rs, userSimilarityMap);
            item.put("source", "database");
            recommendations.add(item);
        });

        return recommendations;
    }

    /**
     * 从数据库获取相似用户
     */
    private List<SimilarUser> getSimilarUsersFromDatabase(Long userId, int limit) {
        // 先尝试从Hadoop缓存获取
        if (hadoopService.isCacheAvailable()) {
            return hadoopService.getSimilarUsersFromCache(userId, limit);
        }
        
        // 降级到数据库查询
        String sql = 
            "SELECT " +
            "CASE WHEN user_id1 = ? THEN user_id2 ELSE user_id1 END as similar_user_id, " +
            "JSON_EXTRACT(similarity_data, '$.similarity_score') as similarity_score " +
            "FROM user_similarity " +
            "WHERE (user_id1 = ? OR user_id2 = ?) " +
            "AND JSON_EXTRACT(similarity_data, '$.similarity_score') > 0.3 " +
            "ORDER BY similarity_score DESC " +
            "LIMIT ?";

        return jdbcTemplate.query(sql, 
            new Object[]{userId, userId, userId, limit},
            (rs, rowNum) -> new SimilarUser(
                rs.getLong("similar_user_id"),
                rs.getDouble("similarity_score")
            ));
    }

    /**
     * 获取用户已浏览的房源ID集合
     */
    private Set<Long> getUserViewedProperties(Long userId) {
        String sql = "SELECT DISTINCT property_id FROM browsing_history WHERE user_id = ?";
        List<Long> propertyIds = jdbcTemplate.queryForList(sql, Long.class, userId);
        return new HashSet<Long>(propertyIds);
    }

    /**
     * 获取房源详细信息
     */
    private Map<String, Object> getPropertyDetails(Long propertyId) {
        String sql = 
            "SELECT " +
            "p.property_id, " +
            "p.title, " +
            "p.price_info, " +
            "p.layout_info, " +
            "p.basic_info, " +
            "c.name as community_name, " +
            "c.location_info " +
            "FROM properties p " +
            "LEFT JOIN communities c ON p.community_id = c.community_id " +
            "WHERE p.property_id = ? AND p.status = 'for_sale'";

        List<Map<String, Object>> results = jdbcTemplate.query(sql, new Object[]{propertyId}, (rs, rowNum) -> {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("propertyId", rs.getLong("property_id"));
            item.put("title", rs.getString("title"));
            
            // 构建summary
            String communityName = rs.getString("community_name");
            Double area = extractJsonDouble(rs.getString("layout_info"), "area");
            Integer bedroomCount = extractJsonInt(rs.getString("layout_info"), "bedroom_count");
            Integer livingRoomCount = extractJsonInt(rs.getString("layout_info"), "living_room_count");
            
            String summary = String.format("%s · %.0f㎡ · %d室%d厅",
                communityName != null ? communityName : "未知小区",
                area != null ? area : 0,
                bedroomCount != null ? bedroomCount : 0,
                livingRoomCount != null ? livingRoomCount : 0);
            item.put("summary", summary);
            
            // 价格
            Double totalPrice = extractJsonDouble(rs.getString("price_info"), "total_price");
            item.put("totalPrice", totalPrice != null ? totalPrice : 0);
            
            // 图片和链接
            item.put("cover", "https://picsum.photos/seed/" + propertyId + "/300/200");
            item.put("detailUrl", "/property/" + propertyId);
            
            return item;
        });

        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 构建推荐项
     */
    private Map<String, Object> buildRecommendationItem(java.sql.ResultSet rs, Map<Long, Double> userSimilarityMap) 
            throws java.sql.SQLException {
        
        Map<String, Object> item = new HashMap<String, Object>();
        Long propertyId = rs.getLong("property_id");
        
        item.put("propertyId", propertyId);
        item.put("title", rs.getString("title"));
        
        // 构建summary
        String communityName = rs.getString("community_name");
        Double area = extractJsonDouble(rs.getString("layout_info"), "area");
        Integer bedroomCount = extractJsonInt(rs.getString("layout_info"), "bedroom_count");
        Integer livingRoomCount = extractJsonInt(rs.getString("layout_info"), "living_room_count");
        
        String summary = String.format("%s · %.0f㎡ · %d室%d厅",
            communityName != null ? communityName : "未知小区",
            area != null ? area : 0,
            bedroomCount != null ? bedroomCount : 0,
            livingRoomCount != null ? livingRoomCount : 0);
        item.put("summary", summary);
        
        // 价格
        Double totalPrice = extractJsonDouble(rs.getString("price_info"), "total_price");
        item.put("totalPrice", totalPrice != null ? totalPrice : 0);
        
        // 图片和链接
        item.put("cover", "https://picsum.photos/seed/" + propertyId + "/300/200");
        item.put("detailUrl", "/property/" + propertyId);
        
        // 统计信息
        int viewCount = rs.getInt("view_count");
        int favoriteCount = rs.getInt("favorite_count");
        
        // 标签
        List<String> tags = new ArrayList<String>();
        tags.add("相似用户推荐");
        if (viewCount > 5) tags.add("多人浏览");
        if (favoriteCount > 2) tags.add("多人收藏");
        if (viewCount > 10) tags.add("热门房源");
        item.put("tags", tags);
        
        // 计算推荐分数和理由
        double recommendationScore = calculateRecommendationScore(
            rs.getString("viewer_ids"), 
            rs.getString("favorite_ids"), 
            userSimilarityMap);
        item.put("recommendationScore", recommendationScore);
        
        String reason = calculateRecommendationReason(viewCount, favoriteCount);
        item.put("reason", reason);
        
        // 统计
        Map<String, Object> stats = new HashMap<String, Object>();
        stats.put("viewCount", viewCount);
        stats.put("favoriteCount", favoriteCount);
        item.put("stats", stats);
        
        return item;
    }

    /**
     * 计算推荐分数
     */
    private double calculateRecommendationScore(String viewerIds, String favoriteIds, Map<Long, Double> userSimilarityMap) {
        double score = 0.0;
        
        if (viewerIds != null && !viewerIds.isEmpty()) {
            for (String id : viewerIds.split(",")) {
                try {
                    Long userId = Long.parseLong(id.trim());
                    Double similarity = userSimilarityMap.get(userId);
                    if (similarity != null) {
                        score += similarity * 1.0;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        
        if (favoriteIds != null && !favoriteIds.isEmpty()) {
            for (String id : favoriteIds.split(",")) {
                try {
                    Long userId = Long.parseLong(id.trim());
                    Double similarity = userSimilarityMap.get(userId);
                    if (similarity != null) {
                        score += similarity * 2.0;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        
        return Math.min(1.0, score);
    }

    /**
     * 计算推荐理由
     */
    private String calculateRecommendationReason(int viewCount, int favoriteCount) {
        List<String> reasons = new ArrayList<String>();
        
        if (viewCount >= 1) {
            reasons.add(viewCount + "位相似用户浏览过");
        }
        if (favoriteCount >= 1) {
            reasons.add(favoriteCount + "位相似用户收藏过");
        }
        
        if (reasons.isEmpty()) {
            return "根据相似用户行为推荐";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < reasons.size(); i++) {
            if (i > 0) sb.append("，");
            sb.append(reasons.get(i));
        }
        return sb.toString();
    }

    /**
     * 获取热门房源（后备方案）
     */
    private List<Map<String, Object>> getPopularProperties(
            Long userId, int limit, boolean excludeViewed, Set<Long> excludePropertyIds) {
        
        StringBuilder sql = new StringBuilder(
            "SELECT " +
            "p.property_id, " +
            "p.title, " +
            "p.price_info, " +
            "p.layout_info, " +
            "p.basic_info, " +
            "c.name as community_name, " +
            "c.location_info, " +
            "COUNT(DISTINCT bh.user_id) as view_count " +
            "FROM properties p " +
            "LEFT JOIN communities c ON p.community_id = c.community_id " +
            "LEFT JOIN browsing_history bh ON p.property_id = bh.property_id " +
            "AND bh.created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
            "WHERE p.status = 'for_sale' ");
        
        List<Object> params = new ArrayList<Object>();
        
        if (excludeViewed) {
            sql.append("AND NOT EXISTS (SELECT 1 FROM browsing_history bh2 WHERE bh2.user_id = ? AND bh2.property_id = p.property_id) ");
            params.add(userId);
        }
        
        if (!excludePropertyIds.isEmpty()) {
            String placeholders = String.join(",", Collections.nCopies(excludePropertyIds.size(), "?"));
            sql.append("AND p.property_id NOT IN (").append(placeholders).append(") ");
            params.addAll(excludePropertyIds);
        }
        
        sql.append("GROUP BY p.property_id ORDER BY view_count DESC LIMIT ?");
        params.add(limit);
        
        List<Map<String, Object>> properties = new ArrayList<Map<String, Object>>();
        
        jdbcTemplate.query(sql.toString(), params.toArray(), rs -> {
            Map<String, Object> item = new HashMap<String, Object>();
            Long propertyId = rs.getLong("property_id");
            
            item.put("propertyId", propertyId);
            item.put("title", rs.getString("title"));
            
            // 构建summary
            String communityName = rs.getString("community_name");
            Double area = extractJsonDouble(rs.getString("layout_info"), "area");
            Integer bedroomCount = extractJsonInt(rs.getString("layout_info"), "bedroom_count");
            Integer livingRoomCount = extractJsonInt(rs.getString("layout_info"), "living_room_count");
            
            String summary = String.format("%s · %.0f㎡ · %d室%d厅",
                communityName != null ? communityName : "未知小区",
                area != null ? area : 0,
                bedroomCount != null ? bedroomCount : 0,
                livingRoomCount != null ? livingRoomCount : 0);
            item.put("summary", summary);
            
            // 价格
            Double totalPrice = extractJsonDouble(rs.getString("price_info"), "total_price");
            item.put("totalPrice", totalPrice != null ? totalPrice : 0);
            
            // 图片和链接
            item.put("cover", "https://picsum.photos/seed/" + propertyId + "/300/200");
            item.put("detailUrl", "/property/" + propertyId);
            
            // 标签
            List<String> tags = new ArrayList<String>();
            tags.add("热门房源");
            int viewCount = rs.getInt("view_count");
            if (viewCount > 10) tags.add("近期热门");
            item.put("tags", tags);
            
            // 推荐理由
            item.put("reason", "近期" + viewCount + "人浏览过");
            item.put("source", "popular");
            
            properties.add(item);
        });
        
        return properties;
    }

    /**
     * 从JSON字符串提取Double值
     */
    @SuppressWarnings("unchecked")
    private Double extractJsonDouble(String json, String key) {
        if (json == null || json.isEmpty()) return null;
        try {
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            Object value = map.get(key);
            if (value instanceof Number) return ((Number) value).doubleValue();
            if (value instanceof String) return Double.parseDouble((String) value);
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * 从JSON字符串提取Integer值
     */
    @SuppressWarnings("unchecked")
    private Integer extractJsonInt(String json, String key) {
        if (json == null || json.isEmpty()) return null;
        try {
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            Object value = map.get(key);
            if (value instanceof Number) return ((Number) value).intValue();
            if (value instanceof String) return Integer.parseInt((String) value);
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * 构建错误响应
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(String message, HttpStatus status) {
        Map<String, Object> error = new HashMap<String, Object>();
        error.put("message", message);
        error.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(status).body(error);
    }
}