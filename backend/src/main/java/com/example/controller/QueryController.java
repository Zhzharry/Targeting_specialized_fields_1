package com.example.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

            // 插入收藏记录
            String sql = "INSERT INTO favorites (user_id, property_id, favorite_data) VALUES (?, ?, ?)" +
                    " ON DUPLICATE KEY UPDATE favorite_data = VALUES(favorite_data), created_at = CURRENT_TIMESTAMP";

            try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, userId);
                ps.setLong(2, propertyId);
                ps.setString(3, "{}");
                ps.executeUpdate();

                Map<String, Object> body = new HashMap<String, Object>();
                body.put("message", "收藏成功");
                body.put("userId", userId);
                body.put("propertyId", propertyId);
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

        String sql = "DELETE FROM favorites WHERE user_id = ? AND property_id = ?";

        try (Connection connection = getConnection();
                PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, propertyId);
            int affected = ps.executeUpdate();

            Map<String, Object> body = new HashMap<String, Object>();
            body.put("userId", userId);
            body.put("propertyId", propertyId);
            body.put("message", affected > 0 ? "取消收藏成功" : "未找到对应的收藏记录");
            return ResponseEntity.ok(body);
        } catch (SQLException e) {
            return buildError("取消收藏失败", e);
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
