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
import java.sql.SQLException;
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

                List<Map<String, Object>> popularProperties = getPopularProperties(userId, remaining, excludeViewed,
                        existingPropertyIds);
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
            if (recommendations.size() >= limit)
                break;

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
                        "LIMIT ?",
                placeholders, placeholders);

        List<Object> params = new ArrayList<Object>(similarUserIds);
        params.addAll(similarUserIds);
        params.add(limit * 3); // 多查一些用于筛选

        List<Map<String, Object>> recommendations = new ArrayList<Map<String, Object>>();
        final Set<Long> finalViewedPropertyIds = viewedPropertyIds;

        jdbcTemplate.query(sql, params.toArray(), rs -> {
            if (recommendations.size() >= limit)
                return;

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
        // 基于用户浏览和收藏行为计算相似度
        String sql = "SELECT " +
                "bh2.user_id, " +
                "COUNT(DISTINCT bh1.property_id) as common_views, " +
                "COUNT(DISTINCT CASE WHEN f1.property_id IS NOT NULL AND f2.property_id IS NOT NULL THEN f1.property_id END) as common_favorites "
                +
                "FROM browsing_history bh1 " +
                "INNER JOIN browsing_history bh2 ON bh1.property_id = bh2.property_id AND bh1.user_id != bh2.user_id " +
                "LEFT JOIN favorites f1 ON f1.user_id = bh1.user_id AND f1.property_id = bh1.property_id " +
                "LEFT JOIN favorites f2 ON f2.user_id = bh2.user_id AND f2.property_id = bh1.property_id " +
                "WHERE bh1.user_id = ? " +
                "GROUP BY bh2.user_id " +
                "HAVING common_views > 0 OR common_favorites > 0 " +
                "ORDER BY (common_views * 1.0 + common_favorites * 2.0) DESC " +
                "LIMIT ?";

        List<SimilarUser> similarUsers = new ArrayList<SimilarUser>();

        jdbcTemplate.query(sql, new Object[] { userId, limit }, (rs, rowNum) -> {
            Long similarUserId = rs.getLong("user_id");
            int commonViews = rs.getInt("common_views");
            int commonFavorites = rs.getInt("common_favorites");

            // 计算相似度：基于共同浏览和收藏的数量
            // 使用Jaccard相似度的简化版本
            double similarityValue = Math.min(1.0, (commonViews * 1.0 + commonFavorites * 2.0) / 10.0);
            Double similarity = Double.valueOf(similarityValue);

            SimilarUser similarUser = new SimilarUser(similarUserId, similarity);

            similarUsers.add(similarUser);
            return similarUser;
        });

        return similarUsers;
    }

    /**
     * 获取房源详细信息（包含特征）
     */
    private Map<String, Object> getPropertyDetailWithFeatures(Long propertyId) {
        String sql = "SELECT " +
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

        List<Map<String, Object>> results = jdbcTemplate.query(sql, new Object[] { propertyId }, (rs, rowNum) -> {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("propertyId", rs.getLong("property_id"));
            item.put("title", rs.getString("title"));

            // 价格信息
            Double totalPrice = extractJsonDouble(rs.getString("price_info"), "total_price");
            Double unitPrice = extractJsonDouble(rs.getString("price_info"), "unit_price");
            item.put("totalPrice", totalPrice != null ? totalPrice : 0);
            item.put("unitPrice", unitPrice != null ? unitPrice : 0);

            // 布局信息
            Double area = extractJsonDouble(rs.getString("layout_info"), "area");
            Integer bedroomCount = extractJsonInt(rs.getString("layout_info"), "bedroom_count");
            Integer livingRoomCount = extractJsonInt(rs.getString("layout_info"), "living_room_count");
            Integer bathroomCount = extractJsonInt(rs.getString("layout_info"), "bathroom_count");
            item.put("area", area != null ? area : 0);
            item.put("bedroomCount", bedroomCount != null ? bedroomCount : 0);
            item.put("livingRoomCount", livingRoomCount != null ? livingRoomCount : 0);
            item.put("bathroomCount", bathroomCount != null ? bathroomCount : 0);

            // 位置信息
            String locationInfo = rs.getString("location_info");
            String communityName = rs.getString("community_name");
            item.put("communityName", communityName);
            item.put("locationInfo", locationInfo);

            // 解析位置信息中的坐标（如果存在）
            if (locationInfo != null && !locationInfo.isEmpty()) {
                try {
                    Map<String, Object> locationMap = objectMapper.readValue(locationInfo, Map.class);
                    Object latObj = locationMap.get("latitude");
                    Object lngObj = locationMap.get("longitude");
                    if (latObj != null)
                        item.put("latitude", latObj instanceof Number ? ((Number) latObj).doubleValue()
                                : Double.parseDouble(latObj.toString()));
                    if (lngObj != null)
                        item.put("longitude", lngObj instanceof Number ? ((Number) lngObj).doubleValue()
                                : Double.parseDouble(lngObj.toString()));
                } catch (Exception ignored) {
                }
            }

            // 基本信息
            String basicInfo = rs.getString("basic_info");
            if (basicInfo != null && !basicInfo.isEmpty()) {
                try {
                    Map<String, Object> basicMap = objectMapper.readValue(basicInfo, Map.class);
                    item.put("basicInfo", basicMap);
                } catch (Exception ignored) {
                }
            }

            return item;
        });

        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 获取候选房源
     */
    private List<Map<String, Object>> getCandidateProperties(Long sourceId, Set<Long> excludedIds, int limit) {
        StringBuilder sql = new StringBuilder(
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
                        "WHERE p.status = 'for_sale' " +
                        "AND p.property_id != ? ");

        List<Object> params = new ArrayList<Object>();
        params.add(sourceId);

        if (!excludedIds.isEmpty()) {
            String placeholders = String.join(",", Collections.nCopies(excludedIds.size(), "?"));
            sql.append("AND p.property_id NOT IN (").append(placeholders).append(") ");
            params.addAll(excludedIds);
        }

        sql.append("ORDER BY RAND() LIMIT ?");
        params.add(limit);

        List<Map<String, Object>> candidates = new ArrayList<Map<String, Object>>();

        jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
            Map<String, Object> item = new HashMap<String, Object>();
            Long propertyId = rs.getLong("property_id");
            item.put("propertyId", propertyId);
            item.put("title", rs.getString("title"));

            // 价格信息
            Double totalPrice = extractJsonDouble(rs.getString("price_info"), "total_price");
            Double unitPrice = extractJsonDouble(rs.getString("price_info"), "unit_price");
            item.put("totalPrice", totalPrice != null ? totalPrice : 0);
            item.put("unitPrice", unitPrice != null ? unitPrice : 0);

            // 布局信息
            Double area = extractJsonDouble(rs.getString("layout_info"), "area");
            Integer bedroomCount = extractJsonInt(rs.getString("layout_info"), "bedroom_count");
            Integer livingRoomCount = extractJsonInt(rs.getString("layout_info"), "living_room_count");
            Integer bathroomCount = extractJsonInt(rs.getString("layout_info"), "bathroom_count");
            item.put("area", area != null ? area : 0);
            item.put("bedroomCount", bedroomCount != null ? bedroomCount : 0);
            item.put("livingRoomCount", livingRoomCount != null ? livingRoomCount : 0);
            item.put("bathroomCount", bathroomCount != null ? bathroomCount : 0);

            // 位置信息
            String locationInfo = rs.getString("location_info");
            String communityName = rs.getString("community_name");
            item.put("communityName", communityName);
            item.put("locationInfo", locationInfo);

            // 解析位置信息中的坐标（如果存在）
            if (locationInfo != null && !locationInfo.isEmpty()) {
                try {
                    Map<String, Object> locationMap = objectMapper.readValue(locationInfo, Map.class);
                    Object latObj = locationMap.get("latitude");
                    Object lngObj = locationMap.get("longitude");
                    if (latObj != null)
                        item.put("latitude", latObj instanceof Number ? ((Number) latObj).doubleValue()
                                : Double.parseDouble(latObj.toString()));
                    if (lngObj != null)
                        item.put("longitude", lngObj instanceof Number ? ((Number) lngObj).doubleValue()
                                : Double.parseDouble(lngObj.toString()));
                } catch (Exception ignored) {
                }
            }

            candidates.add(item);
            return item;
        });

        return candidates;
    }

    /**
     * 计算综合相似度
     */
    private double calculateComprehensiveSimilarity(Map<String, Object> sourceProperty, Map<String, Object> candidate) {
        double totalSimilarity = 0.0;
        double totalWeight = 0.0;

        // 1. 位置相似度 (权重 0.35)
        double locationSimilarity = calculateLocationSimilarity(sourceProperty, candidate);
        totalSimilarity += locationSimilarity * 0.35;
        totalWeight += 0.35;

        // 2. 价格相似度 (权重 0.30)
        double priceSimilarity = calculatePriceSimilarity(sourceProperty, candidate);
        totalSimilarity += priceSimilarity * 0.30;
        totalWeight += 0.30;

        // 3. 布局相似度 (权重 0.25)
        double layoutSimilarity = calculateLayoutSimilarity(sourceProperty, candidate);
        totalSimilarity += layoutSimilarity * 0.25;
        totalWeight += 0.25;

        // 4. 面积相似度 (权重 0.10)
        double areaSimilarity = calculateAreaSimilarity(sourceProperty, candidate);
        totalSimilarity += areaSimilarity * 0.10;
        totalWeight += 0.10;

        return totalWeight > 0 ? totalSimilarity / totalWeight : 0.0;
    }

    /**
     * 计算位置相似度
     */
    private double calculateLocationSimilarity(Map<String, Object> source, Map<String, Object> candidate) {
        String sourceCommunity = (String) source.get("communityName");
        String candidateCommunity = (String) candidate.get("communityName");

        // 如果同一小区，相似度为1.0
        if (sourceCommunity != null && sourceCommunity.equals(candidateCommunity)) {
            return 1.0;
        }

        // 如果有坐标信息，计算距离
        Object sourceLat = source.get("latitude");
        Object sourceLng = source.get("longitude");
        Object candidateLat = candidate.get("latitude");
        Object candidateLng = candidate.get("longitude");

        if (sourceLat != null && sourceLng != null && candidateLat != null && candidateLng != null) {
            try {
                double lat1 = sourceLat instanceof Number ? ((Number) sourceLat).doubleValue()
                        : Double.parseDouble(sourceLat.toString());
                double lng1 = sourceLng instanceof Number ? ((Number) sourceLng).doubleValue()
                        : Double.parseDouble(sourceLng.toString());
                double lat2 = candidateLat instanceof Number ? ((Number) candidateLat).doubleValue()
                        : Double.parseDouble(candidateLat.toString());
                double lng2 = candidateLng instanceof Number ? ((Number) candidateLng).doubleValue()
                        : Double.parseDouble(candidateLng.toString());

                // 计算距离（简化版，使用欧几里得距离）
                double distance = Math.sqrt(Math.pow(lat1 - lat2, 2) + Math.pow(lng1 - lng2, 2));

                // 距离越近，相似度越高（假设0.01度约等于1公里）
                return Math.max(0.0, 1.0 - distance / 0.1);
            } catch (Exception ignored) {
            }
        }

        // 默认相似度
        return 0.3;
    }

    /**
     * 计算价格相似度
     */
    private double calculatePriceSimilarity(Map<String, Object> source, Map<String, Object> candidate) {
        Object sourcePriceObj = source.get("totalPrice");
        Object candidatePriceObj = candidate.get("totalPrice");

        if (sourcePriceObj == null || candidatePriceObj == null) {
            return 0.5;
        }

        double sourcePrice = sourcePriceObj instanceof Number ? ((Number) sourcePriceObj).doubleValue()
                : Double.parseDouble(sourcePriceObj.toString());
        double candidatePrice = candidatePriceObj instanceof Number ? ((Number) candidatePriceObj).doubleValue()
                : Double.parseDouble(candidatePriceObj.toString());

        if (sourcePrice == 0 || candidatePrice == 0) {
            return 0.5;
        }

        // 计算价格差异比例
        double priceRatio = Math.min(sourcePrice, candidatePrice) / Math.max(sourcePrice, candidatePrice);

        return priceRatio;
    }

    /**
     * 计算布局相似度
     */
    private double calculateLayoutSimilarity(Map<String, Object> source, Map<String, Object> candidate) {
        int sourceBedroom = getIntValue(source, "bedroomCount", 0);
        int candidateBedroom = getIntValue(candidate, "bedroomCount", 0);
        int sourceLiving = getIntValue(source, "livingRoomCount", 0);
        int candidateLiving = getIntValue(candidate, "livingRoomCount", 0);
        int sourceBathroom = getIntValue(source, "bathroomCount", 0);
        int candidateBathroom = getIntValue(candidate, "bathroomCount", 0);

        // 计算各项的匹配度
        double bedroomMatch = sourceBedroom == candidateBedroom ? 1.0
                : (Math.abs(sourceBedroom - candidateBedroom) == 1 ? 0.7 : 0.3);
        double livingMatch = sourceLiving == candidateLiving ? 1.0
                : (Math.abs(sourceLiving - candidateLiving) == 1 ? 0.7 : 0.3);
        double bathroomMatch = sourceBathroom == candidateBathroom ? 1.0
                : (Math.abs(sourceBathroom - candidateBathroom) == 1 ? 0.7 : 0.3);

        // 加权平均
        return (bedroomMatch * 0.5 + livingMatch * 0.3 + bathroomMatch * 0.2);
    }

    /**
     * 计算面积相似度
     */
    private double calculateAreaSimilarity(Map<String, Object> source, Map<String, Object> candidate) {
        double sourceArea = getDoubleValue(source, "area", 0);
        double candidateArea = getDoubleValue(candidate, "area", 0);

        if (sourceArea == 0 || candidateArea == 0) {
            return 0.5;
        }

        // 计算面积差异比例
        double areaRatio = Math.min(sourceArea, candidateArea) / Math.max(sourceArea, candidateArea);

        return areaRatio;
    }

    /**
     * 获取整数值（辅助方法）
     */
    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value == null)
            return defaultValue;
        if (value instanceof Number)
            return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 获取双精度值（辅助方法）
     */
    private double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value == null)
            return defaultValue;
        if (value instanceof Number)
            return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
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
        String sql = "SELECT " +
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

        List<Map<String, Object>> results = jdbcTemplate.query(sql, new Object[] { propertyId }, (rs, rowNum) -> {
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
        if (viewCount > 5)
            tags.add("多人浏览");
        if (favoriteCount > 2)
            tags.add("多人收藏");
        if (viewCount > 10)
            tags.add("热门房源");
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
    private double calculateRecommendationScore(String viewerIds, String favoriteIds,
            Map<Long, Double> userSimilarityMap) {
        double score = 0.0;

        if (viewerIds != null && !viewerIds.isEmpty()) {
            for (String id : viewerIds.split(",")) {
                try {
                    Long userId = Long.parseLong(id.trim());
                    Double similarity = userSimilarityMap.get(userId);
                    if (similarity != null) {
                        score += similarity * 1.0;
                    }
                } catch (NumberFormatException ignored) {
                }
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
                } catch (NumberFormatException ignored) {
                }
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
            if (i > 0)
                sb.append("，");
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
            sql.append(
                    "AND NOT EXISTS (SELECT 1 FROM browsing_history bh2 WHERE bh2.user_id = ? AND bh2.property_id = p.property_id) ");
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
            if (viewCount > 10)
                tags.add("近期热门");
            item.put("tags", tags);

            // 推荐理由
            item.put("reason", "近期" + viewCount + "人浏览过");
            item.put("source", "popular");

            // 为热门房源设置默认相似度分数（基于浏览量计算，范围0.1-0.5）
            double score = Math.max(0.1, Math.min(0.5, viewCount / 20.0));
            item.put("score", score);

            properties.add(item);
        });

        return properties;
    }

    /**
     * 从JSON字符串提取Double值
     */
    @SuppressWarnings("unchecked")
    private Double extractJsonDouble(String json, String key) {
        if (json == null || json.isEmpty())
            return null;
        try {
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            Object value = map.get(key);
            if (value instanceof Number)
                return ((Number) value).doubleValue();
            if (value instanceof String)
                return Double.parseDouble((String) value);
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 从JSON字符串提取Integer值
     */
    @SuppressWarnings("unchecked")
    private Integer extractJsonInt(String json, String key) {
        if (json == null || json.isEmpty())
            return null;
        try {
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            Object value = map.get(key);
            if (value instanceof Number)
                return ((Number) value).intValue();
            if (value instanceof String)
                return Integer.parseInt((String) value);
        } catch (Exception ignored) {
        }
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