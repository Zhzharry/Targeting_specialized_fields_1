# 用户相似度测试数据生成工具

## 概述

`generate_extreme_user_preferences.py` 是一个用于生成极端用户偏好数据的Python脚本，主要用于测试用户相似度计算功能。

## 功能

该脚本会生成两类极端数据：

### 1. 极端相似的用户（高相似度测试）
- **用户ID范围**: 100-199
- **特点**:
  - 相同的预算范围（400-600万）
  - 相同的家庭结构（couple）
  - 完全相同的偏好地区（南山区、福田区）
  - 收藏大量相同的房源（15个共同收藏 + 少量个人偏好）

### 2. 极端不相似的用户（低相似度测试）
- **用户ID范围**: 200+
- **特点**:
  - 完全不同的预算范围（高预算 vs 低预算）
  - 不同的家庭结构（single, couple, family, extended_family）
  - 完全不重叠的偏好地区
  - 收藏完全不同的房源（无重叠）

## 环境要求

- Python 3.6+
- pymysql 库

## 安装依赖

```bash
pip install pymysql
```

## 使用方法

### 1. 配置数据库连接

脚本默认使用以下配置（在 `application.properties` 中定义）：
- 主机: `localhost`
- 端口: `3306`
- 数据库: `bigdata`
- 用户名: `bigdata_user`
- 密码: `123456`

如需修改，请编辑脚本中的 `DB_CONFIG` 字典。

### 2. 运行脚本

```bash
cd backend/src/main/java/com/example/service/user_similarity
python generate_extreme_user_preferences.py
```

或者在项目根目录：

```bash
python backend/src/main/java/com/example/service/user_similarity/generate_extreme_user_preferences.py
```

### 3. 验证结果

脚本运行后会：
1. 创建用户数据（插入到 `users` 表）
2. 创建收藏数据（插入到 `favorites` 表）
3. 显示验证信息

## 生成的数据结构

### 用户偏好（user_profile JSON）

```json
{
  "budget": {
    "min": 400,
    "max": 600
  },
  "family_structure": "couple",
  "preferred_locations": ["南山区", "福田区"]
}
```

### 收藏数据

- **相似用户**: 收藏15个共同房源 + 1-3个个人偏好房源
- **不相似用户**: 每个用户收藏10个不同的房源（无重叠）

## 测试用户相似度计算

生成数据后，可以通过以下方式测试：

### 1. 手动触发完整计算

```bash
curl -X POST http://localhost:5000/api/similarity/user/calculate/spark
```

### 2. 手动触发增量计算（针对特定用户）

```bash
curl -X POST http://localhost:5000/api/similarity/user/100/calculate/spark/incremental
```

### 3. 查看结果

```sql
-- 查看相似用户之间的相似度（应该很高）
SELECT * FROM user_similarity 
WHERE (user_id1 = 100 AND user_id2 IN (101, 102, 103, 104))
   OR (user_id2 = 100 AND user_id1 IN (101, 102, 103, 104))
ORDER BY similarity_score DESC;

-- 查看不相似用户之间的相似度（应该很低或为0）
SELECT * FROM user_similarity 
WHERE (user_id1 = 200 AND user_id2 IN (201, 202, 203, 204))
   OR (user_id2 = 200 AND user_id1 IN (201, 202, 203, 204))
ORDER BY similarity_score DESC;
```

## 预期结果

### 相似用户（100-199）
- **相似度分数**: 应该 > 0.5（高相似度）
- **共同收藏**: 15个房源
- **Jaccard相似度**: 应该接近 0.8-0.9

### 不相似用户（200+）
- **相似度分数**: 应该 < 0.1 或为 0（低相似度）
- **共同收藏**: 0个房源
- **Jaccard相似度**: 应该为 0

## 注意事项

1. **用户ID冲突**: 如果ID 100-299已被使用，请修改脚本中的 `SIMILAR_USERS_START_ID` 和 `DISSIMILAR_USERS_START_ID`
2. **房源数量**: 确保数据库中有足够的房源（至少50个），否则收藏数据可能不完整
3. **数据清理**: 如需重新生成，请先删除相关用户和收藏数据：

```sql
-- 删除测试用户
DELETE FROM users WHERE user_id >= 100 AND user_id < 300;

-- 删除测试收藏
DELETE FROM favorites WHERE user_id >= 100 AND user_id < 300;

-- 删除测试相似度数据
DELETE FROM user_similarity WHERE user_id1 >= 100 OR user_id2 >= 100;
```

## 故障排除

### 问题1: 连接数据库失败
- 检查数据库是否运行
- 验证数据库配置是否正确
- 确认用户权限是否足够

### 问题2: 没有可用房源
- 确保 `properties` 表中有数据
- 可以手动插入一些测试房源

### 问题3: 外键约束错误
- 确保用户ID在 `users` 表中存在
- 确保房源ID在 `properties` 表中存在

## 扩展

如需生成更多类型的测试数据，可以修改脚本：
- 调整用户数量
- 修改预算范围
- 添加更多地区偏好
- 创建中等相似度的用户（用于测试边界情况）

