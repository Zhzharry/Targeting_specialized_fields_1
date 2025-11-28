package com.example.controller;

import com.example.service.predict_zhz.HousePricePredictionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 我的页面（个人中心）接口控制器。
 * 统一对接用户偏好、房价预测、历史记录、收藏等功能：
 * - user_preferences  表：保存偏好配置，供主页/我的页使用
 * - browsing_history 表：浏览记录
 * - favorites        表：收藏逻辑（与查询页接口保持复用）
 * - properties       表：补充收藏、历史记录的房源详情
 */
@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "http://localhost:5173")
public class ProfileController {

    private static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";

    private final String jdbcUrl;
    private final String jdbcUsername;
    private final String jdbcPassword;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HousePricePredictionService predictionService;
    private static final Set<String> ALLOWED_PREFERENCE_FIELDS = new HashSet<String>(Arrays.asList(
            "price_range",
            "area_range",
            "locations",
            "districts",
            "city",
            "house_types",
            "bedroom_range",
            "orientations",
            "decorations",
            "keywords",
            "budget"
    ));

    static {
        try {
            Class.forName(MYSQL_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("未找到MySQL驱动，请确认已添加mysql-connector-java依赖", e);
        }
    }

    @Autowired
    public ProfileController(@Value("${spring.datasource.url}") String jdbcUrl,
                             @Value("${spring.datasource.username}") String jdbcUsername,
                             @Value("${spring.datasource.password}") String jdbcPassword,
                             HousePricePredictionService predictionService) {
        this.jdbcUrl = jdbcUrl;
        this.jdbcUsername = jdbcUsername;
        this.jdbcPassword = jdbcPassword;
        this.predictionService = predictionService;
    }

    /**
     * 设置/更新用户偏好。
     * 请求体包含 userId 及 preferenceData，会写入 user_preferences 表。
     * preferenceData 支持 price_range/area_range/locations 等字段，详见 ALLOWED_PREFERENCE_FIELDS。
     */
    @PostMapping("/preferences")
    public ResponseEntity<Map<String, Object>> savePreference(@RequestBody Map<String, Object> payload) {
        Long userId = valueAsLong(payload.get("userId"));
        Object preferenceData = payload.get("preferenceData");

        if (userId == null || preferenceData == null) {
            return error(HttpStatus.BAD_REQUEST, "userId 和 preferenceData 不能为空");
        }

        String sql = "INSERT INTO user_preferences (user_id, preference_data) VALUES (?, ?)";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, sanitizePreferenceData(preferenceData));
            ps.executeUpdate();

            Map<String, Object> body = new HashMap<String, Object>();
            body.put("message", "偏好设置成功");
            body.put("userId", userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        } catch (Exception e) {
            return serverError("保存偏好失败", e);
        }
    }

    /**
     * 房价预测接口：根据城市和特征调用模型并返回预测单价（万元/㎡）。
     * 复用 predictor_zhz 目录下的城市模型（beijng/shanghai/tianjin/shijiazhuang）。
     */
    @PostMapping("/price-predict")
    public ResponseEntity<Map<String, Object>> pricePredict(@RequestBody Map<String, Object> payload) {
        String city = asText(payload.get("city"));
        if (city == null) {
            return error(HttpStatus.BAD_REQUEST, "city 不能为空");
        }

        Map<String, Object> features = extractFeatures(payload);
        if (features.isEmpty()) {
            return error(HttpStatus.BAD_REQUEST, "features 不能为空");
        }

        try {
            double predicted = predictionService.predict(city, features);
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("city", city);
            result.put("features", features);
            result.put("predictedPricePerSquareMeter", predicted);
            result.put("unit", "万元/㎡");
            result.put("message", "预测成功");
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (FileNotFoundException e) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            return serverError("预测失败", e);
        }
    }

    /**
     * 历史记录接口：返回用户的浏览历史（browsing_history 表），包含房源摘要。
     * 复用与主页/查询页一致的 browsing_history 数据结构。
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> history(@RequestParam("userId") Long userId) {
        String sql = "SELECT bh.history_id, bh.property_id, bh.behavior_data, bh.created_at, " +
                "p.title, p.price_info, p.layout_info " +
                "FROM browsing_history bh " +
                "LEFT JOIN properties p ON bh.property_id = p.property_id " +
                "WHERE bh.user_id = ? ORDER BY bh.created_at DESC LIMIT 50";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);

            List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(buildHistoryItem(rs));
                }
            }

            Map<String, Object> body = new HashMap<String, Object>();
            body.put("items", list);
            body.put("count", list.size());
            body.put("message", "历史记录获取成功");
            return ResponseEntity.ok(body);
        } catch (SQLException e) {
            return serverError("获取历史记录失败", e);
        }
    }

    /**
     * 收藏列表接口：返回用户收藏的全部房源（favorites 表）。
     * 与查询页收藏接口复用同一张表，方便前端在多个入口共享收藏记录。
     */
    @GetMapping("/favorites")
    public ResponseEntity<Map<String, Object>> favorites(@RequestParam("userId") Long userId) {
        String sql = "SELECT f.favorite_id, f.property_id, f.created_at, " +
                "p.title, p.price_info, p.layout_info " +
                "FROM favorites f " +
                "LEFT JOIN properties p ON f.property_id = p.property_id " +
                "WHERE f.user_id = ? ORDER BY f.created_at DESC";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, userId);

            List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(buildFavoriteItem(rs));
                }
            }

            Map<String, Object> body = new HashMap<String, Object>();
            body.put("items", list);
            body.put("count", list.size());
            body.put("message", "收藏列表获取成功");
            return ResponseEntity.ok(body);
        } catch (SQLException e) {
            return serverError("获取收藏列表失败", e);
        }
    }

    private Map<String, Object> buildHistoryItem(ResultSet rs) throws SQLException {
        Map<String, Object> item = new HashMap<String, Object>();
        item.put("historyId", rs.getLong("history_id"));
        item.put("propertyId", rs.getLong("property_id"));
        item.put("title", rs.getString("title"));
        item.put("behaviorData", parseJson(rs.getString("behavior_data")));
        item.put("priceInfo", parseJson(rs.getString("price_info")));
        item.put("layoutInfo", parseJson(rs.getString("layout_info")));
        Timestamp createdAt = rs.getTimestamp("created_at");
        item.put("createdAt", createdAt);
        return item;
    }

    private Map<String, Object> buildFavoriteItem(ResultSet rs) throws SQLException {
        Map<String, Object> item = new HashMap<String, Object>();
        item.put("favoriteId", rs.getLong("favorite_id"));
        item.put("propertyId", rs.getLong("property_id"));
        item.put("title", rs.getString("title"));
        item.put("priceInfo", parseJson(rs.getString("price_info")));
        item.put("layoutInfo", parseJson(rs.getString("layout_info")));
        Timestamp createdAt = rs.getTimestamp("created_at");
        item.put("createdAt", createdAt);
        return item;
    }

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

    private Long valueAsLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String asText(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractFeatures(Map<String, Object> payload) {
        Object nested = payload.get("features");
        if (nested instanceof Map) {
            return new HashMap<String, Object>((Map<String, Object>) nested);
        }
        Map<String, Object> copy = new HashMap<String, Object>(payload);
        copy.remove("city");
        return copy;
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
        return DriverManager.getConnection(jdbcUrl, jdbcUsername, jdbcPassword);
    }

    @SuppressWarnings("unchecked")
    private String sanitizePreferenceData(Object preferenceData) throws Exception {
        if (preferenceData instanceof Map) {
            Map<String, Object> raw = (Map<String, Object>) preferenceData;
            Map<String, Object> sanitized = new HashMap<String, Object>();
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                if (ALLOWED_PREFERENCE_FIELDS.contains(entry.getKey())) {
                    sanitized.put(entry.getKey(), entry.getValue());
                }
            }
            if (sanitized.isEmpty()) {
                sanitized.putAll(raw);
            }
            return objectMapper.writeValueAsString(sanitized);
        }
        if (preferenceData instanceof String) {
            return preferenceData.toString();
        }
        return objectMapper.writeValueAsString(preferenceData);
    }
}
