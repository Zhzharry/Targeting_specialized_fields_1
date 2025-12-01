# 房价模型使用说明（predict_zhz）

该目录用于存放各城市的房价预测模型文件，模型由随机森林训练得到，包含：

- `house_price_model.pkl`：模型本体（Pickle 格式）
- `model_config.json`：模型配置，包含特征列、编码、统计量等信息

当前支持的城市文件夹：

| 城市 | 文件夹名 | 备注 |
| ---- | -------- | ---- |
| 北京 | `beijng` | 默认示例模型 |
| 上海 | `shanghai` | 需放置对应模型/配置 |
| 天津 | `tianjin` | 需放置对应模型/配置 |
| 石家庄 | `shijiazhuang` | 需放置对应模型/配置 |

> ⚠️ 若某城市缺少模型文件，可在对应目录下补齐 `house_price_model.pkl` 和 `model_config.json`。

---

## 调用流程

1. **选择模型**：根据前端传入的城市字段映射到对应文件夹。
2. **加载配置**：读取 `model_config.json`，获取特征列和均值等信息。
3. **准备特征**：按照配置中的 `feature_columns` 顺序构建特征向量。缺失值可使用 `scaler_info.means` 中的默认值。
4. **调用 Python 脚本**：通过脚本 `predict_price.py` 加载 pkl 模型，执行预测并返回结果。
5. **返回结果**：接口直接返回预测值（单位：万元/㎡），无需持久化。

环境变量（可选）：

| 配置项 | 说明 | 默认值 |
| ------ | ---- | ------ |
| `predictor.model-base-dir` | 模型根目录 | `src/main/java/com/example/service/predict_zhz` |
| `predictor.python-exec` | Python 可执行文件 | `python` |

---

## 特征列说明（以北京模型为例）

调用模型时需要提供 `model_config.json` 中 `feature_columns` 指定的特征。常见输入示例（可根据业务裁剪）：

| 字段 | 含义 | 示例值 |
| ---- | ---- | ---- |
| `经度` | 房源经度 | 116.4014 |
| `纬度` | 房源纬度 | 39.9263 |
| `到市中心距离_km` | 到中心距离（公里） | 8.5 |
| `区域编码` | 行政区编码（见 `area_encoder`） | 13 |
| `面积（m²）` | 建筑面积 | 90 |
| `室数` / `厅数` / `卫数` | 户型信息 | 3 / 2 / 2 |
| `房龄` | 房龄（年） | 10 |
| `朝向评分` | 朝向评分（1-5） | 4 |
| `小区面积_数值` / `小区户数_数值` | 小区规模 | 800000 / 2000 |
| `容积率_数值` / `绿化率_数值` / `物业费_数值` | 小区配套指标 | 2.0 / 30 / 2.5 |
| `area_encoded` | 区域编码（字符串映射） | 14（海淀） |
| `面积_平方` / `面积_立方` 等 | 面积派生特征 | 根据面积计算 |
| `距离_平方` / `距离_倒数` / `距离_对数` | 距离派生特征 | 根据距离计算 |
| `房龄_平方` / `房龄_对数` | 房龄派生特征 | 根据房龄计算 |
| `面积_距离_交互` / `面积_房龄_交互` 等 | 交互特征 | 根据原始值组合 |

> **提示**：若前端无法全部提供，可在服务端对缺失项用配置中的均值填充，以保证特征维度一致。

完整特征列表可在 `beijng/model_config.json` 的 `feature_columns` 数组中查看，其它城市配置应与之对应。

---

## Python 脚本（predict_price.py）

位于 `predict_zhz/predict_price.py`，作用：

1. 读取输入 JSON（包含模型路径、配置路径、特征）。
2. 加载模型与配置，按顺序构建特征向量。
3. 调用 `model.predict()` 输出 `prediction` 字段。

调用时需确保 Python 环境安装 `numpy`、`scikit-learn` 等依赖。

---

## 接口集成

`ProfileController` 的 `POST /api/profile/price-predict` 接口会：

1. 接收 `city` 和 `features`（JSON 对象）。
2. 根据城市映射选择模型目录。
3. 调用 `HousePricePredictionService` 执行预测。
4. 将 `predictedPricePerSquareMeter` 返回前端（单位：万元/㎡）。

无需在数据库中保存记录，后续可直接在接口中替换或扩展模型逻辑。

