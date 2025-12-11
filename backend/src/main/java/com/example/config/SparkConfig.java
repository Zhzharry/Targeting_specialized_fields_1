// // SparkConfig.java
// package com.example.config;

// import org.apache.spark.SparkConf;
// import org.apache.spark.sql.SparkSession;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;

// @Configuration
// public class SparkConfig {
    
//     @Bean
//     public SparkSession sparkSession() {
//         SparkConf conf = new SparkConf()
//             .setAppName("SecondHandHousingRecommendation")
//             .setMaster("local[*]")
//             .set("spark.driver.memory", "4g")
//             .set("spark.executor.memory", "4g")
//             .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
//             .set("spark.kryoserializer.buffer.max", "256m")
//             .set("spark.sql.shuffle.partitions", "10")
//             .set("spark.default.parallelism", "10");
        
//         return SparkSession.builder()
//             .config(conf)
//             .appName("SecondHandHousingRecommendation")
//             .getOrCreate();
//     }
// }

// SparkConfig.java
package com.example.config;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SparkConfig {
    
    @Bean
    public SparkSession sparkSession() {
        // 设置系统属性，避免Hadoop依赖问题（Windows环境）
        // 使用临时目录作为hadoop.home.dir，避免找不到目录的错误
        String tempDir = System.getProperty("java.io.tmpdir");
        System.setProperty("hadoop.home.dir", tempDir);
        System.setProperty("HADOOP_HOME", tempDir);
        
        // 禁用某些Hadoop安全检查
        System.setProperty("hadoop.security.authentication", "simple");
        
        // 对于Java 23，禁用安全管理器检查
        System.setProperty("java.security.manager", "allow");
        
        SparkConf conf = new SparkConf()
            .setAppName("PropertyRecommendation")
            .setMaster("local[*]")  // 本地模式
            .set("spark.sql.legacy.timeParserPolicy", "LEGACY")
            .set("spark.driver.bindAddress", "127.0.0.1")
            .set("spark.ui.enabled", "false")  // 禁用Web UI
            .set("spark.hadoop.fs.defaultFS", "file:///")  // 使用本地文件系统
            .set("spark.hadoop.fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem")
            // 禁用Hadoop native库检查，避免Windows环境下的警告
            .set("spark.hadoop.fs.AbstractFileSystem.file.impl", "org.apache.hadoop.fs.LocalFileSystem")
            .set("spark.sql.warehouse.dir", tempDir + "/spark-warehouse");
        
        return SparkSession.builder()
            .config(conf)
            .getOrCreate();
    }
}