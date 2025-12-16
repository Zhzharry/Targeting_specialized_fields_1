# 项目结构说明

## 项目概述
本项目是一个专业领域数据分析和房价预测系统，采用前后端分离架构。包含Spring Boot后端服务和Vue 3前端应用。

---

## 📁 根目录结构

```
Targeting_specialized_fields_1/
├── backend/                 # Spring Boot 后端应用
├── frontend/                # Vue 3 前端应用
├── src/                     # 根级别源代码（可能为测试或共享代码）
├── target/                  # Maven 编译输出目录
├── .git/                    # Git 版本控制
├── .idea/                   # IntelliJ IDEA 配置
├── .vscode/                 # VS Code 配置
└── PROJECT_STRUCTURE.md     # 本文档
```

---

## 🔧 后端 (Backend)

### 路径：`/backend`

后端是一个 Spring Boot 应用，提供 REST API 服务。

```
backend/
├── pom.xml                  # Maven 配置文件，定义项目依赖和构建配置
├── mvnw                     # Maven Wrapper 脚本（Linux/Mac）
├── mvnw.cmd                 # Maven Wrapper 脚本（Windows）
│
├── src/main/
│   ├── java/
│   │   └── com/example/
│   │       ├── SpringbootVueDemoApplication.java   # 应用主入口类
│   │       │
│   │       ├── config/
│   │       │   └── SparkConfig.java               # Spark 配置类
│   │       │
│   │       ├── controller/                         # Web 控制层
│   │       │   ├── HomeController.java            # 主页控制器
│   │       │   ├── LoginController.java           # 登录认证控制器
│   │       │   ├── ProfileController.java         # 用户资料控制器
│   │       │   ├── QueryController.java           # 数据查询控制器
│   │       │   └── README.md                      # 控制器说明文档
│   │       │
│   │       ├── service/                            # 业务逻辑层
│   │       │   ├── PropertySimilarityService.java       # 房产相似度计算服务
│   │       │   ├── UserSimilarityService.java           # 用户相似度计算服务
│   │       │   │
│   │       │   └── predict_zhz/                         # 房价预测服务模块
│   │       │       ├── HousePricePredictionService.java # 房价预测核心服务
│   │       │       ├── predict_price.py                 # Python 预测脚本
│   │       │       ├── README.md                        # 预测模块说明
│   │       │       ├── beijing/
│   │       │       │   └── model_config.json           # 北京模型配置
│   │       │       ├── shanghai/
│   │       │       │   └── model_config.json           # 上海模型配置
│   │       │       └── tianjin/
│   │       │           └── model_config.json           # 天津模型配置
│   │       │
│   │       └── README.md                          # 代码结构说明文档
│   │
│   ├── resources/                                  # 资源文件
│   │   ├── application.properties                 # Spring Boot 配置文件
│   │   └── static/                                # 静态文件目录
│   │       └── README.md                          # 静态文件说明
│   │
│   └── 文档/
│       └── 接口文档.md                            # API 接口文档
│
├── src/test/
│   └── java/                                      # 测试源代码
│
├── target/                                        # Maven 编译输出目录
│   ├── classes/                                   # 编译后的类文件
│   ├── springboot-vue-demo-1.0.0.jar.original   # 原始 JAR 包
│   ├── generated-sources/                         # 生成的源代码
│   ├── generated-test-sources/                    # 生成的测试源代码
│   ├── maven-archiver/                            # Maven 归档信息
│   └── maven-status/                              # Maven 编译状态
│
├── SQL/
│   ├── 备份.sql                                   # 数据库备份脚本
│   └── 注入.sql                                   # 数据库初始化脚本
│
├── DEPENDENCIES.md                                # 依赖管理说明文档
└── README.md                                      # 后端项目说明
```

### 核心模块说明：

#### **Controller（控制层）**
- `HomeController`: 处理主页请求
- `LoginController`: 用户登录认证
- `ProfileController`: 用户资料管理
- `QueryController`: 数据查询接口

#### **Service（业务逻辑层）**
- `PropertySimilarityService`: 基于属性的房产相似度推荐算法
- `UserSimilarityService`: 基于用户的相似度计算
- `HousePricePredictionService`: 房价预测服务
  - 支持多个城市（北京、上海、天津）
  - 集成 Python 预测模型（`predict_price.py`）
  - 每个城市有单独的模型配置文件

#### **Config（配置层）**
- `SparkConfig`: Apache Spark 配置，用于大数据处理

---

## 🎨 前端 (Frontend)

### 路径：`/frontend`

前端是一个 Vue 3 + TypeScript + Vite 应用。

```
frontend/
├── package.json             # Node.js 依赖配置
├── pnpm-lock.yaml          # 依赖锁定文件（如果使用 pnpm）
├── vite.config.ts           # Vite 构建工具配置
├── vitest.config.ts         # Vitest 测试框架配置
├── playwright.config.ts     # Playwright E2E 测试配置
├── eslint.config.ts         # ESLint 代码风格配置
│
├── tsconfig.json            # TypeScript 总体配置
├── tsconfig.app.json        # 应用级 TypeScript 配置
├── tsconfig.node.json       # Node.js 相关 TypeScript 配置
├── tsconfig.vitest.json     # Vitest TypeScript 配置
│
├── index.html               # 应用入口 HTML
├── env.d.ts                 # 环境类型定义
│
├── src/                     # 源代码目录
│   ├── main.ts              # 应用主入口
│   ├── App.vue              # 根组件
│   │
│   ├── views/               # 页面组件
│   │   ├── LoginPage.vue        # 登录页面
│   │   ├── ProfilePage.vue      # 个人资料页面
│   │   ├── SearchPage.vue       # 搜索页面
│   │   └── ToolsPage.vue        # 工具页面
│   │
│   ├── components/          # 可复用组件
│   │   ├── PreferenceSettings.vue    # 偏好设置组件
│   │   └── Common/
│   │       └── PropertyCard.vue      # 房产卡片组件
│   │
│   ├── router/              # 路由配置
│   │   └── index.ts         # 路由定义
│   │
│   ├── stores/              # 状态管理 (Pinia)
│   │   └── counter.ts       # 计数器 store（示例）
│   │
│   ├── __tests__/           # 单元测试
│   │   └── App.spec.ts      # App 组件测试
│   │
│   └── assets/              # 静态资源
│       └── image/           # 图片文件
│
├── public/                  # 公共静态资源
│
├── e2e/                     # E2E 端到端测试
│   ├── tsconfig.json        # E2E 测试 TypeScript 配置
│   └── vue.spec.ts          # Vue 应用 E2E 测试
│
├── README.md                # 前端项目说明
```

### 核心模块说明：

#### **Views（页面）**
- `LoginPage`: 用户登录界面
- `ProfilePage`: 用户个人资料管理
- `SearchPage`: 房产搜索和推荐
- `ToolsPage`: 分析工具页面

#### **Components（组件）**
- `PreferenceSettings`: 用户偏好设置
- `PropertyCard`: 房产信息卡片展示

#### **Router（路由）**
- 定义应用的所有路由规则和导航

#### **Stores（状态管理）**
- 使用 Pinia 进行全局状态管理

---

## 🗄️ 其他目录

### `/src`
根级别源代码目录，可能包含共享代码或测试代码。

### `/target`
Maven 编译输出目录，包含：
- 编译后的 Java 类文件
- 生成的 JAR 包
- 编译状态和元数据

---

## 📋 数据库文件

位置：`/backend`

- **备份.sql**: 数据库备份脚本
- **注入.sql**: 数据库初始化和数据注入脚本

---

## 🔗 项目关键特性

| 特性 | 说明 |
|------|------|
| **房价预测** | 基于历史数据的机器学习模型，支持北京、上海、天津多城市预测 |
| **推荐系统** | 房产相似度匹配和用户相似度计算 |
| **用户管理** | 登录认证和个人资料管理 |
| **查询接口** | RESTful API 数据查询接口 |
| **大数据处理** | 集成 Apache Spark 进行大规模数据处理 |
| **前后端分离** | 独立的 Spring Boot 后端和 Vue 前端应用 |

---

## 🛠️ 技术栈

### 后端
- **Java**: Spring Boot 框架
- **Python**: 房价预测模型
- **Apache Spark**: 大数据处理
- **Maven**: 项目构建

### 前端
- **Vue 3**: 前端框架
- **TypeScript**: 类型安全的 JavaScript
- **Vite**: 现代前端构建工具
- **Pinia**: 状态管理
- **Vitest**: 单元测试框架
- **Playwright**: E2E 测试框架
- **ESLint**: 代码风格检查

---

## 📖 相关文档

- **后端文档**：
  - `backend/README.md` - 后端项目说明
  - `backend/src/main/java/com/example/README.md` - 代码结构说明
  - `backend/src/main/java/com/example/controller/README.md` - 控制器说明
  - `backend/src/main/java/com/example/service/predict_zhz/README.md` - 预测模块说明
  - `backend/src/main/文档/接口文档.md` - API 接口文档
  - `backend/DEPENDENCIES.md` - 依赖管理说明

- **前端文档**：
  - `frontend/README.md` - 前端项目说明

---

## 🚀 快速开始

### 后端启动
```bash
cd backend
./mvnw spring-boot:run        # Linux/Mac
# 或
mvnw.cmd spring-boot:run      # Windows
```

### 前端启动
```bash
cd frontend
npm install                    # 安装依赖
npm run dev                    # 启动开发服务器
```

---

## 📝 注意事项

1. 确保已安装 Java 8+ 和 Node.js 14+
2. 数据库需要提前初始化，运行 `backend/注入.sql` 脚本
3. Python 预测模型依赖需要额外配置
4. 前后端服务需要分别启动

---

**最后更新**: 2025年12月16日
