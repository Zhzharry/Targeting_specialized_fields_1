package com.example.controller;

import com.example.service.UserSimilaritySparkService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 查询页面接口控制器。
 * 根据用户输入，从二手房数据库中查询房源信息。
 */
@RestController
@RequestMapping("/api/query")
@CrossOrigin(origins = "http://localhost:5173")
public class QueryController {

    private final DataSource dataSource;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired(required = false)
    private UserSimilaritySparkService userSimilaritySparkService;

    /**
     * 收藏房源。
     */
    @PostMapping("/favorite")
    public ResponseEntity<Map<String, Object>> addFavorite(
            @RequestParam("userId") Long userId,
            @RequestParam("propertyId") Long propertyId) {

        try (Connection connection = getConnection()) {
            // 验证用户是否存在
            String checkUserSql = "SELECT COUNT(1) FROM users WHERE user_id = ?";
            try (PreparedStatement checkUser = connection.prepareStatement(checkUserSql)) {
                checkUser.setLong(1, userId);
                try (ResultSet rs = checkUser.executeQuery()) {
                    if (!rs.next() || rs.getInt(1) == 0) {
                        Map<String, Object> error = new HashMap<String, Object>();
                        error.put("message", "用户不存在");
                        error.put("userId", userId);
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
                    }
                }
            }

            // 验证房源是否存在
            String checkPropertySql = "SELECT COUNT(1) FROM properties WHERE property_id = ?";
            try (PreparedStatement checkProperty = connection.prepareStatement(checkPropertySql)) {
                checkProperty.setLong(1, propertyId);
                try (ResultSet rs = checkProperty.executeQuery()) {
                    if (!rs.next() || rs.getInt(1) == 0) {
                        Map<String, Object> error = new HashMap<String, Object>();
                        error.put("message", "房源不存在");
                        error.put("propertyId", propertyId);
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
                    }
                }
            }

            // 检查是否已经收藏过
            boolean alreadyFavorited = false;
            String checkFavoriteSql = "SELECT COUNT(*) FROM favorites WHERE user_id = ? AND property_id = ?";
            try (PreparedStatement checkFavorite = connection.prepareStatement(checkFavoriteSql)) {
                checkFavorite.setLong(1, userId);
                checkFavorite.setLong(2, propertyId);
                try (ResultSet rs = checkFavorite.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        alreadyFavorited = true;
                    }
                }
            }

            // 插入收藏记录（如果不存在）
            String sql = "INSERT INTO favorites (user_id, property_id, favorite_data) VALUES (?, ?, ?)" +
                    " ON DUPLICATE KEY UPDATE favorite_data = VALUES(favorite_data), created_at = CURRENT_TIMESTAMP";

            try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, userId);
                ps.setLong(2, propertyId);
                ps.setString(3, "{}");
                int rowsAffected = ps.executeUpdate();

                // 如果成功插入新收藏（不是更新），则增加收藏次数
                if (!alreadyFavorited && rowsAffected > 0) {
                    String updateFavoriteCountSql = "UPDATE properties SET favorite_count = favorite_count + 1 WHERE property_id = ?";
                    try (PreparedStatement updatePs = connection.prepareStatement(updateFavoriteCountSql)) {
                        updatePs.setLong(1, propertyId);
                        updatePs.executeUpdate();
                    }
                }

                // 获取更新后的收藏次数
                int favoriteCount = 0;
                String getFavoriteCountSql = "SELECT favorite_count FROM properties WHERE property_id = ?";
                try (PreparedStatement getCountPs = connection.prepareStatement(getFavoriteCountSql)) {
                    getCountPs.setLong(1, propertyId);
                    try (ResultSet rs = getCountPs.executeQuery()) {
                        if (rs.next()) {
                            favoriteCount = rs.getInt("favorite_count");
                        }
                    }
                }

                // 计算用户收藏房源的总数
                int userFavoriteTotalCount = 0;
                String getUserFavoriteCountSql = "SELECT COUNT(*) FROM favorites WHERE user_id = ?";
                try (PreparedStatement getUserCountPs = connection.prepareStatement(getUserFavoriteCountSql)) {
                    getUserCountPs.setLong(1, userId);
                    try (ResultSet rs = getUserCountPs.executeQuery()) {
                        if (rs.next()) {
                            userFavoriteTotalCount = rs.getInt(1);
                        }
                    }
                }

                // 如果用户收藏数量是偶数，重新计算该用户的全部用户相似度
                if (userSimilaritySparkService != null && userFavoriteTotalCount > 0 && userFavoriteTotalCount % 2 == 0) {
                    System.out.println("用户 " + userId + " 收藏了 " + userFavoriteTotalCount + " 个房源（偶数），触发相似度计算");
                    // 异步执行，避免阻塞
                    new Thread(() -> {
                        try {
                            userSimilaritySparkService.calculateUserSimilarityIncremental(userId.intValue());
                        } catch (Exception e) {
                            System.err.println("触发用户相似度计算失败: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }).start();
                }

                Map<String, Object> body = new HashMap<String, Object>();
                body.put("message", alreadyFavorited ? "已收藏" : "收藏成功");
                body.put("userId", userId);
                body.put("propertyId", propertyId);
                body.put("favoriteCount", favoriteCount);
                body.put("success", true);
                return ResponseEntity.status(HttpStatus.CREATED).body(body);
            }
        } catch (SQLException e) {
            // 处理外键约束错误，提供更友好的提示
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("foreign key constraint")) {
                if (errorMessage.contains("property_id")) {
                    Map<String, Object> error = new HashMap<String, Object>();
                    error.put("message", "房源不存在，无法收藏");
                    error.put("propertyId", propertyId);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
                } else if (errorMessage.contains("user_id")) {
                    Map<String, Object> error = new HashMap<String, Object>();
                    error.put("message", "用户不存在，无法收藏");
                    error.put("userId", userId);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
                }
            }
            return buildError("收藏失败", e);
        }
    }

    /**
     * 取消收藏房源。
     */
    @DeleteMapping("/favorite")
    public ResponseEntity<Map<String, Object>> removeFavorite(
            @RequestParam("userId") Long userId,
            @RequestParam("propertyId") Long propertyId) {

        try (Connection connection = getConnection()) {
            // 删除收藏记录
            String sql = "DELETE FROM favorites WHERE user_id = ? AND property_id = ?";
            int affected = 0;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, userId);
                ps.setLong(2, propertyId);
                affected = ps.executeUpdate();
            }

            // 如果成功删除，减少收藏次数
            if (affected > 0) {
                String updateFavoriteCountSql = "UPDATE properties SET favorite_count = GREATEST(favorite_count - 1, 0) WHERE property_id = ?";
                try (PreparedStatement updatePs = connection.prepareStatement(updateFavoriteCountSql)) {
                    updatePs.setLong(1, propertyId);
                    updatePs.executeUpdate();
                }
            }

            // 获取更新后的收藏次数
            int favoriteCount = 0;
            String getFavoriteCountSql = "SELECT favorite_count FROM properties WHERE property_id = ?";
            try (PreparedStatement getCountPs = connection.prepareStatement(getFavoriteCountSql)) {
                getCountPs.setLong(1, propertyId);
                try (ResultSet rs = getCountPs.executeQuery()) {
                    if (rs.next()) {
                        favoriteCount = rs.getInt("favorite_count");
                    }
                }
            }

            // 计算用户收藏房源的总数（取消收藏后）
            int userFavoriteTotalCount = 0;
            if (affected > 0) {
                String getUserFavoriteCountSql = "SELECT COUNT(*) FROM favorites WHERE user_id = ?";
                try (PreparedStatement getUserCountPs = connection.prepareStatement(getUserFavoriteCountSql)) {
                    getUserCountPs.setLong(1, userId);
                    try (ResultSet rs = getUserCountPs.executeQuery()) {
                        if (rs.next()) {
                            userFavoriteTotalCount = rs.getInt(1);
                        }
                    }
                }

                // 如果用户收藏数量是偶数，重新计算该用户的全部用户相似度
                if (userSimilaritySparkService != null && userFavoriteTotalCount > 0 && userFavoriteTotalCount % 2 == 0) {
                    System.out.println("用户 " + userId + " 取消收藏后，剩余 " + userFavoriteTotalCount + " 个收藏（偶数），触发相似度计算");
                    // 异步执行，避免阻塞
                    new Thread(() -> {
                        try {
                            userSimilaritySparkService.calculateUserSimilarityIncremental(userId.intValue());
                        } catch (Exception e) {
                            System.err.println("触发用户相似度计算失败: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }).start();
                }
            }

            Map<String, Object> body = new HashMap<String, Object>();
            body.put("userId", userId);
            body.put("propertyId", propertyId);
            body.put("message", affected > 0 ? "取消收藏成功" : "未找到对应的收藏记录");
            body.put("favoriteCount", favoriteCount);
            body.put("userFavoriteTotalCount", userFavoriteTotalCount);
            body.put("success", affected > 0);
            return ResponseEntity.ok(body);
        } catch (SQLException e) {
            return buildError("取消收藏失败", e);
        }
    }

    /**
     * 获取房源详情
     * GET /api/query/property/{propertyId}?userId={userId}
     * 
     * 功能：
     * 1. 返回房源详细信息（价格、户型、位置等）
     * 2. 自动增加浏览次数（view_count + 1）
     * 3. 如果提供了userId，检查是否已收藏
     * 4. 返回收藏次数等信息
     */
    @GetMapping("/property/{propertyId}")
    public ResponseEntity<Map<String, Object>> getPropertyDetail(
            @PathVariable Long propertyId,
            @RequestParam(value = "userId", required = false) Long userId) {
        
        try (Connection connection = getConnection()) {
            // 1. 查询房源详细信息
            String sql = "SELECT p.property_id, p.title, p.status, p.price_info, p.layout_info, " +
                    "p.basic_info, p.view_count, p.favorite_count, p.updated_at, " +
                    "c.community_id, c.name AS community_name, c.location_info " +
                    "FROM properties p " +
                    "LEFT JOIN communities c ON p.community_id = c.community_id " +
                    "WHERE p.property_id = ?";
            
            Map<String, Object> property = null;
            int currentViewCount = 0;
            
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setLong(1, propertyId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        property = buildProperty(rs);
                        currentViewCount = rs.getInt("view_count");
                    } else {
                        Map<String, Object> error = new HashMap<>();
                        error.put("message", "房源不存在");
                        error.put("propertyId", propertyId);
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
                    }
                }
            }
            
            // 2. 增加浏览次数
            String updateViewCountSql = "UPDATE properties SET view_count = view_count + 1 WHERE property_id = ?";
            try (PreparedStatement ps = connection.prepareStatement(updateViewCountSql)) {
                ps.setLong(1, propertyId);
                ps.executeUpdate();
            }
            
            // 更新返回的浏览次数
            if (property != null) {
                property.put("viewCount", currentViewCount + 1);
            }
            
            // 3. 如果提供了userId，检查是否已收藏
            boolean isFavorited = false;
            if (userId != null) {
                String checkFavoriteSql = "SELECT COUNT(*) FROM favorites WHERE user_id = ? AND property_id = ?";
                try (PreparedStatement ps = connection.prepareStatement(checkFavoriteSql)) {
                    ps.setLong(1, userId);
                    ps.setLong(2, propertyId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            isFavorited = true;
                        }
                    }
                }
            }
            
            // 4. 记录浏览历史（如果提供了userId）
            if (userId != null) {
                try {
                    recordBrowseHistory(connection, userId, propertyId, null);
                } catch (Exception e) {
                    // 浏览记录保存失败不影响详情返回，只记录日志
                    System.err.println("记录浏览历史失败: " + e.getMessage());
                }
            }
            
            // 5. 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("property", property);
            result.put("isFavorited", isFavorited);
            
            return ResponseEntity.ok(result);
            
        } catch (SQLException e) {
            return buildError("获取房源详情失败", e);
        }
    }
    
    /**
     * 记录房源浏览。
     * 点击房源时调用此接口，会：
     * 1. 增加房源的浏览次数（view_count + 1）
     * 2. 如果 source 参数为 "history" 或 "favorite"（从浏览记录或收藏进入），只更新已有浏览记录的浏览时间
     * 3. 否则，如果已有浏览记录则更新浏览时间，如果没有则插入新记录
     */
    @PostMapping("/browse")
    public ResponseEntity<Map<String, Object>> recordBrowse(
            @RequestParam("userId") Long userId,
            @RequestParam("propertyId") Long propertyId,
            @RequestParam(value = "source", required = false) String source) {

        try (Connection connection = getConnection()) {
            // 验证用户是否存在
            String checkUserSql = "SELECT COUNT(1) FROM users WHERE user_id = ?";
            try (PreparedStatement checkUser = connection.prepareStatement(checkUserSql)) {
                checkUser.setLong(1, userId);
                try (ResultSet rs = checkUser.executeQuery()) {
                    if (!rs.next() || rs.getInt(1) == 0) {
                        Map<String, Object> error = new HashMap<String, Object>();
                        error.put("message", "用户不存在");
                        error.put("userId", userId);
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
                    }
                }
            }

            // 验证房源是否存在
            String checkPropertySql = "SELECT COUNT(1) FROM properties WHERE property_id = ?";
            try (PreparedStatement checkProperty = connection.prepareStatement(checkPropertySql)) {
                checkProperty.setLong(1, propertyId);
                try (ResultSet rs = checkProperty.executeQuery()) {
                    if (!rs.next() || rs.getInt(1) == 0) {
                        Map<String, Object> error = new HashMap<String, Object>();
                        error.put("message", "房源不存在");
                        error.put("propertyId", propertyId);
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
                    }
                }
            }

            // 增加房源的浏览次数
            String updateViewCountSql = "UPDATE properties SET view_count = view_count + 1 WHERE property_id = ?";
            try (PreparedStatement ps = connection.prepareStatement(updateViewCountSql)) {
                ps.setLong(1, propertyId);
                ps.executeUpdate();
            }

            // 判断来源：如果是从浏览记录或收藏进入，只更新已有记录的浏览时间
            boolean isFromHistoryOrFavorite = "history".equalsIgnoreCase(source) || "favorite".equalsIgnoreCase(source);

            if (isFromHistoryOrFavorite) {
                // 只更新已有浏览记录的浏览时间
                String updateHistorySql = "UPDATE browsing_history SET created_at = CURRENT_TIMESTAMP " +
                        "WHERE user_id = ? AND property_id = ?";
                try (PreparedStatement ps = connection.prepareStatement(updateHistorySql)) {
                    ps.setLong(1, userId);
                    ps.setLong(2, propertyId);
                    ps.executeUpdate();
                }
            } else {
                // 检查是否已有浏览记录
                String checkHistorySql = "SELECT COUNT(1) FROM browsing_history WHERE user_id = ? AND property_id = ?";
                boolean hasHistory = false;
                try (PreparedStatement checkHistory = connection.prepareStatement(checkHistorySql)) {
                    checkHistory.setLong(1, userId);
                    checkHistory.setLong(2, propertyId);
                    try (ResultSet rs = checkHistory.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            hasHistory = true;
                        }
                    }
                }

                if (hasHistory) {
                    // 更新已有浏览记录的浏览时间
                    String updateHistorySql = "UPDATE browsing_history SET created_at = CURRENT_TIMESTAMP " +
                            "WHERE user_id = ? AND property_id = ?";
                    try (PreparedStatement ps = connection.prepareStatement(updateHistorySql)) {
                        ps.setLong(1, userId);
                        ps.setLong(2, propertyId);
                        ps.executeUpdate();
                    }
                } else {
                    // 插入新的浏览记录
                    String insertHistorySql = "INSERT INTO browsing_history (user_id, property_id, behavior_data) " +
                            "VALUES (?, ?, ?)";
                    try (PreparedStatement ps = connection.prepareStatement(insertHistorySql)) {
                        ps.setLong(1, userId);
                        ps.setLong(2, propertyId);
                        ps.setString(3, "{}"); // 默认空的行为数据
                        ps.executeUpdate();
                    }
                }
            }

            Map<String, Object> body = new HashMap<String, Object>();
            body.put("message", "浏览记录已保存");
            body.put("userId", userId);
            body.put("propertyId", propertyId);
            return ResponseEntity.ok(body);

        } catch (SQLException e) {
            return buildError("记录浏览失败", e);
        }
    }

    @Autowired
    public QueryController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 根据关键字与多种筛选条件查询房源列表。
     * 调用此接口会自动增加返回房源的访问次数。
     * 如果提供了userId，会同时保存浏览记录。
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "district", required = false) String district,
            @RequestParam(value = "minPrice", required = false) Double minPrice,
            @RequestParam(value = "maxPrice", required = false) Double maxPrice,
            @RequestParam(value = "propertyType", required = false) String propertyType,
            @RequestParam(value = "orientation", required = false) String orientation,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "minBedrooms", required = false) Integer minBedrooms,
            @RequestParam(value = "maxBedrooms", required = false) Integer maxBedrooms,
            @RequestParam(value = "minArea", required = false) Double minArea,
            @RequestParam(value = "maxArea", required = false) Double maxArea,
            @RequestParam(value = "minViewCount", required = false) Integer minViewCount,
            @RequestParam(value = "maxViewCount", required = false) Integer maxViewCount,
            @RequestParam(value = "sortBy", required = false, defaultValue = "updated_at") String sortBy,
            @RequestParam(value = "sortOrder", required = false, defaultValue = "desc") String sortOrder,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "source", required = false) String source) {

        // 如果提供了keyword（小区名称），从transaction_records表的transaction_data JSON中搜索community_name
        if (hasText(keyword)) {
            System.out.println("\n[Search] ========================================");
            System.out.println("[Search] Searching by community name in transaction_records table");
            System.out.println("[Search] Keyword: " + keyword);
            System.out.println("[Search] Note: Matching against transaction_data JSON field -> community_name");
            System.out.println("[Search] ========================================\n");
            
            ResponseEntity<Map<String, Object>> transactionResult = searchFromTransactionRecords(keyword, minPrice, maxPrice, userId, source);
            Map<String, Object> transactionBody = (Map<String, Object>) transactionResult.getBody();
            if (transactionBody != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) transactionBody.get("items");
                // 如果transaction_records有结果，直接返回
                if (items != null && !items.isEmpty()) {
                    System.out.println("\n[Search] ✓ Found " + items.size() + " results from transaction_records");
                    System.out.println("[Search] Community names matched: " + keyword);
                    return transactionResult;
                } else {
                    System.out.println("\n[Search] ✗ No results found in transaction_records for community_name: " + keyword);
                    System.out.println("[Search] Falling back to properties table search...\n");
                }
            }
        }

        // 使用原来的properties表搜索逻辑（包括fallback情况）
        String baseSql = "SELECT p.property_id, p.title, p.status, p.price_info, p.layout_info, p.basic_info, " +
                "p.view_count, p.favorite_count, p.updated_at, c.name AS community_name, c.location_info " +
                "FROM properties p LEFT JOIN communities c ON p.community_id = c.community_id WHERE 1 = 1";

        StringBuilder sqlBuilder = new StringBuilder(baseSql);
        List<Object> params = new ArrayList<Object>();

        if (hasText(keyword)) {
            sqlBuilder.append(" AND (p.title LIKE ? OR c.name LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
        }

        if (hasText(district)) {
            sqlBuilder.append(" AND JSON_UNQUOTE(JSON_EXTRACT(c.location_info,'$.district')) = ?");
            params.add(district.trim());
        }

        if (minPrice != null) {
            sqlBuilder.append(
                    " AND CAST(JSON_UNQUOTE(JSON_EXTRACT(p.price_info,'$.total_price')) AS DECIMAL(12,2)) >= ?");
            params.add(minPrice);
        }

        if (maxPrice != null) {
            sqlBuilder.append(
                    " AND CAST(JSON_UNQUOTE(JSON_EXTRACT(p.price_info,'$.total_price')) AS DECIMAL(12,2)) <= ?");
            params.add(maxPrice);
        }

        if (hasText(propertyType)) {
            sqlBuilder.append(" AND JSON_UNQUOTE(JSON_EXTRACT(p.basic_info,'$.property_type')) = ?");
            params.add(propertyType.trim());
        }

        if (hasText(orientation)) {
            sqlBuilder.append(" AND JSON_UNQUOTE(JSON_EXTRACT(p.layout_info,'$.orientation')) = ?");
            params.add(orientation.trim());
        }

        if (hasText(status)) {
            sqlBuilder.append(" AND p.status = ?");
            params.add(status.trim());
        }

        if (minBedrooms != null) {
            sqlBuilder
                    .append(" AND CAST(JSON_UNQUOTE(JSON_EXTRACT(p.layout_info,'$.bedroom_count')) AS UNSIGNED) >= ?");
            params.add(minBedrooms);
        }

        if (maxBedrooms != null) {
            sqlBuilder
                    .append(" AND CAST(JSON_UNQUOTE(JSON_EXTRACT(p.layout_info,'$.bedroom_count')) AS UNSIGNED) <= ?");
            params.add(maxBedrooms);
        }

        if (minArea != null) {
            sqlBuilder.append(" AND CAST(JSON_UNQUOTE(JSON_EXTRACT(p.layout_info,'$.area')) AS DECIMAL(12,2)) >= ?");
            params.add(minArea);
        }

        if (maxArea != null) {
            sqlBuilder.append(" AND CAST(JSON_UNQUOTE(JSON_EXTRACT(p.layout_info,'$.area')) AS DECIMAL(12,2)) <= ?");
            params.add(maxArea);
        }

        if (minViewCount != null) {
            sqlBuilder.append(" AND p.view_count >= ?");
            params.add(minViewCount);
        }

        if (maxViewCount != null) {
            sqlBuilder.append(" AND p.view_count <= ?");
            params.add(maxViewCount);
        }

        // Validate sortBy parameter
        String validSortBy = "updated_at"; // default
        if ("price".equals(sortBy) || "area".equals(sortBy) || "view_count".equals(sortBy)
                || "updated_at".equals(sortBy)) {
            validSortBy = sortBy;
        }

        // Validate sortOrder parameter
        String validSortOrder = "desc"; // default
        if ("asc".equalsIgnoreCase(sortOrder) || "desc".equalsIgnoreCase(sortOrder)) {
            validSortOrder = sortOrder.toLowerCase();
        }

        // Build ORDER BY clause
        String orderByClause = " ORDER BY ";
        if ("price".equals(validSortBy)) {
            orderByClause += "CAST(JSON_UNQUOTE(JSON_EXTRACT(p.price_info,'$.total_price')) AS DECIMAL(12,2))";
        } else if ("area".equals(validSortBy)) {
            orderByClause += "CAST(JSON_UNQUOTE(JSON_EXTRACT(p.layout_info,'$.area')) AS DECIMAL(12,2))";
        } else if ("view_count".equals(validSortBy)) {
            orderByClause += "p.view_count";
        } else {
            orderByClause += "p.updated_at";
        }
        orderByClause += " " + validSortOrder + " LIMIT 20";

        sqlBuilder.append(orderByClause);

        System.out.println("[Search] Searching properties table:");
        System.out.println("  - SQL: " + sqlBuilder.toString());
        System.out.println("  - Parameters count: " + params.size());

        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sqlBuilder.toString())) {

            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }

            List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
            Set<Long> propertyIds = new HashSet<Long>(); // 用于记录需要增加访问次数的房源ID
            
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Long propertyId = rs.getLong("property_id");
                    items.add(buildProperty(rs));
                    propertyIds.add(propertyId);
                }
            }
            
            System.out.println("[Search] Properties table search completed: " + items.size() + " results found");

            // 自动增加所有返回房源的访问次数
            if (!propertyIds.isEmpty()) {
                try {
                    increaseViewCount(connection, propertyIds);
                } catch (Exception e) {
                    // 访问次数增加失败不影响查询结果，只记录日志
                    System.err.println("增加访问次数失败: " + e.getMessage());
                }
            }

            // 如果提供了userId，且查询结果只有1个房源，保存浏览记录
            if (userId != null && propertyIds.size() == 1) {
                try {
                    Long propertyId = propertyIds.iterator().next();
                    recordBrowseHistory(connection, userId, propertyId, source);
                } catch (Exception e) {
                    // 浏览记录保存失败不影响查询结果，只记录日志
                    System.err.println("记录浏览失败: " + e.getMessage());
                }
            }

            Map<String, Object> response = new HashMap<String, Object>();
            response.put("items", items);
            response.put("count", items.size());
            response.put("message", "查询成功");
            return ResponseEntity.ok(response);

        } catch (SQLException e) {
            return buildError("查询失败", e);
        }
    }
    
    /**
     * 批量增加房源的访问次数
     */
    private void increaseViewCount(Connection connection, Set<Long> propertyIds) throws SQLException {
        if (propertyIds == null || propertyIds.isEmpty()) {
            return;
        }
        
        // 批量更新访问次数
        String updateViewCountSql = "UPDATE properties SET view_count = view_count + 1 WHERE property_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(updateViewCountSql)) {
            for (Long propertyId : propertyIds) {
                ps.setLong(1, propertyId);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
    
    /**
     * 保存浏览记录（不增加访问次数，访问次数由increaseViewCount统一处理）
     */
    private void recordBrowseHistory(Connection connection, Long userId, Long propertyId, String source) throws SQLException {
        // 验证用户是否存在
        String checkUserSql = "SELECT COUNT(1) FROM users WHERE user_id = ?";
        try (PreparedStatement checkUser = connection.prepareStatement(checkUserSql)) {
            checkUser.setLong(1, userId);
            try (ResultSet rs = checkUser.executeQuery()) {
                if (!rs.next() || rs.getInt(1) == 0) {
                    System.err.println("用户不存在: " + userId);
                    return;
                }
            }
        }

        // 验证房源是否存在
        String checkPropertySql = "SELECT COUNT(1) FROM properties WHERE property_id = ?";
        try (PreparedStatement checkProperty = connection.prepareStatement(checkPropertySql)) {
            checkProperty.setLong(1, propertyId);
            try (ResultSet rs = checkProperty.executeQuery()) {
                if (!rs.next() || rs.getInt(1) == 0) {
                    System.err.println("房源不存在: " + propertyId);
                    return;
                }
            }
        }

        // 判断来源：如果是从浏览记录或收藏进入，只更新已有记录的浏览时间
        boolean isFromHistoryOrFavorite = "history".equalsIgnoreCase(source) || "favorite".equalsIgnoreCase(source);

        if (isFromHistoryOrFavorite) {
            // 只更新已有浏览记录的浏览时间
            String updateHistorySql = "UPDATE browsing_history SET created_at = CURRENT_TIMESTAMP " +
                    "WHERE user_id = ? AND property_id = ?";
            try (PreparedStatement ps = connection.prepareStatement(updateHistorySql)) {
                ps.setLong(1, userId);
                ps.setLong(2, propertyId);
                ps.executeUpdate();
            }
        } else {
            // 检查是否已有浏览记录
            String checkHistorySql = "SELECT COUNT(1) FROM browsing_history WHERE user_id = ? AND property_id = ?";
            boolean hasHistory = false;
            try (PreparedStatement checkHistory = connection.prepareStatement(checkHistorySql)) {
                checkHistory.setLong(1, userId);
                checkHistory.setLong(2, propertyId);
                try (ResultSet rs = checkHistory.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        hasHistory = true;
                    }
                }
            }

            if (hasHistory) {
                // 更新已有浏览记录的浏览时间
                String updateHistorySql = "UPDATE browsing_history SET created_at = CURRENT_TIMESTAMP " +
                        "WHERE user_id = ? AND property_id = ?";
                try (PreparedStatement ps = connection.prepareStatement(updateHistorySql)) {
                    ps.setLong(1, userId);
                    ps.setLong(2, propertyId);
                    ps.executeUpdate();
                }
            } else {
                // 插入新的浏览记录
                String insertHistorySql = "INSERT INTO browsing_history (user_id, property_id, behavior_data) " +
                        "VALUES (?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(insertHistorySql)) {
                    ps.setLong(1, userId);
                    ps.setLong(2, propertyId);
                    ps.setString(3, "{}"); // 默认空的行为数据
                    ps.executeUpdate();
                }
            }
        }
    }

    private Map<String, Object> buildProperty(ResultSet rs) throws SQLException {
        Map<String, Object> item = new HashMap<String, Object>();
        item.put("propertyId", rs.getLong("property_id"));
        item.put("title", rs.getString("title"));
        item.put("status", rs.getString("status"));
        item.put("communityName", rs.getString("community_name"));
        item.put("viewCount", rs.getInt("view_count"));
        item.put("favoriteCount", rs.getInt("favorite_count"));
        item.put("updatedAt", rs.getTimestamp("updated_at"));
        
        // 解析JSON字段
        Map<String, Object> priceInfo = parseJsonToMap(rs.getString("price_info"));
        Map<String, Object> layoutInfo = parseJsonToMap(rs.getString("layout_info"));
        Map<String, Object> basicInfo = parseJsonToMap(rs.getString("basic_info"));
        Map<String, Object> locationInfo = parseJsonToMap(rs.getString("location_info"));
        
        // 确保layoutInfo包含所有必要字段，处理null值
        if (layoutInfo != null) {
            // 确保楼层信息存在
            if (!layoutInfo.containsKey("floor") || layoutInfo.get("floor") == null) {
                layoutInfo.put("floor", null);
            }
            if (!layoutInfo.containsKey("total_floors") || layoutInfo.get("total_floors") == null) {
                layoutInfo.put("total_floors", null);
            }
            // 确保朝向信息存在
            if (!layoutInfo.containsKey("orientation") || layoutInfo.get("orientation") == null) {
                layoutInfo.put("orientation", null);
            }
            // 确保其他字段存在
            if (!layoutInfo.containsKey("bedroom_count") || layoutInfo.get("bedroom_count") == null) {
                layoutInfo.put("bedroom_count", 0);
            }
            if (!layoutInfo.containsKey("living_room_count") || layoutInfo.get("living_room_count") == null) {
                layoutInfo.put("living_room_count", 0);
            }
            if (!layoutInfo.containsKey("bathroom_count") || layoutInfo.get("bathroom_count") == null) {
                layoutInfo.put("bathroom_count", 0);
            }
            if (!layoutInfo.containsKey("area") || layoutInfo.get("area") == null) {
                layoutInfo.put("area", null);
            }
        } else {
            layoutInfo = new HashMap<String, Object>();
            layoutInfo.put("floor", null);
            layoutInfo.put("total_floors", null);
            layoutInfo.put("orientation", null);
            layoutInfo.put("bedroom_count", 0);
            layoutInfo.put("living_room_count", 0);
            layoutInfo.put("bathroom_count", 0);
            layoutInfo.put("area", null);
        }
        
        // 确保basicInfo包含所有必要字段
        if (basicInfo != null) {
            // 确保装修信息存在
            if (!basicInfo.containsKey("decoration") || basicInfo.get("decoration") == null) {
                basicInfo.put("decoration", null);
            }
            // 确保建造年份存在
            if (!basicInfo.containsKey("build_year") || basicInfo.get("build_year") == null) {
                basicInfo.put("build_year", null);
            }
        } else {
            basicInfo = new HashMap<String, Object>();
            basicInfo.put("decoration", null);
            basicInfo.put("build_year", null);
        }
        
        // 确保priceInfo包含所有必要字段
        if (priceInfo == null) {
            priceInfo = new HashMap<String, Object>();
            priceInfo.put("total_price", null);
            priceInfo.put("unit_price", null);
        }
        
        // 确保locationInfo包含所有必要字段
        if (locationInfo == null) {
            locationInfo = new HashMap<String, Object>();
            locationInfo.put("province", null);
            locationInfo.put("city", null);
            locationInfo.put("district", null);
        }
        
        item.put("priceInfo", priceInfo);
        item.put("layoutInfo", layoutInfo);
        item.put("basicInfo", basicInfo);
        item.put("locationInfo", locationInfo);
        
        return item;
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonToMap(String json) {
        if (!hasText(json)) {
            return null;
        }
        try {
            Object parsed = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            if (parsed instanceof Map) {
                return (Map<String, Object>) parsed;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private Object parseJson(String json) {
        if (!hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return json;
        }
    }

    /**
     * 从transaction_records表搜索房源（根据小区名称和价格区间）
     */
    private ResponseEntity<Map<String, Object>> searchFromTransactionRecords(
            String communityName,
            Double minPrice,
            Double maxPrice,
            Long userId,
            String source) {
        
        System.out.println("[Search] Searching transaction_records table:");
        System.out.println("  - Community name: " + communityName);
        System.out.println("  - Min price: " + minPrice);
        System.out.println("  - Max price: " + maxPrice);
        
        try (Connection connection = getConnection()) {
            // 构建SQL查询，从transaction_records表查询
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT DISTINCT ")
                    .append("tr.transaction_id, ")
                    .append("JSON_UNQUOTE(JSON_EXTRACT(tr.transaction_data, '$.community_name')) AS community_name, ")
                    .append("CAST(JSON_UNQUOTE(JSON_EXTRACT(tr.transaction_data, '$.total_price')) AS DECIMAL(12,2)) AS total_price, ")
                    .append("JSON_EXTRACT(tr.transaction_data, '$.area') AS area, ")
                    .append("JSON_EXTRACT(tr.transaction_data, '$.bedroom_count') AS bedroom_count, ")
                    .append("JSON_EXTRACT(tr.transaction_data, '$.living_room_count') AS living_room_count, ")
                    .append("JSON_EXTRACT(tr.transaction_data, '$.bathroom_count') AS bathroom_count, ")
                    .append("JSON_EXTRACT(tr.transaction_data, '$.orientation') AS orientation, ")
                    .append("JSON_EXTRACT(tr.transaction_data, '$.floor') AS floor, ")
                    .append("JSON_EXTRACT(tr.transaction_data, '$.total_floors') AS total_floors, ")
                    .append("JSON_EXTRACT(tr.transaction_data, '$.decoration') AS decoration, ")
                    .append("JSON_EXTRACT(tr.transaction_data, '$.build_year') AS build_year, ")
                    .append("JSON_EXTRACT(tr.transaction_data, '$.property_type') AS property_type, ")
                    .append("JSON_EXTRACT(tr.transaction_data, '$.district') AS district, ")
                    .append("JSON_EXTRACT(tr.transaction_data, '$.city') AS city, ")
                    .append("JSON_EXTRACT(tr.transaction_data, '$.province') AS province, ")
                    .append("JSON_EXTRACT(tr.transaction_data, '$.address') AS address, ")
                    .append("tr.transaction_data, ")
                    .append("tr.created_at ")
                    .append("FROM transaction_records tr ")
                    .append("WHERE 1 = 1");
            
            List<Object> params = new ArrayList<Object>();
            
            // 小区名称筛选 - 从transaction_data JSON中的community_name字段匹配
            if (hasText(communityName)) {
                // 使用LIKE进行模糊匹配，支持部分匹配
                // 注意：这里匹配的是 transaction_data JSON 中的 community_name 字段
                sqlBuilder.append(" AND JSON_UNQUOTE(JSON_EXTRACT(tr.transaction_data, '$.community_name')) LIKE ?");
                String searchPattern = "%" + communityName.trim() + "%";
                params.add(searchPattern);
                System.out.println("  - Community name search pattern: " + searchPattern);
                System.out.println("  - SQL condition: JSON_UNQUOTE(JSON_EXTRACT(tr.transaction_data, '$.community_name')) LIKE ?");
            }
            
            // 价格区间筛选
            if (minPrice != null) {
                sqlBuilder.append(" AND CAST(JSON_UNQUOTE(JSON_EXTRACT(tr.transaction_data, '$.total_price')) AS DECIMAL(12,2)) >= ?");
                params.add(minPrice);
            }
            
            if (maxPrice != null) {
                sqlBuilder.append(" AND CAST(JSON_UNQUOTE(JSON_EXTRACT(tr.transaction_data, '$.total_price')) AS DECIMAL(12,2)) <= ?");
                params.add(maxPrice);
            }
            
            // 按创建时间倒序，限制返回20条
            sqlBuilder.append(" ORDER BY tr.created_at DESC LIMIT 20");
            
            String finalSql = sqlBuilder.toString();
            System.out.println("  - Final SQL: " + finalSql);
            System.out.println("  - SQL Parameters: " + params);
            
            List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
            
            try (PreparedStatement ps = connection.prepareStatement(finalSql)) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                    System.out.println("    Parameter " + (i + 1) + ": " + params.get(i));
                }
                
                try (ResultSet rs = ps.executeQuery()) {
                    int rowCount = 0;
                    while (rs.next()) {
                        rowCount++;
                        Map<String, Object> item = buildPropertyFromTransactionRecord(rs);
                        String foundCommunityName = (String) item.get("communityName");
                        System.out.println("    Row " + rowCount + ": Found community_name = " + foundCommunityName);
                        items.add(item);
                    }
                    System.out.println("  - Total rows fetched: " + rowCount);
                }
            }
            
            System.out.println("[Search] Transaction records search completed: " + items.size() + " results found");
            
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("items", items);
            response.put("count", items.size());
            response.put("message", "查询成功");
            return ResponseEntity.ok(response);
            
        } catch (SQLException e) {
            System.err.println("[Search] Error searching transaction_records: " + e.getMessage());
            e.printStackTrace();
            return buildError("查询失败", e);
        }
    }
    
    /**
     * 从transaction_records的ResultSet构建Property对象
     */
    private Map<String, Object> buildPropertyFromTransactionRecord(ResultSet rs) throws SQLException {
        Map<String, Object> item = new HashMap<String, Object>();
        
        // 基本信息
        item.put("propertyId", rs.getLong("transaction_id")); // 使用transaction_id作为propertyId
        item.put("title", rs.getString("community_name") + " 房源");
        item.put("status", "for_sale");
        item.put("communityName", rs.getString("community_name"));
        item.put("viewCount", 0);
        item.put("favoriteCount", 0);
        item.put("updatedAt", rs.getTimestamp("created_at"));
        
        // 构建priceInfo
        Map<String, Object> priceInfo = new HashMap<String, Object>();
        Double totalPrice = rs.getDouble("total_price");
        if (rs.wasNull()) {
            totalPrice = 0.0;
        }
        priceInfo.put("total_price", totalPrice);
        priceInfo.put("unit_price", 0.0); // transaction_records可能没有unit_price
        item.put("priceInfo", priceInfo);
        
        // 构建layoutInfo
        Map<String, Object> layoutInfo = new HashMap<String, Object>();
        layoutInfo.put("area", getDoubleFromResultSet(rs, "area"));
        layoutInfo.put("bedroom_count", getIntFromResultSet(rs, "bedroom_count"));
        layoutInfo.put("living_room_count", getIntFromResultSet(rs, "living_room_count"));
        layoutInfo.put("bathroom_count", getIntFromResultSet(rs, "bathroom_count"));
        layoutInfo.put("orientation", getStringFromResultSet(rs, "orientation"));
        layoutInfo.put("floor", getIntFromResultSet(rs, "floor"));
        layoutInfo.put("total_floors", getIntFromResultSet(rs, "total_floors"));
        item.put("layoutInfo", layoutInfo);
        
        // 构建basicInfo
        Map<String, Object> basicInfo = new HashMap<String, Object>();
        basicInfo.put("property_type", getStringFromResultSet(rs, "property_type"));
        basicInfo.put("decoration", getStringFromResultSet(rs, "decoration"));
        basicInfo.put("build_year", getIntFromResultSet(rs, "build_year"));
        item.put("basicInfo", basicInfo);
        
        // 构建locationInfo
        Map<String, Object> locationInfo = new HashMap<String, Object>();
        locationInfo.put("province", getStringFromResultSet(rs, "province"));
        locationInfo.put("city", getStringFromResultSet(rs, "city"));
        locationInfo.put("district", getStringFromResultSet(rs, "district"));
        locationInfo.put("address", getStringFromResultSet(rs, "address"));
        item.put("locationInfo", locationInfo);
        
        return item;
    }
    
    /**
     * 从ResultSet安全获取Double值
     */
    private Double getDoubleFromResultSet(ResultSet rs, String columnName) throws SQLException {
        try {
            Object value = rs.getObject(columnName);
            if (value == null || rs.wasNull()) {
                return null;
            }
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            if (value instanceof String) {
                try {
                    return Double.parseDouble((String) value);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        } catch (SQLException e) {
            return null;
        }
    }
    
    /**
     * 从ResultSet安全获取Integer值
     */
    private Integer getIntFromResultSet(ResultSet rs, String columnName) throws SQLException {
        try {
            Object value = rs.getObject(columnName);
            if (value == null || rs.wasNull()) {
                return null;
            }
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            if (value instanceof String) {
                try {
                    return Integer.parseInt((String) value);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        } catch (SQLException e) {
            return null;
        }
    }
    
    /**
     * 从ResultSet安全获取String值
     */
    private String getStringFromResultSet(ResultSet rs, String columnName) throws SQLException {
        try {
            String value = rs.getString(columnName);
            return (value != null && !rs.wasNull()) ? value : null;
        } catch (SQLException e) {
            return null;
        }
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private ResponseEntity<Map<String, Object>> buildError(String message, Exception e) {
        Map<String, Object> error = new HashMap<String, Object>();
        error.put("message", message);
        error.put("error", e.getMessage());
        return ResponseEntity.internalServerError().body(error);
    }
}
