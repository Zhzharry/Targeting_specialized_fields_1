# 房源相似度测试数据生成工具

## 概述

`generate_extreme_property_data.py` 是一个用于生成极端房源和小区数据的Python脚本，主要用于测试房源相似度计算功能。

## 功能

该脚本会生成两类极端数据：

### 1. 极端相似的房源和小区（高相似度测试）
- **小区ID范围**: 1000-1002
- **房源ID范围**: 10000-19999
- **特点**:
  - 相同的位置区域（南山区）
  - 相同的地理坐标范围（非常接近）
  - 相同的设施配置（物业费、绿化率、容积率等）
  - 相同的价格范围（400-600万）
  - 相同的面积范围（90-120平米）
  - 相同的户型（3室2厅2卫）
  - 相同的朝向（南向）
  - 相同的装修（精装）

### 2. 极端不相似的房源和小区（低相似度测试）
- **小区ID范围**: 1010-1014
- **房源ID范围**: 20000+
- **特点**:
  - 完全不同的位置区域（南山区、龙岗区、宝安区、盐田区、光明区）
  - 完全不同的地理坐标
  - 完全不同的设施配置
  - 完全不同的价格范围（200-1000万）
  - 完全不同的面积范围（60-200平米）
  - 不同的户型（1室到5室）
  - 不同的朝向（南、北、东、西等）
  - 不同的装修（精装、简装、毛坯、豪华装修）

## 环境要求

- Python 3.6+
- pymysql 库

## 安装依赖

```bash
pip install pymysql
```

## 使用方法

### 1. 配置数据库连接

脚本默认使用以下配置：
- 主机: `127.0.0.1`
- 端口: `3306`
- 数据库: `bigdata`
- 用户名: `root`
- 密码: `123456`

如需修改，请编辑脚本中的 `DB_CONFIG` 字典。

### 2. 运行脚本

```bash
cd 验证数据的好坏/房源相似度
python generate_extreme_property_data.py
```

## 生成的数据结构

### 小区数据（communities表）

```json
{
  "location_info": {
    "district": "南山区",
    "city": "深圳市",
    "province": "广东省",
    "longitude": 113.9344,
    "latitude": 22.5329,
    "address": "南山区科技园1000号"
  },
  "facility_info": {
    "management_fee": 3.5,
    "green_ratio": 0.35,
    "plot_ratio": 2.5,
    "parking_spaces": 500,
    "has_gym": true,
    "has_swimming_pool": true,
    "has_kindergarten": true,
    "has_supermarket": true
  }
}
```

### 房源数据（properties表）

```json
{
  "basic_info": {
    "build_year": 2018,
    "decoration": "精装",
    "property_type": "住宅"
  },
  "price_info": {
    "total_price": 500.0,
    "unit_price": 45000.0
  },
  "layout_info": {
    "area": 105.0,
    "bedroom_count": 3,
    "living_room_count": 2,
    "bathroom_count": 2,
    "floor": 15,
    "total_floors": 28,
    "orientation": "south"
  }
}
```

## 测试房源相似度计算

生成数据后，可以通过以下方式测试：

### 1. 手动触发完整计算

```bash
curl -X POST http://localhost:5000/api/similarity/property/calculate/full
```

### 2. 手动触发协同过滤计算

```bash
curl -X POST http://localhost:5000/api/similarity/property/calculate/cf
```

### 3. 查看结果

```sql
-- 查看相似房源之间的相似度（应该很高）
SELECT * FROM property_similarity 
WHERE property_id1 >= 10000 AND property_id1 < 20000
  AND property_id2 >= 10000 AND property_id2 < 20000
ORDER BY JSON_EXTRACT(similarity_data, '$.similarity_score') DESC
LIMIT 20;

-- 查看不相似房源之间的相似度（应该很低）
SELECT * FROM property_similarity 
WHERE property_id1 >= 20000 AND property_id2 >= 20000
ORDER BY JSON_EXTRACT(similarity_data, '$.similarity_score') ASC
LIMIT 20;
```

## 预期结果

### 相似房源（10000-19999）
- **相似度分数**: 应该 > 0.7（高相似度）
- **特征相似**: 价格、面积、户型、朝向、位置等高度相似
- **余弦相似度**: 应该接近 0.8-0.9

### 不相似房源（20000+）
- **相似度分数**: 应该 < 0.3 或为 0（低相似度）
- **特征差异**: 价格、面积、户型、朝向、位置等完全不同
- **余弦相似度**: 应该 < 0.3

## 注意事项

1. **ID冲突**: 如果ID 1000-1020或10000-30000已被使用，请修改脚本中的ID范围
2. **数据清理**: 如需重新生成，请先删除相关数据：

```sql
-- 删除测试房源
DELETE FROM properties WHERE property_id >= 10000;

-- 删除测试小区
DELETE FROM communities WHERE community_id >= 1000;

-- 删除测试相似度数据
DELETE FROM property_similarity WHERE property_id1 >= 10000 OR property_id2 >= 10000;
```

3. **外键约束**: 删除房源前需要先删除相关的收藏、浏览历史等数据

## 故障排除

### 问题1: 连接数据库失败
- 检查数据库是否运行
- 验证数据库配置是否正确
- 确认用户权限是否足够

### 问题2: 外键约束错误
- 确保小区ID在 `communities` 表中存在
- 确保删除顺序正确（先删除依赖数据）

### 问题3: JSON格式错误
- 确保JSON字符串格式正确
- 检查中文字符编码（使用utf8mb4）

## 扩展

如需生成更多类型的测试数据，可以修改脚本：
- 调整房源数量
- 修改价格和面积范围
- 添加更多地区
- 创建中等相似度的房源（用于测试边界情况）

