# controller 接口实现包

## 文件夹作用

此文件夹用于存放RESTful API控制器，负责处理HTTP请求，调用Service层处理业务逻辑，并返回响应结果。这是最小原型中的接口实现层。

## 对应页面/功能

每个Controller对应一个API接口模块，为前端提供数据接口。

## 代码框架（最小原型）

### 基础Controller示例
```java
package com.example.controller;

import com.example.entity.User;
import com.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5137")  // 允许前端Vue应用（5137端口）访问
public class UserController {
    
    @Autowired
    private UserService userService;
    
    // 获取所有用户
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.findAll());
    }
    
    // 根据ID获取用户
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }
    
    // 创建新用户
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.save(user));
    }
    
    // 更新用户信息
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id, 
            @RequestBody User user) {
        return ResponseEntity.ok(userService.update(id, user));
    }
    
    // 删除用户
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

## 常用注解说明

- `@RestController` - 标识为REST控制器
- `@RequestMapping` - 定义基础路径
- `@GetMapping` - 处理GET请求
- `@PostMapping` - 处理POST请求
- `@PutMapping` - 处理PUT请求
- `@DeleteMapping` - 处理DELETE请求
- `@PathVariable` - 获取路径变量
- `@RequestBody` - 获取请求体数据
- `@CrossOrigin` - 跨域配置

## 注意事项（最小原型）

- Controller层只负责接收请求和返回响应
- 直接使用Entity对象，最小原型不需要DTO
- 业务逻辑在Service层实现
- 统一使用ResponseEntity包装返回结果
- CORS配置：允许前端Vue应用（端口5173）访问后端API（端口5000）
- 后端API地址：http://localhost:5000/api

