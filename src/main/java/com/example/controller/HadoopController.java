package com.example.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import com.example.mapreduce.WordCount;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.io.InputStream;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import java.io.FileInputStream;
@RestController
@RequestMapping("/api/hadoop")
public class HadoopController {
    
    private String selectedInputPath;
    private String outputPath = "/output";
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "running");
        response.put("message", "Hadoop WordCount Service is running");
        response.put("selectedInputPath", selectedInputPath);
        response.put("outputPath", outputPath);
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Hadoop WordCount API");
        response.put("version", "1.0.0");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 接收用户选择的文件路径并上传到HDFS
     * POST /api/hadoop/input
     */
    @PostMapping("/input")
    public ResponseEntity<Map<String, Object>> setInputPath(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        String inputPath = request.get("inputPath");
        if (inputPath == null || inputPath.trim().isEmpty()) {
            response.put("status", "error");
            response.put("message", "输入路径不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        
        // 检查路径是否存在
        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            response.put("status", "error");
            response.put("message", "指定的文件或目录不存在: " + inputPath);
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            // 上传文件到HDFS
            uploadFileToHDFS(inputPath);
            
            // 保存输入路径
            selectedInputPath = inputPath;
            response.put("status", "success");
            response.put("message", "输入路径设置成功并已上传到HDFS: " + inputPath);
            response.put("inputPath", selectedInputPath);
            response.put("hdfsPath", "/input");
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "上传文件到HDFS失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 上传文件到HDFS
     */
    private void uploadFileToHDFS(String localPath) throws Exception {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", "hdfs://localhost:9000");
        conf.set("HADOOP_USER_NAME", "root");
        
        FileSystem fs = FileSystem.get(conf);
        
        // 创建HDFS输入目录
        Path hdfsInputDir = new Path("/input");
        if (!fs.exists(hdfsInputDir)) {
            fs.mkdirs(hdfsInputDir);
            System.out.println("创建HDFS目录: /input");
        }
        
        File localFile = new File(localPath);
        Path hdfsFilePath = new Path("/input/" + localFile.getName());
        
        // 如果HDFS中已存在该文件，先删除
        if (fs.exists(hdfsFilePath)) {
            fs.delete(hdfsFilePath, true);
            System.out.println("删除已存在的HDFS文件: " + hdfsFilePath);
        }
        
        // 上传文件
        try (InputStream in = new FileInputStream(localFile)) {
            fs.copyFromLocalFile(new Path(localPath), hdfsFilePath);
            System.out.println("文件上传成功: " + localPath + " -> " + hdfsFilePath);
        }
        
        fs.close();
    }
    
    /**
     * 删除HDFS输出目录
     */
    private void deleteHDFSOutputDirectory(String outputPath) throws Exception {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", "hdfs://localhost:9000");
        conf.set("HADOOP_USER_NAME", "root");
        
        FileSystem fs = FileSystem.get(conf);
        Path outputDir = new Path(outputPath);
        
        if (fs.exists(outputDir)) {
            fs.delete(outputDir, true);
            System.out.println("删除已存在的输出目录: " + outputPath);
        }
        
        fs.close();
    }
    
    /**
     * 启动MapReduce作业
     * POST /api/hadoop/start
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startWordCount(@RequestBody(required = false) Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        if (selectedInputPath == null || selectedInputPath.trim().isEmpty()) {
            response.put("status", "error");
            response.put("message", "请先设置输入路径，使用 /api/hadoop/input 端点");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            // 如果请求中指定了输出路径，则使用指定的路径
            if (request != null && request.containsKey("outputPath")) {
                outputPath = request.get("outputPath");
            }
            
            // 删除已存在的输出目录
            deleteHDFSOutputDirectory(outputPath);
            
            // 使用HDFS路径执行WordCount作业
            String hdfsInputPath = "/input";
            String[] args = {hdfsInputPath, outputPath};
            int result = WordCount.runWordCount(args);
            
            if (result == 0) {
                response.put("status", "success");
                response.put("message", "WordCount作业执行成功");
                response.put("inputPath", selectedInputPath);
                response.put("hdfsInputPath", hdfsInputPath);
                response.put("outputPath", outputPath);
            } else {
                response.put("status", "error");
                response.put("message", "WordCount作业执行失败，退出码: " + result);
            }
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "执行WordCount作业时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取当前配置
     * GET /api/hadoop/config
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> response = new HashMap<>();
        response.put("inputPath", selectedInputPath);
        response.put("outputPath", outputPath);
        response.put("status", selectedInputPath != null ? "configured" : "not_configured");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 重置配置
     * POST /api/hadoop/reset
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetConfig() {
        Map<String, Object> response = new HashMap<>();
        selectedInputPath = null;
        outputPath = "/output";
        response.put("status", "success");
        response.put("message", "配置已重置");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 查看HDFS文件列表
     * GET /api/hadoop/hdfs-list
     */
    @GetMapping("/hdfs-list")
    public ResponseEntity<Map<String, Object>> listHDFSFiles() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Configuration conf = new Configuration();
            conf.set("fs.defaultFS", "hdfs://localhost:9000");
            conf.set("HADOOP_USER_NAME", "root");
            
            FileSystem fs = FileSystem.get(conf);
            Path inputDir = new Path("/input");
            
            if (fs.exists(inputDir)) {
                org.apache.hadoop.fs.FileStatus[] files = fs.listStatus(inputDir);
                String[] fileNames = new String[files.length];
                for (int i = 0; i < files.length; i++) {
                    fileNames[i] = files[i].getPath().getName();
                }
                
                response.put("status", "success");
                response.put("message", "HDFS /input 目录文件列表");
                response.put("files", fileNames);
                response.put("count", files.length);
            } else {
                response.put("status", "info");
                response.put("message", "HDFS /input 目录不存在");
                response.put("files", new String[0]);
                response.put("count", 0);
            }
            
            fs.close();
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "查看HDFS文件列表失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 清理HDFS输出目录
     * POST /api/hadoop/clean
     */
    @PostMapping("/clean")
    public ResponseEntity<Map<String, Object>> cleanHDFS() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Configuration conf = new Configuration();
            conf.set("fs.defaultFS", "hdfs://localhost:9000");
            conf.set("HADOOP_USER_NAME", "root");
            
            FileSystem fs = FileSystem.get(conf);
            
            // 删除输出目录
            Path outputDir = new Path("/output");
            if (fs.exists(outputDir)) {
                fs.delete(outputDir, true);
                System.out.println("删除输出目录: /output");
            }
            
            // 删除输入目录（可选）
            Path inputDir = new Path("/input");
            if (fs.exists(inputDir)) {
                fs.delete(inputDir, true);
                System.out.println("删除输入目录: /input");
            }
            
            fs.close();
            
            response.put("status", "success");
            response.put("message", "HDFS清理完成");
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "清理HDFS失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return ResponseEntity.ok(response);
    }
}
