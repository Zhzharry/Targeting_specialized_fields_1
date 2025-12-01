package com.example.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 登录页面对应的接口控制器。
 * 提供登录与注册接口，并在本类中直接执行数据库操作以保持低耦合。
 */
@RestController
@RequestMapping("/api/login")
@CrossOrigin(origins = "http://localhost:5173")
public class LoginController {

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

    public LoginController(@Value("${spring.datasource.url}") String jdbcUrl,
                           @Value("${spring.datasource.username}") String jdbcUsername,
                           @Value("${spring.datasource.password}") String jdbcPassword) {
        this.jdbcUrl = jdbcUrl;
        this.jdbcUsername = jdbcUsername;
        this.jdbcPassword = jdbcPassword;
    }

    /**
     * 登录接口：验证用户名和密码。
     *
     * @param credentials 前端传递的登录凭据（username、password）
     * @return 登录结果
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, Object> credentials) {
        String username = asTrimmedString(credentials.get("username"));
        String password = asTrimmedString(credentials.get("password"));

        if (username == null || password == null) {
            return badRequest("用户名和密码不能为空");
        }

        String sql = "SELECT user_id, password, user_profile FROM users WHERE username = ?";
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return unauthorized("用户不存在");
                }

                String storedPassword = resultSet.getString("password");
                if (!Objects.equals(storedPassword, password)) {
                    return unauthorized("用户名或密码错误");
                }

                Map<String, Object> data = new HashMap<String, Object>();
                data.put("userId", resultSet.getLong("user_id"));
                data.put("username", username);
                data.put("userProfile", resultSet.getString("user_profile"));
                data.put("message", "登录成功");

                return ResponseEntity.ok(data);
            }
        } catch (SQLException e) {
            return serverError("登录失败", e);
        }
    }

    /**
     * 注册接口：创建新用户。
     *
     * @param payload 前端传递的注册信息（username、password、user_profile等）
     * @return 注册结果
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, Object> payload) {
        String username = asTrimmedString(payload.get("username"));
        String password = asTrimmedString(payload.get("password"));

        if (username == null || password == null) {
            return badRequest("用户名和密码不能为空");
        }

        String checkSql = "SELECT COUNT(1) FROM users WHERE username = ?";
        String insertSql = "INSERT INTO users (username, password, user_profile) VALUES (?, ?, ?)";

        try (Connection connection = getConnection()) {
            // 检查用户名是否已存在
            try (PreparedStatement checkStatement = connection.prepareStatement(checkSql)) {
                checkStatement.setString(1, username);
                try (ResultSet resultSet = checkStatement.executeQuery()) {
                    if (resultSet.next() && resultSet.getInt(1) > 0) {
                        return conflict("用户名已存在");
                    }
                }
            }

            String profileJson = extractProfileJson(payload.get("user_profile"));

            try (PreparedStatement insertStatement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertStatement.setString(1, username);
                insertStatement.setString(2, password);
                insertStatement.setString(3, profileJson);
                insertStatement.executeUpdate();

                Long userId = null;
                try (ResultSet keys = insertStatement.getGeneratedKeys()) {
                    if (keys.next()) {
                        userId = keys.getLong(1);
                    }
                }

                Map<String, Object> response = new HashMap<String, Object>();
                response.put("userId", userId);
                response.put("username", username);
                response.put("message", "注册成功");
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            }
        } catch (SQLException | JsonProcessingException e) {
            return serverError("注册失败", e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, jdbcUsername, jdbcPassword);
    }

    private String asTrimmedString(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private String extractProfileJson(Object profile) throws JsonProcessingException {
        if (profile == null) {
            return null;
        }
        if (profile instanceof String) {
            String text = ((String) profile).trim();
            return text.isEmpty() ? null : text;
        }
        return objectMapper.writeValueAsString(profile);
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseEntity<Map<String, Object>> unauthorized(String message) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, message);
    }

    private ResponseEntity<Map<String, Object>> conflict(String message) {
        return buildErrorResponse(HttpStatus.CONFLICT, message);
    }

    private ResponseEntity<Map<String, Object>> serverError(String message, Exception e) {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("message", message);
        body.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
