# entity 模型文件包

## 文件夹作用

此文件夹用于存放JPA实体类，对应数据库表的映射对象。实体类定义了数据模型的结构和关系。这是最小原型中的模型文件层。

## 对应页面/功能

实体类对应数据库表，每个实体类对应一个业务领域的数据模型。

## 代码框架

### 基础Entity示例
```java
package com.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;
    
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;
    
    @Column(name = "password", nullable = false)
    private String password;
    
    @Column(name = "phone", length = 20)
    private String phone;
    
    @Column(name = "address", length = 200)
    private String address;
    
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
    
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
    
    // 关联关系示例
    // @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    // private List<Order> orders;
}
```

## 常用JPA注解说明

- `@Entity` - 标识为JPA实体类
- `@Table` - 指定数据库表名
- `@Id` - 主键标识
- `@GeneratedValue` - 主键生成策略
- `@Column` - 列映射配置
- `@OneToMany` - 一对多关系
- `@ManyToOne` - 多对一关系
- `@ManyToMany` - 多对多关系
- `@OneToOne` - 一对一关系
- `@CreationTimestamp` - 自动设置创建时间
- `@UpdateTimestamp` - 自动更新修改时间

## 关系映射示例

### 一对多关系
```java
@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
private List<Order> orders;
```

### 多对一关系
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id")
private User user;
```

## 注意事项

- 实体类应该使用Lombok的@Data注解
- 避免在实体类中包含业务逻辑
- 使用合适的关联关系注解
- 注意懒加载和急加载的选择
- 合理使用级联操作（CascadeType）
- 时间字段使用LocalDateTime类型

