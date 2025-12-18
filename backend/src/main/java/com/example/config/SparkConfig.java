// SparkConfig.java
package com.example.config;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;

/**
 * Spark配置类
 * 
 * 特点：
 * 1. Windows本地开发环境兼容
 * 2. 支持通过配置文件调整参数
 * 3. 优化的内存和序列化配置
 * 
 * 版本兼容性：
 * - Java 1.8
 * - Spark 3.3.4 (Scala 2.12)
 * - Hadoop 3.4.1
 */
@Configuration
public class SparkConfig {
    
    @Value("${spark.app.name:PropertyRecommendation}")
    private String appName;
    
    @Value("${spark.master:local[*]}")
    private String master;
    
    @Value("${spark.driver.memory:4g}")
    private String driverMemory;
    
    @Value("${spark.executor.memory:4g}")
    private String executorMemory;
    
    @Value("${spark.sql.shuffle.partitions:10}")
    private String shufflePartitions;
    
    private SparkSession sparkSession;
    
    @Bean
    public SparkSession sparkSession() {
        // ========== Windows环境兼容性设置 ==========
        String tempDir = System.getProperty("java.io.tmpdir");
        System.setProperty("hadoop.home.dir", tempDir);
        System.setProperty("HADOOP_HOME", tempDir);
        
        // 禁用Hadoop安全检查
        System.setProperty("hadoop.security.authentication", "simple");
        
        // ========== Spark配置 ==========
        SparkConf conf = new SparkConf()
            .setAppName(appName)
            .setMaster(master)
            
            // ---------- 内存配置 ----------
            .set("spark.driver.memory", driverMemory)
            .set("spark.executor.memory", executorMemory)
            
            // ---------- 序列化配置（性能优化）----------
            .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
            .set("spark.kryoserializer.buffer.max", "256m")
            
            // ---------- 并行度配置 ----------
            .set("spark.sql.shuffle.partitions", shufflePartitions)
            .set("spark.default.parallelism", shufflePartitions)
            
            // ---------- 时间解析兼容性 ----------
            .set("spark.sql.legacy.timeParserPolicy", "LEGACY")
            
            // ---------- 网络配置（本地模式）----------
            .set("spark.driver.bindAddress", "127.0.0.1")
            .set("spark.ui.enabled", "false")
            
            // ---------- 文件系统配置（本地模式）----------
            .set("spark.hadoop.fs.defaultFS", "file:///")
            .set("spark.hadoop.fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem")
            .set("spark.sql.warehouse.dir", tempDir + "/spark-warehouse")
            
            // ---------- 其他优化 ----------
            .set("spark.hadoop.validateOutputSpecs", "false")
            .set("spark.sql.adaptive.enabled", "true");
        
        sparkSession = SparkSession.builder()
            .config(conf)
            .getOrCreate();
        
        // 设置日志级别，减少输出
        sparkSession.sparkContext().setLogLevel("WARN");
        
        System.out.println("========================================");
        System.out.println("SparkSession 初始化完成");
        System.out.println("  App Name: " + appName);
        System.out.println("  Master: " + master);
        System.out.println("  Driver Memory: " + driverMemory);
        System.out.println("  Executor Memory: " + executorMemory);
        System.out.println("  Spark Version: " + sparkSession.version());
        System.out.println("========================================");
        
        return sparkSession;
    }
    
    /**
     * 应用关闭时释放Spark资源
     */
    @PreDestroy
    public void close() {
        if (sparkSession != null && !sparkSession.sparkContext().isStopped()) {
            sparkSession.stop();
            System.out.println("SparkSession 已关闭");
        }
    }
}