# 数据库数据注入脚本使用说明

## 概述

本目录包含为数据库各个表注入测试数据的SQL脚本，以及一键执行脚本。

## 文件说明

### SQL脚本文件（按执行顺序）

1. **01_users.sql** - 用户表数据注入
   - 新增10个用户（user_id: 4-13）
   - 包含用户基本信息、手机号、用户画像等

2. **02_communities.sql** - 小区表数据注入
   - 新增8个小区（community_id: 7-14）
   - 包含小区基本信息、位置信息、设施信息等

3. **03_properties.sql** - 房源表数据注入
   - 新增17个房源（property_id: 10-26）
   - 包含房源基本信息、价格信息、户型信息等
   - **注意**: 依赖02_communities.sql中的小区ID

4. **04_browsing_history.sql** - 浏览历史表数据注入
   - 为多个用户生成浏览历史记录
   - 包含浏览时长、浏览次数等行为数据

5. **05_favorites.sql** - 收藏表数据注入
   - 为多个用户生成收藏记录
   - 包含收藏备注、标签等信息

6. **06_search_history.sql** - 搜索历史表数据注入
   - 为多个用户生成搜索历史记录
   - 包含搜索关键词、筛选条件等

7. **07_user_preferences.sql** - 用户偏好表数据注入
   - 为新增用户生成偏好数据
   - 包含关键词、位置、价格范围、户型偏好等

8. **08_user_recommendations.sql** - 用户推荐表数据注入
   - 为多个用户生成推荐记录
   - 包含推荐类型、分数、原因等

9. **09_user_similarity.sql** - 用户相似度表数据注入
   - 生成用户之间的相似度数据
   - 基于协同过滤算法计算

10. **10_property_similarity.sql** - 房源相似度表数据注入
    - 生成房源之间的相似度数据
    - 基于内容相似度算法计算

11. **11_property_features.sql** - 房源特征表数据注入
    - 为新增房源添加特征标签
    - 包含地铁、学区、海景等特征

### 一键执行脚本

- **一键注入数据.bat** - Windows批处理脚本
- **一键注入数据.sh** - Linux/Mac Shell脚本
- **一键注入数据.py** - Python跨平台脚本（推荐）

## 使用方法

### 方法一：使用一键脚本（推荐）

#### Windows系统
```bash
# 双击运行或在命令行执行
一键注入数据.bat
```

#### Linux/Mac系统
```bash
# 添加执行权限
chmod +x 一键注入数据.sh

# 执行脚本
./一键注入数据.sh
```

#### Python脚本（跨平台）
```bash
# 确保已安装Python 3.7+
python 一键注入数据.py
```

### 方法二：手动执行SQL文件

如果需要单独执行某个SQL文件，可以使用MySQL命令行：

```bash
mysql -hlocalhost -P3306 -ubigdata_user -p123456 bigdata < 01_users.sql
```

或者使用MySQL客户端工具（如Navicat、DBeaver等）逐个执行。

## 配置说明

### 数据库连接配置

在执行脚本前，请确保数据库连接配置正确。可以在以下文件中修改：

- **一键注入数据.bat**: 修改文件开头的变量
  ```batch
  set DB_HOST=localhost
  set DB_PORT=3306
  set DB_USER=bigdata_user
  set DB_PASSWORD=123456
  set DB_NAME=bigdata
  ```

- **一键注入数据.sh**: 修改文件开头的变量
  ```bash
  DB_HOST="localhost"
  DB_PORT="3306"
  DB_USER="bigdata_user"
  DB_PASSWORD="123456"
  DB_NAME="bigdata"
  ```

- **一键注入数据.py**: 修改文件中的 `DB_CONFIG` 字典
  ```python
  DB_CONFIG = {
      'host': 'localhost',
      'port': '3306',
      'user': 'bigdata_user',
      'password': '123456',
      'database': 'bigdata'
  }
  ```

## 执行顺序

**重要**: SQL文件必须按顺序执行，因为存在外键依赖关系：

1. 先执行基础表：users, communities, properties
2. 再执行关联表：browsing_history, favorites, search_history等
3. 最后执行相似度和推荐表

脚本已经按照正确的顺序命名（01-11），按顺序执行即可。

## 数据说明

### 用户数据
- 新增10个用户（ID: 4-13）
- 包含不同预算、家庭结构、偏好位置的用户

### 小区数据
- 新增8个小区（ID: 7-14）
- 分布在深圳不同区域（南山区、福田区、宝安区等）
- 包含不同开发商、建成年份、设施配置

### 房源数据
- 新增17个房源（ID: 10-26）
- 包含不同户型（2房、3房、4房）
- 不同价格区间（300万-1200万）
- 不同装修程度（精装、豪华）

### 行为数据
- 浏览历史：约50条记录
- 收藏记录：约30条记录
- 搜索历史：约30条记录

### 推荐和相似度数据
- 用户推荐：约30条记录
- 用户相似度：约20对用户
- 房源相似度：约30对房源

## 注意事项

1. **外键约束**: 确保先执行基础表（users, communities, properties）的脚本
2. **ID冲突**: 如果数据库中已有数据，可能需要调整SQL中的ID范围
3. **数据量**: 脚本生成的是测试数据，实际生产环境需要更多数据
4. **编码问题**: 确保数据库和脚本文件都使用UTF-8编码

## 故障排除

### 问题1: MySQL命令未找到
**解决方案**: 
- 确保MySQL已安装
- 将MySQL的bin目录添加到系统PATH环境变量
- 或者使用MySQL客户端工具手动执行SQL文件

### 问题2: 连接数据库失败
**解决方案**:
- 检查数据库服务是否运行
- 检查用户名、密码是否正确
- 检查数据库名称是否存在
- 检查防火墙设置

### 问题3: 外键约束错误
**解决方案**:
- 确保按顺序执行SQL文件
- 检查基础表数据是否已正确插入
- 检查外键引用的ID是否存在

### 问题4: 编码问题
**解决方案**:
- 确保数据库字符集为utf8mb4
- 确保SQL文件使用UTF-8编码保存
- 在MySQL连接时指定字符集

## 数据验证

执行完成后，可以使用以下SQL验证数据：

```sql
-- 检查用户数量
SELECT COUNT(*) FROM users;

-- 检查小区数量
SELECT COUNT(*) FROM communities;

-- 检查房源数量
SELECT COUNT(*) FROM properties;

-- 检查浏览历史数量
SELECT COUNT(*) FROM browsing_history;

-- 检查收藏数量
SELECT COUNT(*) FROM favorites;
```

## 扩展数据

如果需要更多测试数据，可以：

1. 复制现有SQL文件并修改数据
2. 使用数据生成工具（如Faker）
3. 从实际业务数据中提取

## 联系支持

如有问题，请检查：
1. 数据库连接配置
2. SQL文件执行顺序
3. 数据库表结构是否匹配
4. 外键约束是否正确

