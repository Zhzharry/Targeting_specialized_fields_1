package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 应用入口。
 * 负责启动后端服务（端口、组件扫描等均遵循默认配置）。
 */
@SpringBootApplication
public class SpringbootVueDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootVueDemoApplication.class, args);
    }
}

