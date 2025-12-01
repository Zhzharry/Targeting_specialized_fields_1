package com.example.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 我的页面（个人中心）接口控制器。
 * 包含：设置偏好、房价预测、历史记录查询。
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

    static {
        try {
            Class.forName(MYSQL_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("未找到MySQL驱动，请确认已添加mysql-connector-java依赖", e);
        }
    }

    public ProfileController(@Value("${spring.datasource.url}") String jdbcUrl,
                             @Value("${spring.datasource.username}") String jdbcUsername,
                             @Value("${spring.datasource.password}") String jdbcPassword) {
        this.jdbcUrl = jdbcUrl;
        this.jdbcUsername = jdbcUsername;
        this.jdbcPassword = jdbcPassword;
    }

    /**
     * 设置/更新用户偏好。
     * 请求体包含 userId 及 preferenceData，会写入 user_preferences 表。
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
            ps.setString(2, objectMapper.writeValueAsString(preferenceData));
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
     * 房价预测接口：接收前端传入的房源信息，返回固定 1 元/㎡ 作为占位。
     * 日后可在此处接入真实的模型预测。
     */
    @PostMapping("/price-predict")
    public ResponseEntity<Map<String, Object>> pricePredict(@RequestBody Map<String, Object> payload) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("input", payload);
        result.put("predictedPricePerSquareMeter", 1);
        result.put("unit", "万元/㎡");
        result.put("message", "预测成功（示例值，后续可替换为真实算法）");
        return ResponseEntity.ok(result);
    }

    /**
     * 历史记录接口：返回用户的浏览历史（browsing_history 表），包含房源摘要。
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
}
