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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 查询页面接口控制器。
 * 根据用户输入，从二手房数据库中查询房源信息。
 */
@RestController
@RequestMapping("/api/query")
@CrossOrigin(origins = "http://localhost:5173")
public class QueryController {

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

    public QueryController(@Value("${spring.datasource.url}") String jdbcUrl,
                           @Value("${spring.datasource.username}") String jdbcUsername,
                           @Value("${spring.datasource.password}") String jdbcPassword) {
        this.jdbcUrl = jdbcUrl;
        this.jdbcUsername = jdbcUsername;
        this.jdbcPassword = jdbcPassword;
    }

    /**
     * 根据关键字与筛选条件查询房源列表。
     *
     * @param keyword   标题或小区关键字
     * @param district  行政区
     * @param minPrice  最低总价（万元）
     * @param maxPrice  最高总价（万元）
     * @return 查询结果
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "district", required = false) String district,
            @RequestParam(value = "minPrice", required = false) Double minPrice,
            @RequestParam(value = "maxPrice", required = false) Double maxPrice) {

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
            sqlBuilder.append(" AND CAST(JSON_UNQUOTE(JSON_EXTRACT(p.price_info,'$.total_price')) AS DECIMAL(12,2)) >= ?");
            params.add(minPrice);
        }

        if (maxPrice != null) {
            sqlBuilder.append(" AND CAST(JSON_UNQUOTE(JSON_EXTRACT(p.price_info,'$.total_price')) AS DECIMAL(12,2)) <= ?");
            params.add(maxPrice);
        }

        sqlBuilder.append(" ORDER BY p.updated_at DESC LIMIT 20");

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sqlBuilder.toString())) {

            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }

            List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    items.add(buildProperty(rs));
                }
            }

            Map<String, Object> response = new HashMap<String, Object>();
            response.put("items", items);
            response.put("count", items.size());
            response.put("message", "查询成功");
            return ResponseEntity.ok(response);

        } catch (SQLException e) {
            Map<String, Object> error = new HashMap<String, Object>();
            error.put("message", "查询失败");
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
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
        item.put("priceInfo", parseJson(rs.getString("price_info")));
        item.put("layoutInfo", parseJson(rs.getString("layout_info")));
        item.put("basicInfo", parseJson(rs.getString("basic_info")));
        item.put("locationInfo", parseJson(rs.getString("location_info")));
        return item;
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
        return DriverManager.getConnection(jdbcUrl, jdbcUsername, jdbcPassword);
    }
}
