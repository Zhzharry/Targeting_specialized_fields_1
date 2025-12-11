package com.example.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 主页对应的接口控制器。
 * 提供首页展示所需的数据：个人信息和猜你喜欢。
 */
@RestController
@RequestMapping("/api/home")
@CrossOrigin(origins = "http://localhost:5173")
public class HomeController {

    private final DataSource dataSource;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public HomeController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 获取首页概览数据（静态示例）。
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        Map<String, Object> overview = new HashMap<String, Object>();
        overview.put("title", "首页概览");
        overview.put("welcomeMessage", "欢迎访问首页");
        overview.put("notifications", 2);
        overview.put("shortcuts", Arrays.asList("我的", "猜你喜欢", "数据分析"));
        return ResponseEntity.ok(overview);
    }

    /**
     * “我的”接口：返回用户的详细信息，用于跳转个人主页。
     * 需要前端传入 userId 并从 users / user_preferences 表中读取信息。
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyProfile(@RequestParam("userId") Long userId) {
        String userSql = "SELECT user_id, username, user_profile, created_at FROM users WHERE user_id = ?";
        String preferenceSql = "SELECT preference_data FROM user_preferences WHERE user_id = ? ORDER BY preference_id DESC LIMIT 1";

        try (Connection connection = getConnection();
             PreparedStatement userStatement = connection.prepareStatement(userSql);
             PreparedStatement preferenceStatement = connection.prepareStatement(preferenceSql)) {

            userStatement.setLong(1, userId);
            try (ResultSet userResult = userStatement.executeQuery()) {
                if (!userResult.next()) {
                    return ResponseEntity.notFound().build();
                }

                Map<String, Object> profile = new HashMap<String, Object>();
                profile.put("userId", userResult.getLong("user_id"));
                profile.put("username", userResult.getString("username"));
                profile.put("joinedAt", userResult.getTimestamp("created_at"));

                String userProfileJson = userResult.getString("user_profile");
                profile.put("userProfile", parseJson(userProfileJson));

                preferenceStatement.setLong(1, userId);
                try (ResultSet preferenceResult = preferenceStatement.executeQuery()) {
                    if (preferenceResult.next()) {
                        profile.put("preferences", parseJson(preferenceResult.getString("preference_data")));
                    }
                }

                // 额外的统计信息（示例数据，可根据需求从数据库计算）
                profile.put("stats", getDefaultStats());

                Map<String, Object> response = new HashMap<String, Object>();
                response.put("profile", profile);
                response.put("message", "个人信息获取成功");
                return ResponseEntity.ok(response);
            }
        } catch (SQLException e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("message", "获取个人信息失败");
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 猜你喜欢接口：返回前端展示的房源推荐（当前使用静态示例数据）。
     */
    @GetMapping("/guess-you-like")
    public ResponseEntity<Map<String, Object>> guessYouLike() {
        return ResponseEntity.ok(buildRecommendationResponse("猜你喜欢数据（示例）"));
    }


    /**
     * 其他用户也在看接口：基于用户相似度，推荐相似用户看过或收藏的房源
     */
    @GetMapping("/others-also-viewed")
    public ResponseEntity<Map<String, Object>> getOthersAlsoViewed(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "limit", defaultValue = "5") Integer limit,
            @RequestParam(value = "excludeViewed", defaultValue = "true") Boolean excludeViewed) {
        
        if (userId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "userId不能为空");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        
        try {
            // 1. 获取相似用户（Top 10个最相似的用户）
            List<Long> similarUserIds = getSimilarUserIds(userId, 10);
            
            List<Map<String, Object>> recommendations;
            
            if (similarUserIds.isEmpty()) {
                // 如果没有相似用户，返回热门房源作为后备
                recommendations = getPopularProperties(userId, limit, excludeViewed);
            } else {
                // 2. 从相似用户获取房源推荐
                recommendations = getPropertiesFromSimilarUsers(userId, similarUserIds, limit, excludeViewed);
                
                // 3. 如果推荐不足，用热门房源补足
                if (recommendations.size() < limit) {
                    int remaining = limit - recommendations.size();
                    List<Map<String, Object>> popularProperties = getPopularProperties(userId, remaining, excludeViewed);
                    recommendations.addAll(popularProperties);
                }
            }
            
            // 4. 构建响应
            Map<String, Object> body = new HashMap<>();
            body.put("items", recommendations);
            body.put("count", recommendations.size());
            body.put("userId", userId);
            body.put("message", "其他用户也在看数据获取成功");
            
            return ResponseEntity.ok(body);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "获取其他用户也在看数据失败");
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 获取相似用户ID列表
     */
    private List<Long> getSimilarUserIds(Long userId, int limit) throws SQLException {
        String sql = "SELECT " +
                    "CASE WHEN user_id1 = ? THEN user_id2 ELSE user_id1 END as similar_user_id, " +
                    "JSON_EXTRACT(similarity_data, '$.similarity_score') as similarity_score " +
                    "FROM user_similarity " +
                    "WHERE (user_id1 = ? OR user_id2 = ?) " +
                    "AND JSON_EXTRACT(similarity_data, '$.similarity_score') > 0.3 " +
                    "ORDER BY similarity_score DESC " +
                    "LIMIT ?";
        
        List<Long> similarUserIds = new ArrayList<>();
        
        try (Connection connection = getConnection();
            PreparedStatement ps = connection.prepareStatement(sql)) {
            
            ps.setLong(1, userId);
            ps.setLong(2, userId);
            ps.setLong(3, userId);
            ps.setInt(4, limit);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    similarUserIds.add(rs.getLong("similar_user_id"));
                }
            }
        }
        
        return similarUserIds;
    }

    /**
     * 从相似用户获取房源推荐
     */
    private List<Map<String, Object>> getPropertiesFromSimilarUsers(
            Long currentUserId, List<Long> similarUserIds, 
            int limit, boolean excludeViewed) throws SQLException {
        
        if (similarUserIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 构建占位符
        String placeholders = String.join(",", Collections.nCopies(similarUserIds.size(), "?"));
        
        // 构建查询：获取相似用户看过或收藏的房源
        String sql = "SELECT " +
                    "p.property_id, p.title, p.price_info, p.layout_info, " +
                    "p.basic_info, c.community_name, c.location_info, " +
                    "COUNT(DISTINCT bh.user_id) as view_count, " +
                    "COUNT(DISTINCT f.user_id) as favorite_count, " +
                    "GROUP_CONCAT(DISTINCT bh.user_id) as viewer_ids " +
                    "FROM browsing_history bh " +
                    "JOIN properties p ON bh.property_id = p.property_id " +
                    "LEFT JOIN communities c ON p.community_id = c.community_id " +
                    "LEFT JOIN favorites f ON p.property_id = f.property_id AND f.user_id IN (" + placeholders + ") " +
                    "WHERE bh.user_id IN (" + placeholders + ") " +
                    "AND p.status = 'for_sale' ";
        
        if (excludeViewed) {
            sql += "AND NOT EXISTS (SELECT 1 FROM browsing_history bh2 " +
                "WHERE bh2.user_id = ? AND bh2.property_id = p.property_id) ";
        }
        
        sql += "GROUP BY p.property_id " +
            "ORDER BY view_count DESC, favorite_count DESC " +
            "LIMIT ?";
        
        List<Map<String, Object>> recommendations = new ArrayList<>();
        
        try (Connection connection = getConnection();
            PreparedStatement ps = connection.prepareStatement(sql)) {
            
            int paramIndex = 1;
            
            // 设置收藏查询的用户ID
            for (Long uid : similarUserIds) {
                ps.setLong(paramIndex++, uid);
            }
            
            // 设置浏览历史查询的用户ID
            for (Long uid : similarUserIds) {
                ps.setLong(paramIndex++, uid);
            }
            
            // 排除当前用户已看过的
            if (excludeViewed) {
                ps.setLong(paramIndex++, currentUserId);
            }
            
            ps.setInt(paramIndex, limit * 3); // 多取一些用于筛选
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next() && recommendations.size() < limit) {
                    Map<String, Object> recommendation = buildOtherUsersRecommendationItem(rs, similarUserIds);
                    recommendations.add(recommendation);
                }
            }
        }
        
        return recommendations;
    }

    /**
     * 构建其他用户推荐的房源项
     */
    private Map<String, Object> buildOtherUsersRecommendationItem(ResultSet rs, List<Long> similarUserIds) throws SQLException {
        Map<String, Object> item = new HashMap<>();
        Long propertyId = rs.getLong("property_id");
        
        item.put("propertyId", propertyId);
        item.put("title", rs.getString("title"));
        
        // 构建summary
        String communityName = rs.getString("community_name");
        Double area = extractAreaFromJson(rs.getString("layout_info"));
        Integer bedroomCount = extractBedroomCountFromJson(rs.getString("layout_info"));
        Integer livingRoomCount = extractLivingRoomCountFromJson(rs.getString("layout_info"));
        
        String summary = String.format("%s · %.0f㎡ · %d室%d厅", 
            communityName != null ? communityName : "未知小区",
            area != null ? area : 0,
            bedroomCount != null ? bedroomCount : 0,
            livingRoomCount != null ? livingRoomCount : 0);
        item.put("summary", summary);
        
        // 价格（转换为万元）
        Double totalPrice = extractTotalPriceFromJson(rs.getString("price_info"));
        item.put("totalPrice", totalPrice != null ? totalPrice / 10000.0 : 0);
        
        // 图片和链接
        item.put("cover", "https://picsum.photos/seed/" + propertyId + "/300/200");
        item.put("detailUrl", "/property/" + propertyId);
        
        // 标签
        List<String> tags = new ArrayList<>();
        int viewCount = rs.getInt("view_count");
        int favoriteCount = rs.getInt("favorite_count");
        
        tags.add("相似用户推荐");
        if (viewCount > 5) tags.add("多人浏览");
        if (favoriteCount > 2) tags.add("多人收藏");
        if (viewCount > 10) tags.add("热门房源");
        item.put("tags", tags);
        
        // 计算推荐理由
        String reason = calculateRecommendationReason(viewCount, favoriteCount, similarUserIds.size());
        item.put("reason", reason);
        
        // 统计信息
        Map<String, Object> stats = new HashMap<>();
        stats.put("viewCount", viewCount);
        stats.put("favoriteCount", favoriteCount);
        stats.put("similarUserCount", Math.min(viewCount, similarUserIds.size()));
        item.put("stats", stats);
        
        return item;
    }

    /**
     * 计算推荐理由
     */
    private String calculateRecommendationReason(int viewCount, int favoriteCount, int similarUserCount) {
        List<String> reasons = new ArrayList<>();
        
        if (viewCount >= 3) {
            reasons.add(viewCount + "位相似用户浏览过");
        }
        
        if (favoriteCount >= 2) {
            reasons.add(favoriteCount + "位相似用户收藏过");
        }
        
        if (reasons.isEmpty()) {
            return "根据相似用户行为推荐";
        }
        
        return String.join("，", reasons);
    }

    /**
     * 获取热门房源（后备方案）
     */
    private List<Map<String, Object>> getPopularProperties(
            Long userId, int limit, boolean excludeViewed) throws SQLException {
        
        String sql = "SELECT " +
                    "p.property_id, p.title, p.price_info, p.layout_info, " +
                    "p.basic_info, c.community_name, c.location_info, " +
                    "COUNT(DISTINCT bh.user_id) as view_count " +
                    "FROM properties p " +
                    "LEFT JOIN communities c ON p.community_id = c.community_id " +
                    "LEFT JOIN browsing_history bh ON p.property_id = bh.property_id " +
                    "AND bh.created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
                    "WHERE p.status = 'for_sale' ";
        
        if (excludeViewed) {
            sql += "AND NOT EXISTS (SELECT 1 FROM browsing_history bh2 " +
                "WHERE bh2.user_id = ? AND bh2.property_id = p.property_id) ";
        }
        
        sql += "GROUP BY p.property_id " +
            "ORDER BY view_count DESC " +
            "LIMIT ?";
        
        List<Map<String, Object>> properties = new ArrayList<>();
        
        try (Connection connection = getConnection();
            PreparedStatement ps = connection.prepareStatement(sql)) {
            
            int paramIndex = 1;
            if (excludeViewed) {
                ps.setLong(paramIndex++, userId);
            }
            ps.setInt(paramIndex, limit);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    Long propertyId = rs.getLong("property_id");
                    
                    item.put("propertyId", propertyId);
                    item.put("title", rs.getString("title"));
                    
                    // 构建summary
                    String communityName = rs.getString("community_name");
                    Double area = extractAreaFromJson(rs.getString("layout_info"));
                    Integer bedroomCount = extractBedroomCountFromJson(rs.getString("layout_info"));
                    Integer livingRoomCount = extractLivingRoomCountFromJson(rs.getString("layout_info"));
                    
                    String summary = String.format("%s · %.0f㎡ · %d室%d厅", 
                        communityName != null ? communityName : "未知小区",
                        area != null ? area : 0,
                        bedroomCount != null ? bedroomCount : 0,
                        livingRoomCount != null ? livingRoomCount : 0);
                    item.put("summary", summary);
                    
                    // 价格（转换为万元）
                    Double totalPrice = extractTotalPriceFromJson(rs.getString("price_info"));
                    item.put("totalPrice", totalPrice != null ? totalPrice / 10000.0 : 0);
                    
                    // 图片和链接
                    item.put("cover", "https://picsum.photos/seed/" + propertyId + "/300/200");
                    item.put("detailUrl", "/property/" + propertyId);
                    
                    // 标签
                    List<String> tags = new ArrayList<>();
                    tags.add("热门房源");
                    if (rs.getInt("view_count") > 10) tags.add("近期热门");
                    item.put("tags", tags);
                    
                    // 推荐理由
                    item.put("reason", "近期" + rs.getInt("view_count") + "人浏览过");
                    
                    properties.add(item);
                }
            }
        }
        
        return properties;
    }

    // 添加辅助方法：解析JSON为Map（如果还没有的话）
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonToMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            Object result = objectMapper.readValue(json, Object.class);
            if (result instanceof Map) {
                return (Map<String, Object>) result;
            }
            return new HashMap<>();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    // 修改现有的提取方法，使用新的parseJsonToMap
    private Double extractTotalPriceFromJson(String json) {
        try {
            Map<String, Object> priceInfo = parseJsonToMap(json);
            if (priceInfo.containsKey("total_price")) {
                Object price = priceInfo.get("total_price");
                if (price instanceof Number) return ((Number) price).doubleValue();
                if (price instanceof String) return Double.parseDouble((String) price);
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return null;
    }

    private Double extractAreaFromJson(String json) {
        try {
            Map<String, Object> layoutInfo = parseJsonToMap(json);
            if (layoutInfo.containsKey("area")) {
                Object area = layoutInfo.get("area");
                if (area instanceof Number) return ((Number) area).doubleValue();
                if (area instanceof String) return Double.parseDouble((String) area);
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return null;
    }

    private Integer extractBedroomCountFromJson(String json) {
        try {
            Map<String, Object> layoutInfo = parseJsonToMap(json);
            if (layoutInfo.containsKey("bedroom_count")) {
                Object count = layoutInfo.get("bedroom_count");
                if (count instanceof Number) return ((Number) count).intValue();
                if (count instanceof String) return Integer.parseInt((String) count);
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return null;
    }

    private Integer extractLivingRoomCountFromJson(String json) {
        try {
            Map<String, Object> layoutInfo = parseJsonToMap(json);
            if (layoutInfo.containsKey("living_room_count")) {
                Object count = layoutInfo.get("living_room_count");
                if (count instanceof Number) return ((Number) count).intValue();
                if (count instanceof String) return Integer.parseInt((String) count);
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return null;
    }





    /**
     * 获取相似房源推荐
     */
    @GetMapping("/recommend/similar")
    public ResponseEntity<Map<String, Object>> getSimilarProperties(
            @RequestParam("propertyId") Long propertyId,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "limit", defaultValue = "5") Integer limit) {
        
        if (propertyId == null) {
            return error(HttpStatus.BAD_REQUEST, "propertyId不能为空");
        }
        
        try {
            // 1. 从相似度表获取已计算的推荐
            List<Map<String, Object>> recommendations = 
                getSimilarPropertiesFromSimilarityTable(propertyId, userId, limit);
            
            // 2. 如果不足，综合计算补足
            if (recommendations.size() < limit) {
                int remaining = limit - recommendations.size();
                List<Map<String, Object>> realtimeRecommendations = 
                    getRealtimeSimilarProperties(propertyId, userId, remaining, recommendations);
                recommendations.addAll(realtimeRecommendations);
            }
            
            Map<String, Object> body = new HashMap<String, Object>();
            body.put("propertyId", propertyId);
            body.put("recommendations", recommendations);
            body.put("count", recommendations.size());
            body.put("message", "推荐获取成功");
            
            return ResponseEntity.ok(body);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("message", "获取推荐失败");
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 从相似度表获取已计算的推荐
     */
    private List<Map<String, Object>> getSimilarPropertiesFromSimilarityTable(
            Long propertyId, Long userId, int limit) throws SQLException {
        
        List<Map<String, Object>> recommendations = new ArrayList<>();
        
        // 查询相似度表
        String sql = "SELECT " +
                    "CASE WHEN property_id1 = ? THEN property_id2 ELSE property_id1 END as similar_id, " +
                    "similarity_data " +
                    "FROM property_similarity " +
                    "WHERE (property_id1 = ? OR property_id2 = ?) " +
                    "ORDER BY JSON_EXTRACT(similarity_data, '$.similarity_score') DESC " +
                    "LIMIT ?";
        
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            
            ps.setLong(1, propertyId);
            ps.setLong(2, propertyId);
            ps.setLong(3, propertyId);
            ps.setInt(4, limit);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Long similarId = rs.getLong("similar_id");
                    
                    // 获取房源详情
                    Map<String, Object> propertyDetail = getPropertyDetail(similarId);
                    if (propertyDetail != null) {
                        // 添加相似度信息
                        String similarityData = rs.getString("similarity_data");
                        propertyDetail.put("similarity", parseJson(similarityData));
                        propertyDetail.put("source", "pre_calculated");
                        
                        recommendations.add(propertyDetail);
                    }
                }
            }
        }
        
        return recommendations;
    }
    
    /**
     * 实时计算相似房源（综合位置、价格、房型）
     */
    private List<Map<String, Object>> getRealtimeSimilarProperties(
            Long sourceId, Long userId, int needCount,
            List<Map<String, Object>> existingRecs) throws SQLException {
        
        // 1. 获取源房源信息
        Map<String, Object> sourceProperty = getPropertyDetailWithFeatures(sourceId);
        if (sourceProperty == null) {
            return new ArrayList<>();
        }
        
        // 2. 获取候选房源（排除已推荐的）
        Set<Long> excludedIds = new HashSet<>();
        excludedIds.add(sourceId);
        for (Map<String, Object> rec : existingRecs) {
            excludedIds.add((Long) rec.get("propertyId"));
        }
        
        // 获取候选房源（限制数量避免性能问题）
        List<Map<String, Object>> candidates = getCandidateProperties(sourceId, excludedIds, needCount * 5);
        
        // 3. 计算综合相似度并排序
        candidates.forEach(candidate -> {
            double similarity = calculateComprehensiveSimilarity(sourceProperty, candidate);
            candidate.put("similarityScore", similarity);
        });
        
        // 排序并取前N个
        return candidates.stream()
                .sorted((a, b) -> Double.compare(
                    (Double) b.get("similarityScore"),
                    (Double) a.get("similarityScore")
                ))
                .filter(c -> (Double) c.get("similarityScore") > 0.2) // 过滤低相似度
                .limit(needCount)
                .peek(c -> {
                    c.put("source", "realtime_calculated");
                    c.put("similarity", Map.of(
                        "score", c.get("similarityScore"),
                        "algorithm", "comprehensive",
                        "factors", Map.of(
                            "location", 0.35,
                            "price", 0.30,
                            "layout", 0.25,
                            "area", 0.10
                        )
                    ));
                    c.remove("similarityScore");
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 计算综合相似度
     */
    private double calculateComprehensiveSimilarity(
            Map<String, Object> source, Map<String, Object> target) {
        
        double totalSimilarity = 0.0;
        
        // 1. 位置相似度 (权重0.35)
        double locationSimilarity = calculateLocationSimilarity(source, target);
        totalSimilarity += locationSimilarity * 0.35;
        
        // 2. 价格相似度 (权重0.30)
        double priceSimilarity = calculatePriceSimilarity(source, target);
        totalSimilarity += priceSimilarity * 0.30;
        
        // 3. 房型相似度 (权重0.25)
        double layoutSimilarity = calculateLayoutSimilarity(source, target);
        totalSimilarity += layoutSimilarity * 0.25;
        
        // 4. 面积相似度 (权重0.10)
        double areaSimilarity = calculateAreaSimilarity(source, target);
        totalSimilarity += areaSimilarity * 0.10;
        
        return totalSimilarity;
    }
    
    /**
     * 计算位置相似度
     */
    private double calculateLocationSimilarity(Map<String, Object> source, Map<String, Object> target) {
        try {
            // 获取行政区
            String sourceDistrict = extractDistrict(source);
            String targetDistrict = extractDistrict(target);
            
            if (sourceDistrict != null && sourceDistrict.equals(targetDistrict)) {
                return 0.9; // 同一行政区
            }
            
            // 获取经纬度（如果可用）
            Double sourceLng = extractLongitude(source);
            Double sourceLat = extractLatitude(source);
            Double targetLng = extractLongitude(target);
            Double targetLat = extractLatitude(target);
            
            if (sourceLng != null && sourceLat != null && 
                targetLng != null && targetLat != null) {
                // 计算距离相似度（距离越近，相似度越高）
                double distance = calculateDistance(
                    sourceLat, sourceLng, targetLat, targetLng
                );
                
                // 距离小于5公里，相似度较高
                if (distance < 5) return 0.8;
                if (distance < 10) return 0.6;
                if (distance < 20) return 0.4;
            }
            
            // 小区名称相似度
            String sourceCommunity = (String) source.get("communityName");
            String targetCommunity = (String) target.get("communityName");
            
            if (sourceCommunity != null && targetCommunity != null &&
                sourceCommunity.contains(targetCommunity.substring(0, Math.min(3, targetCommunity.length())))) {
                return 0.7; // 相似小区名称
            }
            
        } catch (Exception e) {
            // 忽略异常
        }
        
        return 0.3; // 默认相似度
    }
    
    /**
     * 计算价格相似度
     */
    private double calculatePriceSimilarity(Map<String, Object> source, Map<String, Object> target) {
        try {
            Double sourcePrice = extractTotalPrice(source);
            Double targetPrice = extractTotalPrice(target);
            
            if (sourcePrice == null || targetPrice == null || sourcePrice == 0) {
                return 0.3;
            }
            
            double priceRatio = Math.min(sourcePrice, targetPrice) / 
                               Math.max(sourcePrice, targetPrice);
            
            // 价格比例越高，相似度越高
            if (priceRatio > 0.9) return 0.9; // 价格相差<10%
            if (priceRatio > 0.8) return 0.7; // 价格相差<20%
            if (priceRatio > 0.6) return 0.5; // 价格相差<40%
            if (priceRatio > 0.4) return 0.3; // 价格相差<60%
            
        } catch (Exception e) {
            // 忽略异常
        }
        
        return 0.2; // 默认相似度
    }
    
    /**
     * 计算房型相似度
     */
    private double calculateLayoutSimilarity(Map<String, Object> source, Map<String, Object> target) {
        try {
            Integer sourceBedroom = extractBedroomCount(source);
            Integer targetBedroom = extractBedroomCount(target);
            Integer sourceLivingRoom = extractLivingRoomCount(source);
            Integer targetLivingRoom = extractLivingRoomCount(target);
            
            if (sourceBedroom != null && targetBedroom != null) {
                // 卧室数量完全相同
                if (sourceBedroom.equals(targetBedroom)) {
                    double similarity = 0.8;
                    
                    // 客厅数量也相同，相似度更高
                    if (sourceLivingRoom != null && targetLivingRoom != null && 
                        sourceLivingRoom.equals(targetLivingRoom)) {
                        similarity = 0.9;
                    }
                    
                    return similarity;
                }
                
                // 卧室数量相差1个
                if (Math.abs(sourceBedroom - targetBedroom) == 1) {
                    return 0.6;
                }
                
                // 卧室数量相差2个
                if (Math.abs(sourceBedroom - targetBedroom) == 2) {
                    return 0.4;
                }
            }
            
        } catch (Exception e) {
            // 忽略异常
        }
        
        return 0.3; // 默认相似度
    }
    
    /**
     * 计算面积相似度
     */
    private double calculateAreaSimilarity(Map<String, Object> source, Map<String, Object> target) {
        try {
            Double sourceArea = extractArea(source);
            Double targetArea = extractArea(target);
            
            if (sourceArea == null || targetArea == null || sourceArea == 0) {
                return 0.3;
            }
            
            double areaRatio = Math.min(sourceArea, targetArea) / 
                              Math.max(sourceArea, targetArea);
            
            if (areaRatio > 0.9) return 0.9; // 面积相差<10%
            if (areaRatio > 0.8) return 0.7; // 面积相差<20%
            if (areaRatio > 0.6) return 0.5; // 面积相差<40%
            
        } catch (Exception e) {
            // 忽略异常
        }
        
        return 0.3; // 默认相似度
    }
    
    /**
     * 获取候选房源
     */
    private List<Map<String, Object>> getCandidateProperties(
            Long excludeId, Set<Long> excludedIds, int limit) throws SQLException {
        
        String placeholders = excludedIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));
        
        String sql = "SELECT p.property_id, p.title, p.price_info, p.layout_info, " +
                    "p.basic_info, c.community_name, c.location_info " +
                    "FROM properties p " +
                    "LEFT JOIN communities c ON p.community_id = c.community_id " +
                    "WHERE p.status = 'for_sale' " +
                    "AND p.property_id NOT IN (" + placeholders + ") " +
                    "ORDER BY p.created_at DESC " +
                    "LIMIT ?";
        
        List<Map<String, Object>> candidates = new ArrayList<>();
        
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            
            int paramIndex = 1;
            for (Long id : excludedIds) {
                ps.setLong(paramIndex++, id);
            }
            ps.setInt(paramIndex, limit);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> property = new HashMap<>();
                    property.put("propertyId", rs.getLong("property_id"));
                    property.put("title", rs.getString("title"));
                    property.put("priceInfo", parseJson(rs.getString("price_info")));
                    property.put("layoutInfo", parseJson(rs.getString("layout_info")));
                    property.put("basicInfo", parseJson(rs.getString("basic_info")));
                    property.put("communityName", rs.getString("community_name"));
                    property.put("locationInfo", parseJson(rs.getString("location_info")));
                    
                    candidates.add(property);
                }
            }
        }
        
        return candidates;
    }
    
    /**
     * 获取房源详情
     */
    private Map<String, Object> getPropertyDetail(Long propertyId) throws SQLException {
        String sql = "SELECT p.property_id, p.title, p.price_info, p.layout_info, " +
                    "p.basic_info, c.community_name, c.location_info " +
                    "FROM properties p " +
                    "LEFT JOIN communities c ON p.community_id = c.community_id " +
                    "WHERE p.property_id = ? AND p.status = 'for_sale'";
        
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            
            ps.setLong(1, propertyId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> property = new HashMap<>();
                    property.put("propertyId", rs.getLong("property_id"));
                    property.put("title", rs.getString("title"));
                    property.put("priceInfo", parseJson(rs.getString("price_info")));
                    property.put("layoutInfo", parseJson(rs.getString("layout_info")));
                    property.put("basicInfo", parseJson(rs.getString("basic_info")));
                    property.put("communityName", rs.getString("community_name"));
                    property.put("locationInfo", parseJson(rs.getString("location_info")));
                    
                    return property;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 获取房源详情（包含特征提取）
     */
    private Map<String, Object> getPropertyDetailWithFeatures(Long propertyId) throws SQLException {
        Map<String, Object> detail = getPropertyDetail(propertyId);
        if (detail != null) {
            // 提取关键特征
            detail.put("district", extractDistrict(detail));
            detail.put("longitude", extractLongitude(detail));
            detail.put("latitude", extractLatitude(detail));
            detail.put("totalPrice", extractTotalPrice(detail));
            detail.put("bedroomCount", extractBedroomCount(detail));
            detail.put("livingRoomCount", extractLivingRoomCount(detail));
            detail.put("area", extractArea(detail));
        }
        return detail;
    }
    
    // 辅助方法：提取特征值
    private String extractDistrict(Map<String, Object> property) {
        try {
            Map<String, Object> locationInfo = (Map<String, Object>) property.get("locationInfo");
            if (locationInfo != null && locationInfo.containsKey("district")) {
                return locationInfo.get("district").toString();
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return null;
    }
    
    private Double extractLongitude(Map<String, Object> property) {
        try {
            Map<String, Object> locationInfo = (Map<String, Object>) property.get("locationInfo");
            if (locationInfo != null && locationInfo.containsKey("longitude")) {
                Object lng = locationInfo.get("longitude");
                if (lng instanceof Number) return ((Number) lng).doubleValue();
                if (lng instanceof String) return Double.parseDouble((String) lng);
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return null;
    }
    
    private Double extractLatitude(Map<String, Object> property) {
        try {
            Map<String, Object> locationInfo = (Map<String, Object>) property.get("locationInfo");
            if (locationInfo != null && locationInfo.containsKey("latitude")) {
                Object lat = locationInfo.get("latitude");
                if (lat instanceof Number) return ((Number) lat).doubleValue();
                if (lat instanceof String) return Double.parseDouble((String) lat);
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return null;
    }
    
    private Double extractTotalPrice(Map<String, Object> property) {
        try {
            Map<String, Object> priceInfo = (Map<String, Object>) property.get("priceInfo");
            if (priceInfo != null && priceInfo.containsKey("total_price")) {
                Object price = priceInfo.get("total_price");
                if (price instanceof Number) return ((Number) price).doubleValue();
                if (price instanceof String) return Double.parseDouble((String) price);
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return null;
    }
    
    private Integer extractBedroomCount(Map<String, Object> property) {
        try {
            Map<String, Object> layoutInfo = (Map<String, Object>) property.get("layoutInfo");
            if (layoutInfo != null && layoutInfo.containsKey("bedroom_count")) {
                Object count = layoutInfo.get("bedroom_count");
                if (count instanceof Number) return ((Number) count).intValue();
                if (count instanceof String) return Integer.parseInt((String) count);
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return null;
    }
    
    private Integer extractLivingRoomCount(Map<String, Object> property) {
        try {
            Map<String, Object> layoutInfo = (Map<String, Object>) property.get("layoutInfo");
            if (layoutInfo != null && layoutInfo.containsKey("living_room_count")) {
                Object count = layoutInfo.get("living_room_count");
                if (count instanceof Number) return ((Number) count).intValue();
                if (count instanceof String) return Integer.parseInt((String) count);
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return null;
    }
    
    private Double extractArea(Map<String, Object> property) {
        try {
            Map<String, Object> layoutInfo = (Map<String, Object>) property.get("layoutInfo");
            if (layoutInfo != null && layoutInfo.containsKey("area")) {
                Object area = layoutInfo.get("area");
                if (area instanceof Number) return ((Number) area).doubleValue();
                if (area instanceof String) return Double.parseDouble((String) area);
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return null;
    }
    
    /**
     * 计算两点间距离（公里）- Haversine公式
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371.0; // 地球半径（公里）
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLng / 2) * Math.sin(dLng / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return earthRadius * c;
    }
    
    // 原有的 error 和 serverError 方法
    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
    
    private ResponseEntity<Map<String, Object>> serverError(String message, Exception e) {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("message", message);
        body.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }


    
    /**
     * 去往查询界面的入口接口，复用猜你喜欢数据，方便前端在跳转前展示推荐内容。
     */
    @GetMapping("/go-query")
    public ResponseEntity<Map<String, Object>> goQueryPage() {
        return ResponseEntity.ok(buildRecommendationResponse("查询页推荐数据（示例）"));
    }

    private Map<String, Object> buildRecommendationResponse(String message) {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("items", buildGuessItems());
        body.put("message", message);
        return body;
    }

    private List<Map<String, Object>> buildGuessItems() {
        return Arrays.asList(
                buildGuessItem(101L, "万科城市花园 精装三房 南向采光好", "南山区 · 89.5㎡ · 3室2厅2卫", 650.5, "https://example.com/property/101"),
                buildGuessItem(102L, "华润城 四房大户型 学区房", "福田区 · 128㎡ · 4室2厅3卫", 980.0, "https://example.com/property/102"),
                buildGuessItem(103L, "科技园 地铁口复式 Loft", "南山区 · 68㎡ · 2室1厅1卫", 520.0, "https://example.com/property/103")
        );
    }

    private Map<String, Object> buildGuessItem(Long propertyId, String title, String summary, Double price, String url) {
        Map<String, Object> item = new HashMap<String, Object>();
        item.put("propertyId", propertyId);
        item.put("title", title);
        item.put("summary", summary);
        item.put("totalPrice", price);
        item.put("cover", "https://picsum.photos/seed/" + propertyId + "/300/200");
        item.put("detailUrl", url);
        item.put("tags", Arrays.asList("近地铁", "学区房", "南北通透"));
        return item;
    }

    private Map<String, Object> getDefaultStats() {
        Map<String, Object> stats = new HashMap<String, Object>();
        stats.put("favorites", 12);
        stats.put("browsed", 48);
        stats.put("recommendations", 6);
        return stats;
    }

    private Object parseJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return json;
        }
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
