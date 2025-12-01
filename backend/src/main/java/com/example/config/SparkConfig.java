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
        // 设置系统属性，避免Hadoop依赖问题
        System.setProperty("hadoop.home.dir", "C:/hadoop");
        System.setProperty("HADOOP_HOME", "C:/hadoop");
        
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
            .set("spark.hadoop.fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem");
        
        return SparkSession.builder()
            .config(conf)
            .getOrCreate();
    }
}