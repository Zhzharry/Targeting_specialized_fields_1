package com.example.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 查询页面接口控制器。
 */
@RestController
@RequestMapping("/api/query")
@CrossOrigin(origins = "http://localhost:5173")
public class QueryController {

    /**
     * 根据关键字执行示例查询。
     *
     * @param keyword 查询关键字
     * @return 查询结果示例数据
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> search(@RequestParam(value = "keyword", required = false) String keyword) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("keyword", keyword);
        result.put("items", new String[]{"示例结果1", "示例结果2"});
        result.put("message", "查询成功（示例数据）");
        return ResponseEntity.ok(result);
    }
}
