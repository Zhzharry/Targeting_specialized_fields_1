# Hadoop词频统计系统

基于Java 1.8、Spring Boot 2.7.18和Hadoop 3.4.1的词频统计系统，支持文件上传到HDFS并执行MapReduce作业。

## 功能特性

- 🚀 **完整的MapReduce词频统计**：支持HDFS上的文件进行词频统计
- 📁 **文件上传管理**：支持本地文件上传到HDFS
- 🌐 **RESTful API**：提供完整的REST API接口
- 🖥️ **Web界面**：提供友好的Web操作界面
- 📊 **实时监控**：支持HDFS状态检查和目录浏览

## 系统要求

- Java 1.8+
- Maven 3.6+
- Hadoop 3.4.1
- Spring Boot 2.7.18

## 快速开始

### 1. 环境准备

确保Hadoop环境已正确配置并启动：

```bash
# 启动HDFS
$HADOOP_HOME/sbin/start-dfs.sh

# 启动YARN
$HADOOP_HOME/sbin/start-yarn.sh

# 检查HDFS状态
hdfs dfsadmin -report
```

### 2. 配置修改

修改 `src/main/resources/application.properties` 中的Hadoop配置：

```properties
# 根据你的实际环境修改这些路径
hadoop.home.dir=/path/to/your/hadoop
hadoop.conf.dir=/path/to/your/hadoop/etc/hadoop
hdfs.default.fs=hdfs://your-namenode:9000
hdfs.user=your-username
```

### 3. 编译运行

```bash
# 编译项目
mvn clean compile

# 运行应用
mvn spring-boot:run
```

### 4. 访问系统

打开浏览器访问：http://localhost:8080

## API接口

### 基础接口

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/wordcount/` | 系统状态 |
| GET | `/api/wordcount/check-hdfs` | 检查HDFS状态 |
| GET | `/api/wordcount/run-demo` | 运行演示 |

### 文件操作

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/wordcount/upload` | 上传文件并分析 |
| POST | `/api/wordcount/send-to-input` | 发送文件到HDFS |
| GET | `/api/wordcount/list/{path}` | 列出HDFS目录 |

### MapReduce操作

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/wordcount/do` | 开始MapReduce分析 |
| GET | `/api/wordcount/run/{inputPath}/{outputPath}` | 执行MapReduce作业 |

## 使用示例

### 1. 运行演示

访问 `GET /api/wordcount/run-demo` 使用预设的测试数据：

- 文件A：`China is my motherland\nI love China`
- 文件B：`I am from China`

期望结果：
```
I 2
is 1
China 3
my 1
love 1
am 1
from 1
motherland 1
```

### 2. 上传文件分析

使用 `POST /api/wordcount/upload` 上传文本文件进行词频统计。

### 3. 模拟Linux命令流程

1. **发送文件到HDFS**：
   ```bash
   curl -X POST "http://localhost:8080/api/wordcount/send-to-input" \
        -d "filePath=fileA.txt"
   ```

2. **开始MapReduce分析**：
   ```bash
   curl -X POST "http://localhost:8080/api/wordcount/do" \
        -d "keyword=start"
   ```

## 项目结构

```
src/main/java/com/example/hadoopwordcount/
├── config/
│   └── HadoopConfig.java          # Hadoop配置类
├── controller/
│   └── WordCountController.java    # REST控制器
├── mapreduce/
│   ├── WordCountMapper.java       # Map阶段
│   └── WordCountReducer.java      # Reduce阶段
├── service/
│   ├── HadoopWordCountService.java # 业务逻辑服务
│   └── HDFSService.java           # HDFS操作服务
└── HadoopWordCountApplication.java # 主应用类
```

## 配置说明

### application.properties

```properties
# 应用配置
server.port=8080
spring.application.name=hadoop-wordcount

# 文件上传配置
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# Hadoop配置
hadoop.home.dir=/opt/hadoop
hadoop.conf.dir=/opt/hadoop/etc/hadoop
hdfs.default.fs=hdfs://localhost:9000
hdfs.user=root

# 日志配置
logging.level.com.example.hadoopwordcount=INFO
logging.level.org.apache.hadoop=WARN
```

## 故障排除

### 1. HDFS连接失败

检查Hadoop服务是否启动：
```bash
jps  # 查看Java进程
hdfs dfsadmin -report  # 检查HDFS状态
```

### 2. 权限问题

确保Hadoop用户有正确的权限：
```bash
hdfs dfs -chmod 755 /input
hdfs dfs -chmod 755 /output
```

### 3. 端口冲突

如果8080端口被占用，修改 `application.properties` 中的 `server.port` 配置。

## 开发说明

### 添加新的MapReduce作业

1. 创建新的Mapper和Reducer类
2. 在Service中添加新的作业执行方法
3. 在Controller中添加对应的API接口

### 扩展HDFS功能

在 `HDFSService` 中添加新的HDFS操作方法，如文件复制、移动、删除等。

## 许可证

MIT License

## 贡献

欢迎提交Issue和Pull Request来改进这个项目。
