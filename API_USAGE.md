# Hadoop WordCount API 使用说明

## 🚀 新功能：自定义文件路径选择

现在您可以通过API自由选择要处理的文件路径，并启动WordCount分析。

## 📋 API 端点

### 1. 设置输入路径
**POST** `/api/hadoop/input`

设置要处理的文件或目录路径。

**请求体：**
```json
{
    "inputPath": "/path/to/your/files"
}
```

**响应示例：**
```json
{
    "status": "success",
    "message": "输入路径设置成功: /path/to/your/files",
    "inputPath": "/path/to/your/files"
}
```

### 2. 启动WordCount作业
**POST** `/api/hadoop/start`

启动MapReduce分析作业。

**请求体（可选）：**
```json
{
    "outputPath": "/custom/output/path"
}
```

**响应示例：**
```json
{
    "status": "success",
    "message": "WordCount作业执行成功",
    "inputPath": "/path/to/your/files",
    "outputPath": "/output"
}
```

### 3. 查看当前状态
**GET** `/api/hadoop/status`

获取服务当前状态和配置。

**响应示例：**
```json
{
    "status": "running",
    "message": "Hadoop WordCount Service is running",
    "selectedInputPath": "/path/to/your/files",
    "outputPath": "/output"
}
```

### 4. 查看配置
**GET** `/api/hadoop/config`

获取当前配置信息。

**响应示例：**
```json
{
    "inputPath": "/path/to/your/files",
    "outputPath": "/output",
    "status": "configured"
}
```

### 5. 重置配置
**POST** `/api/hadoop/reset`

重置所有配置。

**响应示例：**
```json
{
    "status": "success",
    "message": "配置已重置"
}
```

## 🔄 使用流程

### 方法1：使用Web界面
1. 打开浏览器访问 `http://localhost:8080`
2. 在"设置输入路径"部分输入文件路径
3. 点击"设置输入路径"按钮
4. 点击"开始分析"按钮启动作业

### 方法2：使用curl命令

#### 步骤1：设置输入路径
```bash
curl -X POST "http://localhost:8080/api/hadoop/input" \
  -H "Content-Type: application/json" \
  -d '{"inputPath": "/path/to/your/files"}'
```

#### 步骤2：启动WordCount作业
```bash
# 使用默认输出路径
curl -X POST "http://localhost:8080/api/hadoop/start"

# 或使用自定义输出路径
curl -X POST "http://localhost:8080/api/hadoop/start" \
  -H "Content-Type: application/json" \
  -d '{"outputPath": "/custom/output/path"}'
```

#### 步骤3：查看状态
```bash
curl -X GET "http://localhost:8080/api/hadoop/status"
```

## 📁 支持的文件类型

- **单个文件**：`/path/to/file.txt`
- **目录**：`/path/to/directory/`（会处理目录下所有文件）
- **相对路径**：`./data/input/`
- **绝对路径**：`/home/user/data/`

## ⚠️ 注意事项

1. **路径验证**：系统会验证输入路径是否存在
2. **输出路径**：如果不指定输出路径，默认使用 `/output`
3. **文件权限**：确保应用有读取输入文件和写入输出目录的权限
4. **Hadoop环境**：确保Hadoop环境已正确配置

## 🐛 错误处理

### 常见错误及解决方案

1. **路径不存在**
   ```json
   {
     "status": "error",
     "message": "指定的文件或目录不存在: /invalid/path"
   }
   ```

2. **未设置输入路径**
   ```json
   {
     "status": "error",
     "message": "请先设置输入路径，使用 /api/hadoop/input 端点"
   }
   ```

3. **Hadoop作业执行失败**
   ```json
   {
     "status": "error",
     "message": "WordCount作业执行失败，退出码: 1"
   }
   ```

## 🎯 示例场景

### 场景1：处理单个文件
```bash
# 设置单个文件路径
curl -X POST "http://localhost:8080/api/hadoop/input" \
  -H "Content-Type: application/json" \
  -d '{"inputPath": "/home/user/document.txt"}'

# 启动分析
curl -X POST "http://localhost:8080/api/hadoop/start"
```

### 场景2：处理整个目录
```bash
# 设置目录路径
curl -X POST "http://localhost:8080/api/hadoop/input" \
  -H "Content-Type: application/json" \
  -d '{"inputPath": "/home/user/documents/"}'

# 启动分析并指定输出路径
curl -X POST "http://localhost:8080/api/hadoop/start" \
  -H "Content-Type: application/json" \
  -d '{"outputPath": "/results/wordcount"}'
```

### 场景3：批量处理多个文件
```bash
# 设置包含多个文件的目录
curl -X POST "http://localhost:8080/api/hadoop/input" \
  -H "Content-Type: application/json" \
  -d '{"inputPath": "/data/input_files/"}'

# 启动分析
curl -X POST "http://localhost:8080/api/hadoop/start"
```

## 🔧 高级配置

### 环境变量
```bash
export HADOOP_HOME=/opt/hadoop
export HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop
export HADOOP_USER_NAME=root
```

### 应用配置
在 `application.properties` 中可以配置：
```properties
# 服务器端口
server.port=8080

# Hadoop配置
hadoop.home.dir=/opt/hadoop
hadoop.conf.dir=/opt/hadoop/etc/hadoop
```

## 📊 监控和日志

- **应用日志**：查看控制台输出获取详细执行信息
- **Hadoop日志**：检查Hadoop作业执行日志
- **状态监控**：使用 `/api/hadoop/status` 端点监控服务状态

## 🚀 部署建议

1. **生产环境**：建议使用Hadoop集群模式
2. **开发环境**：可以使用本地模式进行测试
3. **资源管理**：根据数据量调整JVM内存设置
4. **安全配置**：在生产环境中配置适当的文件权限
