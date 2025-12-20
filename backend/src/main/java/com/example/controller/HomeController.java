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
     * 猜你喜欢接口：返回前端展示的房源推荐（从数据库获取真实房源）。
     */
    @GetMapping("/guess-you-like")
    public ResponseEntity<Map<String, Object>> guessYouLike(
            @RequestParam(value = "userId", required = false) Long userId) {
        try {
            List<Map<String, Object>> recommendedProperties = new ArrayList<>();
            Set<Long> propertyIds = new HashSet<>(); // 用于去重
            
            if (userId != null) {
                // 1. 获取相似度最高的3个用户的全部收藏房源
                List<Map<String, Object>> similarUsersFavorites = getSimilarUsersFavorites(userId, 3);
                for (Map<String, Object> favorite : similarUsersFavorites) {
                    Long propertyId = ((Number) favorite.get("propertyId")).longValue();
                    if (!propertyIds.contains(propertyId)) {
                        propertyIds.add(propertyId);
                        recommendedProperties.add(favorite);
                    }
                }
                
                // 2. 获取用户自己收藏的房源
                List<Long> userFavoritePropertyIds = getUserFavoritePropertyIds(userId);
                
                // 3. 获取与用户收藏房源相似度最高的前5个房源（每个收藏房源取前5个相似房源）
                for (Long favoritePropertyId : userFavoritePropertyIds) {
                    List<Map<String, Object>> similarProperties = getSimilarPropertiesForRecommendation(favoritePropertyId, 5);
                    for (Map<String, Object> similar : similarProperties) {
                        Long propertyId = ((Number) similar.get("propertyId")).longValue();
                        // 排除用户已收藏的房源
                        if (!userFavoritePropertyIds.contains(propertyId) && !propertyIds.contains(propertyId)) {
                            propertyIds.add(propertyId);
                            recommendedProperties.add(similar);
                        }
                    }
                }
            }
            
            // 如果推荐结果不足或没有userId，补充热门房源
            if (recommendedProperties.size() < 10) {
                List<Map<String, Object>> popularProperties = getPopularProperties(10);
                for (Map<String, Object> popular : popularProperties) {
                    Long propertyId = ((Number) popular.get("propertyId")).longValue();
                    if (!propertyIds.contains(propertyId)) {
                        propertyIds.add(propertyId);
                        recommendedProperties.add(popular);
                        if (recommendedProperties.size() >= 10) {
                            break;
                        }
                    }
                }
            }
            
            // 限制返回数量
            if (recommendedProperties.size() > 10) {
                recommendedProperties = recommendedProperties.subList(0, 10);
            }

            Map<String, Object> body = new HashMap<String, Object>();
            body.put("items", recommendedProperties);
            body.put("message", "Guess you like data retrieved successfully");
            body.put("count", recommendedProperties.size());
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            e.printStackTrace();
            // 失败时返回热门房源
            try {
                List<Map<String, Object>> properties = getPopularProperties(10);
                Map<String, Object> body = new HashMap<String, Object>();
                body.put("items", properties);
                body.put("message", "Guess you like data (fallback to popular properties)");
                return ResponseEntity.ok(body);
            } catch (Exception ex) {
                return ResponseEntity.ok(buildRecommendationResponse("Guess you like data (example)"));
            }
        }
    }
    
    /**
     * 获取相似度最高的N个用户的全部收藏房源
     */
    private List<Map<String, Object>> getSimilarUsersFavorites(Long userId, int topN) throws SQLException {
        List<Map<String, Object>> properties = new ArrayList<>();
        
        // 1. 获取相似度最高的N个用户
        String similarUsersSql = "SELECT " +
                "CASE WHEN user_id1 = ? THEN user_id2 ELSE user_id1 END as similar_user_id, " +
                "CAST(JSON_EXTRACT(similarity_data, '$.similarity_score') AS DECIMAL(8,4)) as similarity_score " +
                "FROM user_similarity " +
                "WHERE user_id1 = ? OR user_id2 = ? " +
                "ORDER BY similarity_score DESC " +
                "LIMIT ?";
        
        List<Long> similarUserIds = new ArrayList<>();
        try (Connection connection = getConnection();
                PreparedStatement ps = connection.prepareStatement(similarUsersSql)) {
            ps.setLong(1, userId);
            ps.setLong(2, userId);
            ps.setLong(3, userId);
            ps.setInt(4, topN);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    similarUserIds.add(rs.getLong("similar_user_id"));
                }
            }
        }
        
        if (similarUserIds.isEmpty()) {
            return properties;
        }
        
        // 2. 获取这些用户的全部收藏房源
        String placeholders = similarUserIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String favoritesSql = "SELECT DISTINCT f.property_id, p.title, p.price_info, p.layout_info, " +
                "c.name as community_name, c.location_info, p.view_count " +
                "FROM favorites f " +
                "INNER JOIN properties p ON f.property_id = p.property_id " +
                "LEFT JOIN communities c ON p.community_id = c.community_id " +
                "WHERE f.user_id IN (" + placeholders + ") " +
                "AND p.status = 'for_sale'";
        
        try (Connection connection = getConnection();
                PreparedStatement ps = connection.prepareStatement(favoritesSql)) {
            for (int i = 0; i < similarUserIds.size(); i++) {
                ps.setLong(i + 1, similarUserIds.get(i));
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> property = buildPropertyCard(rs);
                    properties.add(property);
                }
            }
        }
        
        return properties;
    }
    
    /**
     * 获取用户收藏的房源ID列表
     */
    private List<Long> getUserFavoritePropertyIds(Long userId) throws SQLException {
        List<Long> propertyIds = new ArrayList<>();
        String sql = "SELECT property_id FROM favorites WHERE user_id = ?";
        
        try (Connection connection = getConnection();
                PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    propertyIds.add(rs.getLong("property_id"));
                }
            }
        }
        
        return propertyIds;
    }
    
    /**
     * 获取与指定房源相似度最高的前N个房源（用于推荐）
     */
    private List<Map<String, Object>> getSimilarPropertiesForRecommendation(Long propertyId, int limit) throws SQLException {
        List<Map<String, Object>> properties = new ArrayList<>();
        
        String sql = "SELECT " +
                "CASE WHEN ps.property_id1 = ? THEN ps.property_id2 ELSE ps.property_id1 END as similar_property_id, " +
                "CAST(JSON_EXTRACT(ps.similarity_data, '$.similarity_score') AS DECIMAL(8,4)) as similarity_score, " +
                "p.title, p.price_info, p.layout_info, " +
                "c.name as community_name, c.location_info, p.view_count " +
                "FROM property_similarity ps " +
                "INNER JOIN properties p ON (CASE WHEN ps.property_id1 = ? THEN ps.property_id2 ELSE ps.property_id1 END) = p.property_id " +
                "LEFT JOIN communities c ON p.community_id = c.community_id " +
                "WHERE (ps.property_id1 = ? OR ps.property_id2 = ?) " +
                "AND p.status = 'for_sale' " +
                "ORDER BY similarity_score DESC " +
                "LIMIT ?";
        
        try (Connection connection = getConnection();
                PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, propertyId);
            ps.setLong(2, propertyId);
            ps.setLong(3, propertyId);
            ps.setLong(4, propertyId);
            ps.setInt(5, limit);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> property = buildPropertyCard(rs);
                    property.put("propertyId", rs.getLong("similar_property_id"));
                    properties.add(property);
                }
            }
        }
        
        return properties;
    }
    
    /**
     * 构建房源卡片数据（用于推荐列表）
     */
    private Map<String, Object> buildPropertyCard(ResultSet rs) throws SQLException {
        Map<String, Object> property = new HashMap<>();
        // 尝试获取property_id，如果没有则尝试similar_property_id
        try {
            property.put("propertyId", rs.getLong("property_id"));
        } catch (SQLException e) {
            try {
                property.put("propertyId", rs.getLong("similar_property_id"));
            } catch (SQLException ex) {
                // 如果都没有，使用默认值
                property.put("propertyId", 0L);
            }
        }
        property.put("title", rs.getString("title"));
        
        // 解析价格信息
        String priceInfoJson = rs.getString("price_info");
        Map<String, Object> priceInfo = parseJsonToMap(priceInfoJson);
        Double totalPrice = priceInfo != null
                ? ((Number) priceInfo.getOrDefault("total_price", 0)).doubleValue()
                : 0.0;
        
        // 解析户型信息
        String layoutInfoJson = rs.getString("layout_info");
        Map<String, Object> layoutInfo = parseJsonToMap(layoutInfoJson);
        
        // 构建summary
        String communityName = rs.getString("community_name");
        Double area = extractDouble(layoutInfo, "area");
        Integer bedroomCount = extractInt(layoutInfo, "bedroom_count");
        Integer livingRoomCount = extractInt(layoutInfo, "living_room_count");
        
        String summary = String.format("%s · %.0f㎡ · %d室%d厅",
                communityName != null ? communityName : "Unknown Community",
                area != null ? area : 0,
                bedroomCount != null ? bedroomCount : 0,
                livingRoomCount != null ? livingRoomCount : 0);
        property.put("summary", summary);
        
        property.put("totalPrice", totalPrice);
        property.put("cover", "https://picsum.photos/seed/" + property.get("propertyId") + "/300/200");
        property.put("detailUrl", "https://example.com/property/" + property.get("propertyId"));
        property.put("tags", Arrays.asList("Recommended", "High Similarity"));
        
        return property;
    }

    /**
     * 获取热门房源（浏览次数最多）
     */
    private List<Map<String, Object>> getPopularProperties(int limit) throws SQLException {
        List<Map<String, Object>> properties = new ArrayList<>();

        String sql = "SELECT p.property_id, p.title, p.price_info, p.layout_info, " +
                "c.name as community_name, c.location_info, p.view_count " +
                "FROM properties p " +
                "LEFT JOIN communities c ON p.community_id = c.community_id " +
                "WHERE p.status = 'for_sale' " +
                "ORDER BY p.view_count DESC " +
                "LIMIT ?";

        try (Connection connection = getConnection();
                PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> property = new HashMap<>();
                    property.put("propertyId", rs.getLong("property_id"));
                    property.put("title", rs.getString("title"));

                    // 解析价格信息
                    String priceInfoJson = rs.getString("price_info");
                    Map<String, Object> priceInfo = parseJsonToMap(priceInfoJson);
                    Double totalPrice = priceInfo != null
                            ? ((Number) priceInfo.getOrDefault("total_price", 0)).doubleValue()
                            : 0.0;

                    // 解析户型信息
                    String layoutInfoJson = rs.getString("layout_info");
                    Map<String, Object> layoutInfo = parseJsonToMap(layoutInfoJson);

                    // 构建summary
                    String communityName = rs.getString("community_name");
                    Double area = extractDouble(layoutInfo, "area");
                    Integer bedroomCount = extractInt(layoutInfo, "bedroom_count");
                    Integer livingRoomCount = extractInt(layoutInfo, "living_room_count");

                    String summary = String.format("%s · %.0f㎡ · %d室%d厅",
                            communityName != null ? communityName : "未知小区",
                            area != null ? area : 0,
                            bedroomCount != null ? bedroomCount : 0,
                            livingRoomCount != null ? livingRoomCount : 0);
                    property.put("summary", summary);

                    property.put("totalPrice", totalPrice);
                    property.put("cover", "https://picsum.photos/seed/" + rs.getLong("property_id") + "/300/200");
                    property.put("detailUrl", "https://example.com/property/" + rs.getLong("property_id"));
                    property.put("tags", Arrays.asList("热门房源", "高浏览量"));

                    properties.add(property);
                }
            }
        }

        return properties;
    }

    /**
     * 热门推荐接口：返回前100个浏览量最高的房源中随机10个
     */
    @GetMapping("/popular")
    public ResponseEntity<Map<String, Object>> getPopularRecommendations() {
        try (Connection connection = getConnection()) {
            // 1. 先获取前100个浏览量最高的房源
            String top100Sql = "SELECT " +
                    "p.property_id, " +
                    "p.title, " +
                    "p.price_info, " +
                    "p.layout_info, " +
                    "p.basic_info, " +
                    "p.view_count, " +
                    "p.favorite_count, " +
                    "c.name as community_name, " +
                    "c.location_info " +
                    "FROM properties p " +
                    "LEFT JOIN communities c ON p.community_id = c.community_id " +
                    "WHERE p.status = 'for_sale' " +
                    "ORDER BY p.view_count DESC, p.favorite_count DESC " +
                    "LIMIT 100";

            List<Map<String, Object>> top100Properties = new ArrayList<>();

            try (PreparedStatement ps = connection.prepareStatement(top100Sql);
                    ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Map<String, Object> property = new HashMap<>();
                    Long propertyId = rs.getLong("property_id");

                    property.put("propertyId", propertyId);
                    property.put("title", rs.getString("title"));
                    property.put("viewCount", rs.getInt("view_count"));
                    property.put("favoriteCount", rs.getInt("favorite_count"));

                    // 解析JSON字段
                    String priceInfoJson = rs.getString("price_info");
                    String layoutInfoJson = rs.getString("layout_info");
                    String basicInfoJson = rs.getString("basic_info");
                    String locationInfoJson = rs.getString("location_info");

                    // 价格信息
                    Object priceInfoObj = parseJson(priceInfoJson);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> priceInfo = (priceInfoObj instanceof Map) ? (Map<String, Object>) priceInfoObj
                            : new HashMap<>();
                    property.put("priceInfo", priceInfo);
                    Double totalPrice = extractDouble(priceInfo, "total_price");
                    property.put("totalPrice", totalPrice != null ? totalPrice : 0);

                    // 户型信息
                    Object layoutInfoObj = parseJson(layoutInfoJson);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> layoutInfo = (layoutInfoObj instanceof Map)
                            ? (Map<String, Object>) layoutInfoObj
                            : new HashMap<>();
                    property.put("layoutInfo", layoutInfo);
                    Double area = extractDouble(layoutInfo, "area");
                    Integer bedroomCount = extractInt(layoutInfo, "bedroom_count");
                    Integer livingRoomCount = extractInt(layoutInfo, "living_room_count");

                    // 构建summary
                    String communityName = rs.getString("community_name");
                    String summary = String.format("%s · %.0f㎡ · %d室%d厅",
                            communityName != null ? communityName : "未知小区",
                            area != null ? area : 0,
                            bedroomCount != null ? bedroomCount : 0,
                            livingRoomCount != null ? livingRoomCount : 0);
                    property.put("summary", summary);

                    // 基本信息
                    Object basicInfoObj = parseJson(basicInfoJson);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> basicInfo = (basicInfoObj instanceof Map) ? (Map<String, Object>) basicInfoObj
                            : new HashMap<>();
                    property.put("basicInfo", basicInfo);

                    // 位置信息
                    Object locationInfoObj = parseJson(locationInfoJson);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> locationInfo = (locationInfoObj instanceof Map)
                            ? (Map<String, Object>) locationInfoObj
                            : new HashMap<>();
                    property.put("locationInfo", locationInfo);

                    // 图片和链接
                    property.put("cover", "https://picsum.photos/seed/" + propertyId + "/300/200");
                    property.put("detailUrl", "/property/" + propertyId);

                    // 标签
                    List<String> tags = new ArrayList<>();
                    int viewCount = rs.getInt("view_count");
                    int favoriteCount = rs.getInt("favorite_count");
                    tags.add("热门房源");
                    if (viewCount > 50)
                        tags.add("超热门");
                    if (favoriteCount > 10)
                        tags.add("多人收藏");
                    property.put("tags", tags);

                    top100Properties.add(property);
                }
            }

            // 2. 从100个中随机选择10个
            Collections.shuffle(top100Properties);
            List<Map<String, Object>> random10 = top100Properties.stream()
                    .limit(10)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("items", random10);
            response.put("count", random10.size());
            response.put("totalCandidates", top100Properties.size());
            response.put("message", "热门推荐获取成功");

            return ResponseEntity.ok(response);

        } catch (SQLException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "获取热门推荐失败");
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 从Map中提取Double值
     */
    private Double extractDouble(Map<String, Object> map, String key) {
        if (map == null)
            return null;
        Object value = map.get(key);
        if (value == null)
            return null;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    /**
     * 从Map中提取Integer值
     */
    private Integer extractInt(Map<String, Object> map, String key) {
        if (map == null)
            return null;
        Object value = map.get(key);
        if (value == null)
            return null;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    /**
     * 解析JSON字符串为Map
     */
    private Map<String, Object> parseJsonToMap(String json) {
        Object result = parseJson(json);
        if (result instanceof Map) {
            return (Map<String, Object>) result;
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
            List<Map<String, Object>> recommendations = getSimilarPropertiesFromSimilarityTable(propertyId, userId,
                    limit);

            // 2. 如果不足，综合计算补足
            if (recommendations.size() < limit) {
                int remaining = limit - recommendations.size();
                List<Map<String, Object>> realtimeRecommendations = getRealtimeSimilarProperties(propertyId, userId,
                        remaining, recommendations);
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
                        (Double) a.get("similarityScore")))
                .filter(c -> (Double) c.get("similarityScore") > 0.2) // 过滤低相似度
                .limit(needCount)
                .peek(c -> {
                    c.put("source", "realtime_calculated");

                    // Java 8 兼容：替换 Map.of() 为 HashMap
                    Map<String, Object> factors = new HashMap<>();
                    factors.put("location", 0.35);
                    factors.put("price", 0.30);
                    factors.put("layout", 0.25);
                    factors.put("area", 0.10);

                    Map<String, Object> similarity = new HashMap<>();
                    similarity.put("score", c.get("similarityScore"));
                    similarity.put("algorithm", "comprehensive");
                    similarity.put("factors", factors);

                    c.put("similarity", similarity);

                    Map<String, Object> similarityMap = new HashMap<>();
                    similarityMap.put("score", c.get("similarityScore"));
                    similarityMap.put("algorithm", "comprehensive");
                    similarityMap.put("factors", factors);

                    c.put("similarity", similarityMap);
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
                        sourceLat, sourceLng, targetLat, targetLng);

                // 距离小于5公里，相似度较高
                if (distance < 5)
                    return 0.8;
                if (distance < 10)
                    return 0.6;
                if (distance < 20)
                    return 0.4;
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
            if (priceRatio > 0.9)
                return 0.9; // 价格相差<10%
            if (priceRatio > 0.8)
                return 0.7; // 价格相差<20%
            if (priceRatio > 0.6)
                return 0.5; // 价格相差<40%
            if (priceRatio > 0.4)
                return 0.3; // 价格相差<60%

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

            if (areaRatio > 0.9)
                return 0.9; // 面积相差<10%
            if (areaRatio > 0.8)
                return 0.7; // 面积相差<20%
            if (areaRatio > 0.6)
                return 0.5; // 面积相差<40%

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
                if (lng instanceof Number)
                    return ((Number) lng).doubleValue();
                if (lng instanceof String)
                    return Double.parseDouble((String) lng);
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
                if (lat instanceof Number)
                    return ((Number) lat).doubleValue();
                if (lat instanceof String)
                    return Double.parseDouble((String) lat);
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
                if (price instanceof Number)
                    return ((Number) price).doubleValue();
                if (price instanceof String)
                    return Double.parseDouble((String) price);
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
                if (count instanceof Number)
                    return ((Number) count).intValue();
                if (count instanceof String)
                    return Integer.parseInt((String) count);
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
                if (count instanceof Number)
                    return ((Number) count).intValue();
                if (count instanceof String)
                    return Integer.parseInt((String) count);
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
                if (area instanceof Number)
                    return ((Number) area).doubleValue();
                if (area instanceof String)
                    return Double.parseDouble((String) area);
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
                buildGuessItem(101L, "万科城市花园 精装三房 南向采光好", "南山区 · 89.5㎡ · 3室2厅2卫", 650.5,
                        "https://example.com/property/101"),
                buildGuessItem(102L, "华润城 四房大户型 学区房", "福田区 · 128㎡ · 4室2厅3卫", 980.0, "https://example.com/property/102"),
                buildGuessItem(103L, "科技园 地铁口复式 Loft", "南山区 · 68㎡ · 2室1厅1卫", 520.0,
                        "https://example.com/property/103"));
    }

    private Map<String, Object> buildGuessItem(Long propertyId, String title, String summary, Double price,
            String url) {
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
