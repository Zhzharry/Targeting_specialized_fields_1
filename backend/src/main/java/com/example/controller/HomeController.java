package com.example.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 主页对应的接口控制器。
 * 提供首页展示所需的数据：个人信息和猜你喜欢。
 */
@RestController
@RequestMapping("/api/home")
@CrossOrigin(origins = "http://localhost:5173")
public class HomeController {

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

    public HomeController(@Value("${spring.datasource.url}") String jdbcUrl,
                          @Value("${spring.datasource.username}") String jdbcUsername,
                          @Value("${spring.datasource.password}") String jdbcPassword) {
        this.jdbcUrl = jdbcUrl;
        this.jdbcUsername = jdbcUsername;
        this.jdbcPassword = jdbcPassword;
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
        return DriverManager.getConnection(jdbcUrl, jdbcUsername, jdbcPassword);
    }
}
