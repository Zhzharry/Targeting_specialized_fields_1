
# 项目结构说明（完整版）

## 项目概述
本项目是一个专业领域的数据分析与房价预测系统，采用前后端分离架构。后端为 Spring Boot 应用，集成离线大数据处理能力（Spark/Hadoop），前端为 Vue 3 + TypeScript 应用。

---

## 📁 根目录结构

```
Targeting_specialized_fields_1/
├── backend/                 # Spring Boot 后端应用（主服务）
├── frontend/                # Vue 3 前端应用
├── src/                     # 根级别源代码或共享/测试代码
├── target/                  # Maven 编译输出目录
├── .git/                    # Git 版本控制
├── .vscode/                 # VS Code 配置
└── PROJECT_STRUCTURE.md     # 本文档（当前文件）
```

---

## 🔧 后端（backend）

路径：`/backend`

后端为 Spring Boot 应用，负责对外提供 REST API，同时调度/触发离线批处理（Spark）作业，支持 HDFS 数据交互与 MySQL 持久化。

```
backend/
├── pom.xml                  # Maven 依赖与构建配置（包含 Spark/Hadoop/MySQL 等）
├── mvnw / mvnw.cmd          # Maven Wrapper（跨平台）
│
├── src/main/
│   ├── java/
│   │   └── com/example/
│   │       ├── SpringbootVueDemoApplication.java   # 应用主入口
│   │       │
│   │       ├── batch/                              # 批处理模块（Spark Job）
│   │       │   ├── PropertySimilarityBatchJob.java # 房源相似度计算 Spark 作业
│   │       │   ├── UserSimilarityBatchJob.java     # 用户相似度计算 Spark 作业
│   │       │   └── README.md                       # 批处理说明（输入/输出/运行频率）
│   │       │
│   │       ├── config/                             # 配置类（读取 application.properties）
│   │       │   ├── SparkConfig.java                # Spark 连接/模式配置（local/yarn）
│   │       │   ├── HadoopConfig.java               # HDFS/Hadoop 配置
│   │       │   ├── DatabaseConfig.java             # 数据源与连接池配置
│   │       │   └── CacheConfig.java                # 缓存（Redis）配置
│   │       │
│   │       ├── controller/                         # 控制层（对外 API）
│   │       │   ├── HomeController.java             # 首页/推荐相关接口（读取预计算表）
│   │       │   ├── LoginController.java            # 登录认证
│   │       │   ├── ProfileController.java          # 用户资料管理
│   │       │   ├── QueryController.java            # 数据查询接口
│   │       │   ├── OthersAlsoViewedController.java # 其他用户也在看
│   │       │   └── README.md                       # 控制器说明
│   │       │
│   │       ├── service/                            # 业务逻辑层（核心服务）
│   │       │   ├── PropertySimilarityService.java  # 相似度服务（调度/降级逻辑）
│   │       │   ├── UserSimilarityService.java      # 用户相似度服务（查询/缓存）
│   │       │   └── predict_zhz/                    # 房价预测模块（Python 集成）
│   │       │       ├── HousePricePredictionService.java
│   │       │       ├── predict_price.py            # Python 模型脚本（供离线/Batch 使用）
│   │       │       └── README.md
│   │       │
│   │       ├── util/                               # 通用工具
│   │       │   ├── HdfsUtil.java                   # HDFS 操作（上传/下载/读写 Parquet/CSV）
│   │       │   ├── SparkJobLauncher.java           # 提交/本地运行 Spark Job 的工具
│   │       │   └── README.md
│   │       │
│   │       ├── hadoop/                             # Hadoop/MapReduce（可选实现）
│   │       │   ├── mapreduce/                       # MapReduce 示例实现
│   │       │   │   ├── UserBehaviorMap.java
│   │       │   │   ├── UserBehaviorReduce.java
│   │       │   │   └── README.md
│   │       │   ├── writable/                        # 自定义 Writable
│   │       │   │   └── UserBehaviorWritable.java
│   │       │   └── README.md
│   │       │
│   │       ├── repository/                         # DAO 层（JPA / MyBatis）
│   │       │   ├── PropertyRepository.java         # 房源数据访问
│   │       │   ├── UserRepository.java             # 用户数据访问
│   │       │   └── README.md
│   │       │
│   │       ├── dto/                                # DTO / VO 抽象
│   │       │   ├── PropertyDTO.java
│   │       │   ├── UserDTO.java
│   │       │   └── README.md
│   │       │
│   │       ├── exception/                          # 统一异常处理
│   │       │   ├── GlobalExceptionHandler.java
│   │       │   └── ApiError.java
│   │       │
│   │       └── README.md                           # 模块总览
│   │
│   ├── resources/                                  # 资源与配置文件
│   │   ├── application.properties                  # 应用配置（DB/HDFS/Spark/调度等）
│   │   ├── log4j2.xml                               # 日志配置
│   │   ├── static/                                  # 静态文件（供后端直接提供）
│   │   └── mapper/                                  # MyBatis XML（如使用）
│   │
│   └── 文档/                                       # 项目内部文档
│       ├── 接口文档.md                              # REST API 文档（示例/请求/返回）
│       └── 运维部署.md                              # 部署/环境/监控/备份说明
│
├── src/test/                                       # 单元/集成测试
├── target/                                         # Maven 输出
├── SQL/                                            # DB 脚本
│   ├── 备份.sql
│   └── 注入.sql
├── DEPENDENCIES.md                                 # 依赖列表与版本说明
├── HADOOP_OPTIMIZATION_GUIDE.md                    # Hadoop/Spark 优化与部署指南
└── README.md                                       # 后端整体说明
```

### 核心模块说明（简要）

- Controller（控制层）
  - `HomeController`：主页与推荐接口，优先读取 `property_similarity` 预计算表，若无则启用降级的实时计算。
  - `LoginController` / `ProfileController`：认证与用户资料管理。
  - `QueryController`：通用数据查询接口。

- Batch（批处理）
  - `PropertySimilarityBatchJob`：使用 Spark 计算房源相似度，支持分层粗排/精排、地理距离、时间衰减等优化策略；输出写入 MySQL 表 `property_similarity` 并在 HDFS 存储备份。
  - `UserSimilarityBatchJob`：基于用户行为矩阵计算用户相似度，写入 `user_similarity` 表。

- Service（业务层）
  - `PropertySimilarityService`：作为调度入口，触发/管理批处理任务，提供查询与缓存封装，保留小规模实时计算作为降级方案。
  - `HousePricePredictionService`：封装 Python 模型调用（可通过 subprocess 或 RPC 调用模型服务）。

- Util（工具）
  - `HdfsUtil`：HDFS 文件上传/下载/读写（Parquet/CSV）
  - `SparkJobLauncher`：本地或 YARN 上提交 Spark 作业的封装。能生成 spark-submit 命令或使用 SparkLauncher API。

---

## 🎨 前端（frontend）

路径：`/frontend`（Vue 3 + TypeScript + Vite）

```
frontend/
├── package.json
├── vite.config.ts           # 含 dev proxy 配置（将 `/api` 转发到后端）
├── src/
│   ├── main.ts
│   ├── App.vue
│   ├── api/                 # 后端请求封装（Axios 实例 + 各接口）
│   │   ├── index.ts         # Axios 实例与拦截器
│   │   ├── auth.api.ts
│   │   └── query.api.ts
│   ├── stores/              # Pinia 状态管理
│   ├── components/
│   └── views/
└── README.md
```

前端要点：
- `vite.config.ts` 推荐设置 dev server proxy：将 `/api` 请求代理到后端，避免 CORS 干扰。
- 使用 Pinia 管理用户、搜索与偏好状态。

---

## 🗄️ 其他目录

- `/src`：根级别通用或示例代码（非核心后端/前端）
- `/target`：Maven 编译输出（JAR、classes、generated-sources 等）

---

## 📋 数据库脚本

位置：`/backend/SQL`
- `备份.sql`：数据库导出/备份脚本
- `注入.sql`：初始化数据与表结构脚本

---

## 🔗 项目关键特性

- 房价预测：历史数据驱动的 ML 模型（多城市）
- 推荐系统：房源相似度 + 用户相似度，支持离线批处理与在线降级
- 大数据：集成 Spark（主要）和 Hadoop（可选 MapReduce）用于离线计算

---

## 🛠️ 技术栈

- 后端：Java (Spring Boot), Maven, Spark, Hadoop, MySQL
- 前端：Vue 3, TypeScript, Vite, Pinia
- 其他：Python（预测模型）、Redis（缓存）、Docker（可选部署）

---

## 🚀 快速开始（开发）

后端（Windows）：
```powershell
cd backend
mvnw.cmd spring-boot:run
```

前端：
```bash
cd frontend
npm install
npm run dev
```

若需要在本地运行 Spark 批处理，可参考 `backend/batch/README.md` 使用 `local[*]` 模式模拟集群。

---

## 注意事项

1. 推荐使用 Java 8 作为后端运行时以保证兼容性；Spark 版本需与依赖兼容。
2. 确认 `application.properties` 中的 HDFS 与 MySQL 配置。
3. 若使用 MapReduce 模块，请确保 Hadoop 集群配置正确或在伪分布模式下测试。

---

**最后更新**: 2025年12月17日
