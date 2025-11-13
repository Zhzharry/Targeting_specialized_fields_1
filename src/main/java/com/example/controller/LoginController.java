package com.example.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * 登录页面对应的接口控制器。
 * 提供最小原型所需的登录接口。
 */
@RestController
@RequestMapping("/api/login")
@CrossOrigin(origins = "http://localhost:5173")
public class LoginController {

    /**
     * 模拟登录接口。
     *
     * @param credentials 前端传递的登录凭据（用户名、密码等）
     * @return 登录结果
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, Object> credentials) {
        // TODO: 在此处接入真实的认证逻辑
        Map<String, Object> response = Collections.singletonMap("message", "登录成功（示例数据）");
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
