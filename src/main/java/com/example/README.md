# 最小原型项目结构

## 项目架构

本项目采用最小原型架构，只保留核心的四个部分：

### 1. controller - 接口实现层
- **作用**：实现RESTful API接口，处理HTTP请求
- **位置**：`src/main/java/com/example/controller/`
- **说明**：直接使用Entity对象，不需要DTO转换

### 2. repository - 数据库通讯层
- **作用**：数据库访问接口，提供CRUD操作
- **位置**：`src/main/java/com/example/repository/`
- **说明**：继承JpaRepository，提供数据库操作

### 3. entity - 模型文件层
- **作用**：数据模型定义，对应数据库表
- **位置**：`src/main/java/com/example/entity/`
- **说明**：JPA实体类，定义数据结构和关系

### 4. service - 算法层
- **作用**：业务逻辑和算法实现
- **位置**：`src/main/java/com/example/service/`
- **说明**：处理业务规则、算法逻辑、数据处理等

## 数据流向

```
Controller (接口) 
    ↓
Service (算法/业务逻辑)
    ↓
Repository (数据库通讯)
    ↓
Entity (模型)
    ↓
Database (数据库)
```
## 开发建议

1. **接口开发**：在controller包中创建Controller类
2. **数据库操作**：在repository包中创建Repository接口
3. **数据模型**：在entity包中创建Entity类
4. **算法实现**：在service包中实现业务逻辑和算法

## 注意事项

- 最小原型直接使用Entity，不需要DTO转换
- 异常处理使用RuntimeException即可
- 配置信息在application.properties中管理
- 保持代码简洁，专注于核心功能实现

