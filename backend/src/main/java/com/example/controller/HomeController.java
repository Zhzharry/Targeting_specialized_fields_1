package com.example.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 主页对应的接口控制器。
 * 提供首页展示所需的基础数据。
 */
@RestController
@RequestMapping("/api/home")
@CrossOrigin(origins = "http://localhost:5173")
public class HomeController {

    /**
     * 获取首页概览数据。
     *
     * @return 首页概览信息
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        Map<String, Object> overview = new HashMap<String, Object>();
        overview.put("title", "首页概览");
        overview.put("welcomeMessage", "欢迎访问首页（示例数据）");
        overview.put("notifications", 0);
        return ResponseEntity.ok(overview);
    }
}
