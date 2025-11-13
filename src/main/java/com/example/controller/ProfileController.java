package com.example.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 我的页面（个人中心）接口控制器。
 */
@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "http://localhost:5173")
public class ProfileController {

    /**
     * 获取个人信息。
     *
     * @return 个人信息示例数据
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getProfile() {
        Map<String, Object> profile = new HashMap<String, Object>();
        profile.put("username", "demo_user");
        profile.put("email", "demo@example.com");
        profile.put("avatar", "https://example.com/avatar.png");
        return ResponseEntity.ok(profile);
    }

    /**
     * 更新个人信息（示例接口）。
     *
     * @param payload 更新字段
     * @return 更新结果
     */
    @PutMapping
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody Map<String, Object> payload) {
        // TODO: 在此处接入真实的更新逻辑
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("message", "个人信息已更新（示例数据）");
        result.put("data", payload);
        return ResponseEntity.ok(result);
    }
}
