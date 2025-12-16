package com.example.config;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

/**
 * Hadoop配置类
 * 位置：backend/src/main/java/com/example/config/HadoopConfig.java
 */
@org.springframework.context.annotation.Configuration
public class HadoopConfig {

    @Value("${hadoop.fs.defaultFS:hdfs://localhost:9000}")
    private String defaultFS;

    @Value("${hadoop.mapreduce.framework.name:local}")
    private String mapReduceFramework;

    @Value("${hadoop.data.dir:/tmp/hadoop-data}")
    private String hadoopDataDir;

    /**
     * 创建Hadoop Configuration Bean
     */
    @Bean
    public Configuration hadoopConfiguration() {
        Configuration conf = new Configuration();
        
        // 基本配置
        conf.set("fs.defaultFS", defaultFS);
        conf.set("mapreduce.framework.name", mapReduceFramework);
        
        // 本地模式配置（开发环境）
        if ("local".equals(mapReduceFramework)) {
            conf.set("mapreduce.jobtracker.address", "local");
            conf.set("fs.defaultFS", "file:///");
        }
        
        // 序列化配置
        conf.set("io.serializations", 
            "org.apache.hadoop.io.serializer.JavaSerialization," +
            "org.apache.hadoop.io.serializer.WritableSerialization");
        
        // 数据目录
        conf.set("hadoop.tmp.dir", hadoopDataDir + "/tmp");
        conf.set("mapreduce.cluster.local.dir", hadoopDataDir + "/local");
        
        return conf;
    }

    /**
     * 创建Hadoop FileSystem Bean
     */
    @Bean
    public FileSystem hadoopFileSystem(Configuration hadoopConfiguration) throws IOException {
        return FileSystem.get(hadoopConfiguration);
    }

    public String getHadoopDataDir() {
        return hadoopDataDir;
    }
}