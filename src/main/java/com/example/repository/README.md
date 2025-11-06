# repository 数据库通讯包

## 文件夹作用

此文件夹用于存放数据访问接口，继承JpaRepository，提供数据库的CRUD操作和自定义查询方法。这是最小原型中的数据库通讯层。

## 对应页面/功能

Repository层为所有需要数据操作的接口提供数据库访问支持。

## 代码框架

### 基础Repository示例
```java
package com.example.repository;

import com.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // 根据用户名查找用户
    Optional<User> findByUsername(String username);
    
    // 根据邮箱查找用户
    Optional<User> findByEmail(String email);
    
    // 根据用户名和邮箱查找用户
    Optional<User> findByUsernameAndEmail(String username, String email);
    
    // 查找所有启用的用户
    List<User> findByEnabledTrue();
    
    // 根据用户名模糊查询
    List<User> findByUsernameContaining(String keyword);
    
    // 自定义JPQL查询
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findUserByEmail(@Param("email") String email);
    
    // 自定义原生SQL查询
    @Query(value = "SELECT * FROM users WHERE username LIKE %:keyword%", 
           nativeQuery = true)
    List<User> searchUsers(@Param("keyword") String keyword);
    
    // 统计查询
    long countByEnabledTrue();
    
    // 删除操作
    void deleteByUsername(String username);
}
```

## 方法命名规则

Spring Data JPA支持根据方法名自动生成查询：

- `findBy...` - 查找
- `countBy...` - 统计
- `deleteBy...` - 删除
- `existsBy...` - 判断存在
- `...And...` - 条件连接
- `...Or...` - 或条件
- `...Like` - 模糊查询
- `...Containing` - 包含
- `...Between` - 范围查询
- `...GreaterThan` - 大于
- `...LessThan` - 小于

## 复杂查询示例

```java
// 分页查询
Page<User> findByUsernameContaining(String keyword, Pageable pageable);

// 排序查询
List<User> findByEnabledTrueOrderByCreateTimeDesc();

// 多条件查询
List<User> findByUsernameContainingAndEmailContaining(
    String username, String email);
```

## 注意事项

- Repository接口需要继承JpaRepository<Entity, ID>
- 使用@Repository注解标识
- 方法名要符合Spring Data JPA命名规范
- 复杂查询使用@Query注解
- 使用Optional包装可能为null的返回值
- 注意N+1查询问题，合理使用@EntityGraph

