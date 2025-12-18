# Docker 部署指南

本项目支持使用 Docker 在本地运行完整的前后端应用。

## 前置要求

- Docker Desktop
- Docker Compose

## 快速启动

1. 克隆项目到本地
2. 进入项目根目录
3. 运行以下命令：

```bash
# 构建并启动所有服务
docker-compose up --build

# 或者后台运行
docker-compose up -d --build
```

## 服务说明

- **frontend**: Vue.js 前端应用 (http://localhost:5173)
- **backend**: Spring Boot 后端 API (http://localhost:8080)
- **mysql**: MySQL 数据库 (localhost:3306)

## 访问应用

- 前端界面: http://localhost:5173
- 后端 API: http://localhost:8080
- 数据库: localhost:3306 (用户名: root, 密码: password)

## 开发模式

前端使用 Vite 开发服务器，支持热重载。修改前端代码后会自动重新编译。

## 数据初始化

MySQL 容器启动时会自动执行以下 SQL 文件：

- `backend/插入热力图测试数据.sql`
- `backend/插入密集热力图数据_正确版.sql`

## 停止服务

```bash
# 停止所有服务
docker-compose down

# 停止并删除数据卷
docker-compose down -v
```

## 单独构建服务

```bash
# 构建前端
docker-compose build frontend

# 构建后端
docker-compose build backend

# 构建数据库
docker-compose build mysql
```

## 故障排除

1. **端口冲突**: 确保本地端口 5173、8080、3306 未被占用
2. **构建失败**: 检查 Docker Desktop 是否正常运行
3. **数据库连接失败**: 等待 MySQL 容器完全启动（可能需要几分钟）
4. **前端无法访问后端**: 检查 CORS 配置和网络连接

## 环境变量

可以在`.env`文件中自定义配置：

```env
MYSQL_ROOT_PASSWORD=your_password
MYSQL_DATABASE=your_db_name
MYSQL_USER=your_user
MYSQL_PASSWORD=your_password
```

## 日志查看

```bash
# 查看所有服务日志
docker-compose logs

# 查看特定服务日志
docker-compose logs frontend
docker-compose logs backend
docker-compose logs mysql

# 实时查看日志
docker-compose logs -f
```
