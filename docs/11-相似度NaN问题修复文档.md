# 相似度 NaN 问题修复文档

**文档版本**: v1.0
**修复日期**: 2024 年 12 月 18 日
**问题状态**: ✅ 已修复

## 🔍 问题描述

在"其他用户也在看"推荐功能中，房源卡片的相似度显示出现"NaN%"，影响用户体验。

### 问题现象

- 推荐房源卡片显示"相似度 NaN%"
- 前端控制台可能出现数学计算错误
- 影响推荐功能的可靠性感知

## 🔧 问题根因分析

### 1. 后端计算问题

在 `OthersAlsoViewedController.java` 的 `calculateRecommendationScore()` 方法中：

- 相似度计算可能涉及除零操作
- 数学运算结果可能为 `Double.NaN`
- 未对无效数值进行处理

### 2. 热门房源补足问题

当个性化推荐不足时，使用热门房源补足，但 `getPopularProperties()` 方法：

- 未设置 `score` 字段
- 前端尝试计算 `Math.round(property.score * 100)` 时因 `score` 为 `null` 而产生 `NaN`

## 🛠️ 修复方案

### 1. 后端修复 (`OthersAlsoViewedController.java`)

#### 修复相似度计算方法

```java
private double calculateRecommendationScore(Map<String, Object> userData, Map<String, Object> propertyData) {
    // ... 现有计算逻辑 ...

    // 添加NaN检查和边界处理
    if (Double.isNaN(score)) {
        return 0.0;  // 返回默认相似度
    }
    return Math.min(1.0, Math.max(0.0, score));  // 确保分数在0-1范围内
}
```

#### 修复热门房源 score 设置

```java
// 为热门房源设置默认相似度分数（基于浏览量计算，范围0.1-0.5）
double score = Math.max(0.1, Math.min(0.5, viewCount / 20.0));
item.put("score", score);
```

### 2. 前端修复 (`SearchPage.vue`)

#### 添加安全检查

```vue
<span class="similarity-score">
  相似度 {{ property.score && !isNaN(property.score) ? Math.round(property.score * 100) : '计算中' }}%
</span>
```

## 📊 修复效果

### 修复前

- 相似度显示: "相似度 NaN%"
- 用户体验: 困惑和不信任

### 修复后

- 个性化推荐: "相似度 85%" (基于用户相似度计算)
- 热门房源补足: "相似度 25%" (基于浏览量计算)
- 异常情况: "相似度 计算中%" (友好提示)

## 🧪 测试验证

### 自动化测试脚本

创建 `test-recommendation.js` 用于验证 API 返回的相似度分数是否有效：

```javascript
// 检查所有房源的score字段是否为有效数值
response.data.forEach((property, index) => {
  const score = property.score;
  if (
    score !== null &&
    score !== undefined &&
    !isNaN(score) &&
    score >= 0 &&
    score <= 1
  ) {
    console.log(`✅ 房源 ${index + 1}: 相似度 ${(score * 100).toFixed(1)}%`);
  } else {
    console.log(`❌ 房源 ${index + 1}: 相似度无效 (${score})`);
  }
});
```

### 手动测试步骤

1. 启动后端服务: `mvnw spring-boot:run`
2. 启动前端服务: `npm run dev`
3. 访问搜索页面，查看"其他用户也在看"区域
4. 验证所有房源都显示有效的相似度百分比

## 📁 相关文件

- `backend/src/main/java/com/example/controller/OthersAlsoViewedController.java`
- `frontend/src/views/SearchPage.vue`
- `test-recommendation.js` (测试脚本)
- `启动服务.bat` (快速启动脚本)

## 🎯 预防措施

### 代码规范

1. 所有数学计算后检查 `Double.isNaN()` 和 `Double.isInfinite()`
2. 为可选字段设置合理的默认值
3. 前端显示数值前进行有效性检查

### 监控建议

1. 添加日志记录异常相似度计算
2. 监控 API 响应中的无效分数比例
3. 设置告警阈值，当 NaN 比例超过一定值时触发通知

## 📝 更新日志

- **v1.0** (2024-12-18): 完成相似度 NaN 问题的修复和文档编写
