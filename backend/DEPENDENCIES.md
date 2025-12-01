# 项目依赖配置说明

## Java版本
- **Java 8 (1.8)** - 项目使用Java 8开发

## 核心框架版本

### Spring Boot
- **版本**: 2.7.18
- **说明**: Spring Boot 2.7.18是最后一个支持Java 8的稳定版本
- **注意**: Spring Boot 3.x需要Java 17+，本项目使用2.7.18以支持Java 8

### MySQL
- **版本**: 8.0.33
- **驱动**: mysql-connector-java
- **说明**: MySQL 8.x完全支持Java 8

### Hadoop
- **版本**: 3.4.1
- **依赖模块**:
  - hadoop-client
  - hadoop-common
  - hadoop-hdfs
- **说明**: Hadoop 3.4.1完全支持Java 8

### Spark
- **版本**: 3.3.4
- **Scala版本**: 2.12
- **依赖模块**:
  - spark-core_2.12
  - spark-sql_2.12
  - spark-mllib_2.12 (可选，机器学习功能)
- **说明**: Spark 3.3.4是最后一个支持Java 8的稳定版本
- **注意**: Spark 3.4+需要Java 11+，因此使用3.3.4版本

## 版本兼容性说明

| 组件 | 版本 | Java 8支持 | 说明 |
|------|------|-----------|------|
| Spring Boot | 2.7.18 | ✅ | 最后一个支持Java 8的版本 |
| MySQL | 8.0.33 | ✅ | 完全支持 |
| Hadoop | 3.4.1 | ✅ | 完全支持 |
| Spark | 3.3.4 | ✅ | 最后一个支持Java 8的版本 |

## 重要注意事项

1. **Java版本限制**
   - 必须使用Java 8 (JDK 1.8)
   - 不要升级到Java 11或更高版本，否则Spark和Spring Boot版本需要升级

2. **包名差异**
   - Spring Boot 2.x使用 `javax.*` 包
   - Spring Boot 3.x使用 `jakarta.*` 包
   - 本项目使用 `javax.*` 包

3. **依赖冲突处理**
   - 已排除Hadoop和Spark中的slf4j-log4j12和log4j依赖
   - 使用Spring Boot默认的日志框架

4. **CORS配置**
   - Spring Boot 2.x需要在Controller上使用@CrossOrigin注解
   - 或创建WebMvcConfigurer配置类

## 构建和运行

```bash
# 清理并编译
mvn clean compile

# 运行应用
mvn spring-boot:run

# 打包
mvn clean package
```

## 数据库配置

### 开发环境（H2）
- 使用内存数据库H2
- 配置在application.properties中

### 生产环境（MySQL 8）
- 取消注释MySQL配置
- 修改数据库连接信息
- 确保MySQL 8已安装并运行

