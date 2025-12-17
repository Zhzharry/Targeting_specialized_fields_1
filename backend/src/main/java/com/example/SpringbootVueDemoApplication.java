package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;      // 新增
import org.springframework.scheduling.annotation.EnableScheduling; // 新增
/**
 * Spring Boot 应用入口。
 * 负责启动后端服务（端口、组件扫描等均遵循默认配置）。
 */


@SpringBootApplication
@EnableAsync        // 【新增】启用异步执行
@EnableScheduling   // 【新增】启用定时任务
public class SpringbootVueDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootVueDemoApplication.class, args);
    }
}

