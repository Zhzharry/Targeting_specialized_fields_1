// PropertySimilarityCFService.java
package com.example.service;

import org.springframework.stereotype.Service;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.util.*;

/**
 * 协同过滤和Hadoop MapReduce相似度计算服务
 * 
 * 功能：
 * 1. 基于用户行为的协同过滤（Item-Item CF）
 * 2. Hadoop MapReduce大规模相似度计算
 * 
 * 版本兼容性：
 * - Java 1.8
 * - Hadoop 3.4.1
 * - Spark 3.3.4
 */
@Service
public class PropertySimilarityCFService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Value("${hadoop.hdfs.uri:hdfs://localhost:9000}")
    private String hdfsUri;
    
    @Value("${hadoop.tmp.dir:/tmp/hadoop-property-similarity}")
    private String hadoopTmpDir;
    
    private static final int BATCH_SIZE = 1000;
    private static final double CF_THRESHOLD = 0.05;
    
    /**
     * 基于用户行为计算房源相似度（物品-物品协同过滤）
     */
    @Transactional
    public void calculatePropertySimilarityCF() {
        System.out.println("========================================");
        System.out.println("开始计算基于协同过滤的房源相似度...");
        System.out.println("========================================");
        
        // 从浏览历史获取数据
        String sql = "SELECT user_id, property_id, " +
                    "COALESCE(JSON_EXTRACT(behavior_data, '$.view_count'), 1) as view_count, " +
                    "COALESCE(JSON_EXTRACT(behavior_data, '$.duration'), 60) as duration " +
                    "FROM browsing_history " +
                    "WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
        
        List<Map<String, Object>> views = jdbcTemplate.queryForList(sql);
        
        if (views.isEmpty()) {
            System.out.println("没有找到浏览历史数据，尝试使用收藏数据...");
            views = jdbcTemplate.queryForList(
                "SELECT user_id, property_id, 1 as view_count, 300 as duration FROM favorites");
        }
        
        if (views.isEmpty()) {
            System.out.println("警告：没有找到用户行为数据，跳过协同过滤计算");
            return;
        }
        
        System.out.println("获取到 " + views.size() + " 条用户行为数据");
        
        // 构建用户-物品评分矩阵
        Map<Integer, Map<Integer, Double>> userItemMatrix = new HashMap<>();
        
        for (Map<String, Object> view : views) {
            Integer userId = ((Number) view.get("user_id")).intValue();
            Integer propertyId = ((Number) view.get("property_id")).intValue();
            Double viewCount = ((Number) view.get("view_count")).doubleValue();
            Double duration = ((Number) view.get("duration")).doubleValue();
            
            // 计算隐式评分
            double implicitRating = viewCount * Math.log1p(duration / 60.0);
            
            userItemMatrix.computeIfAbsent(userId, k -> new HashMap<>())
                         .merge(propertyId, implicitRating, Double::sum);
        }
        
        System.out.println("用户数: " + userItemMatrix.size());
        
        // 计算物品-物品相似度
        calculateItemItemSimilarity(userItemMatrix);
    }
    
    /**
     * 计算物品-物品相似度
     */
    private void calculateItemItemSimilarity(Map<Integer, Map<Integer, Double>> userItemMatrix) {
        // 获取所有房源ID
        Set<Integer> allProperties = new HashSet<>();
        for (Map<Integer, Double> items : userItemMatrix.values()) {
            allProperties.addAll(items.keySet());
        }
        
        List<Integer> propertyList = new ArrayList<>(allProperties);
        Collections.sort(propertyList);
        
        Map<Integer, Integer> propertyIndex = new HashMap<>();
        for (int i = 0; i < propertyList.size(); i++) {
            propertyIndex.put(propertyList.get(i), i);
        }
        
        int n = propertyList.size();
        if (n < 2) {
            System.out.println("房源数量不足，无法计算协同过滤相似度");
            return;
        }
        
        System.out.println("参与计算的房源数: " + n);
        
        // 构建共现矩阵
        double[][] cooccurrence = new double[n][n];
        double[] itemPopularity = new double[n];
        
        for (Map<Integer, Double> items : userItemMatrix.values()) {
            List<Integer> userItems = new ArrayList<>(items.keySet());
            
            for (int i = 0; i < userItems.size(); i++) {
                int idx1 = propertyIndex.get(userItems.get(i));
                double rating1 = items.get(userItems.get(i));
                itemPopularity[idx1] += 1;
                
                for (int j = i + 1; j < userItems.size(); j++) {
                    int idx2 = propertyIndex.get(userItems.get(j));
                    double rating2 = items.get(userItems.get(j));
                    
                    // 加权共现
                    double weight = Math.sqrt(rating1 * rating2);
                    cooccurrence[idx1][idx2] += weight;
                    cooccurrence[idx2][idx1] += weight;
                }
            }
        }
        
        // 计算相似度并保存
        String insertSql = "INSERT INTO property_similarity " +
                          "(property_id1, property_id2, similarity_data, created_at, updated_at) " +
                          "VALUES (?, ?, ?, NOW(), NOW()) " +
                          "ON DUPLICATE KEY UPDATE " +
                          "similarity_data = JSON_SET(similarity_data, '$.cf_similarity', " +
                          "CAST(JSON_EXTRACT(VALUES(similarity_data), '$.cf_similarity') AS DECIMAL(8,4))), " +
                          "updated_at = NOW()";
        
        List<Object[]> batchArgs = new ArrayList<>();
        int savedPairs = 0;
        
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (cooccurrence[i][j] > 0) {
                    // Jaccard相似度
                    double jaccard = cooccurrence[i][j] / 
                        (itemPopularity[i] + itemPopularity[j] - cooccurrence[i][j] + 1e-10);
                    
                    // 余弦相似度（基于共现）
                    double cosine = cooccurrence[i][j] / 
                        (Math.sqrt(itemPopularity[i]) * Math.sqrt(itemPopularity[j]) + 1e-10);
                    
                    double cfSimilarity = 0.6 * jaccard + 0.4 * cosine;
                    
                    if (cfSimilarity > CF_THRESHOLD) {
                        int prop1 = propertyList.get(i);
                        int prop2 = propertyList.get(j);
                        
                        JSONObject data = new JSONObject();
                        data.put("cf_similarity", Math.round(cfSimilarity * 10000) / 10000.0);
                        data.put("jaccard_score", Math.round(jaccard * 10000) / 10000.0);
                        data.put("cosine_cf_score", Math.round(cosine * 10000) / 10000.0);
                        data.put("cooccurrence_count", cooccurrence[i][j]);
                        data.put("algorithm", "item_item_cf");
                        data.put("calculation_method", "user_behavior");
                        
                        batchArgs.add(new Object[]{prop1, prop2, data.toJSONString()});
                        savedPairs++;
                        
                        if (batchArgs.size() >= BATCH_SIZE) {
                            jdbcTemplate.batchUpdate(insertSql, batchArgs);
                            batchArgs.clear();
                        }
                    }
                }
            }
        }
        
        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(insertSql, batchArgs);
        }
        
        System.out.println("协同过滤相似度计算完成，保存了 " + savedPairs + " 对数据");
    }
    
    // ==================== Hadoop MapReduce部分 ====================
    
    /**
     * 使用Hadoop MapReduce计算大规模相似度
     * 适用于房源数量超过10000的情况
     */
    public void calculateSimilarityWithMapReduce() throws Exception {
        System.out.println("========================================");
        System.out.println("使用Hadoop MapReduce计算房源相似度...");
        System.out.println("========================================");
        
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", hdfsUri);
        
        String inputPath = hadoopTmpDir + "/input/properties.txt";
        String outputPath = hadoopTmpDir + "/output/similarity";
        
        // 导出房源数据到HDFS
        exportPropertiesToHDFS(conf, inputPath);
        
        // 删除输出目录
        FileSystem fs = FileSystem.get(conf);
        Path outPath = new Path(outputPath);
        if (fs.exists(outPath)) {
            fs.delete(outPath, true);
        }
        
        // 配置MapReduce作业
        Job job = Job.getInstance(conf, "Property Similarity Calculation");
        job.setJarByClass(PropertySimilarityCFService.class);
        
        job.setMapperClass(SimilarityMapper.class);
        job.setReducerClass(SimilarityReducer.class);
        
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        
        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        
        FileInputFormat.addInputPath(job, new Path(inputPath));
        FileOutputFormat.setOutputPath(job, outPath);
        
        boolean success = job.waitForCompletion(true);
        
        if (success) {
            importSimilarityFromHDFS(conf, outputPath);
            System.out.println("MapReduce相似度计算完成！");
        } else {
            throw new RuntimeException("MapReduce作业执行失败");
        }
    }
    
    /**
     * 导出房源数据到HDFS
     */
    private void exportPropertiesToHDFS(Configuration conf, String path) throws IOException {
        String sql = "SELECT p.property_id, " +
                    "COALESCE(JSON_EXTRACT(p.price_info, '$.total_price'), 0) as total_price, " +
                    "COALESCE(JSON_EXTRACT(p.price_info, '$.unit_price'), 0) as unit_price, " +
                    "COALESCE(JSON_EXTRACT(p.layout_info, '$.area'), 0) as area, " +
                    "COALESCE(JSON_EXTRACT(p.layout_info, '$.bedroom_count'), 0) as bedroom_count, " +
                    "COALESCE(JSON_EXTRACT(p.layout_info, '$.floor'), 0) as floor, " +
                    "COALESCE(JSON_EXTRACT(p.layout_info, '$.total_floors'), 1) as total_floors, " +
                    "COALESCE(JSON_EXTRACT(c.location_info, '$.longitude'), 0) as longitude, " +
                    "COALESCE(JSON_EXTRACT(c.location_info, '$.latitude'), 0) as latitude, " +
                    "COALESCE(JSON_EXTRACT(p.basic_info, '$.build_year'), 2000) as build_year " +
                    "FROM properties p " +
                    "JOIN communities c ON p.community_id = c.community_id " +
                    "WHERE p.status = 'for_sale'";
        
        List<Map<String, Object>> properties = jdbcTemplate.queryForList(sql);
        
        FileSystem fs = FileSystem.get(conf);
        Path hdfsPath = new Path(path);
        fs.mkdirs(hdfsPath.getParent());
        
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(fs.create(hdfsPath, true)))) {
            
            for (Map<String, Object> p : properties) {
                StringBuilder sb = new StringBuilder();
                sb.append(p.get("property_id")).append("|");
                sb.append(p.get("total_price")).append("|");
                sb.append(p.get("unit_price")).append("|");
                sb.append(p.get("area")).append("|");
                sb.append(p.get("bedroom_count")).append("|");
                sb.append(p.get("floor")).append("|");
                sb.append(p.get("total_floors")).append("|");
                sb.append(p.get("longitude")).append("|");
                sb.append(p.get("latitude")).append("|");
                sb.append(p.get("build_year"));
                
                writer.write(sb.toString());
                writer.newLine();
            }
        }
        
        System.out.println("已导出 " + properties.size() + " 条房源数据到HDFS: " + path);
    }
    
    /**
     * 从HDFS导入相似度结果
     */
    private void importSimilarityFromHDFS(Configuration conf, String path) throws IOException {
        FileSystem fs = FileSystem.get(conf);
        Path hdfsPath = new Path(path);
        
        String insertSql = "INSERT INTO property_similarity " +
                          "(property_id1, property_id2, similarity_data, created_at, updated_at) " +
                          "VALUES (?, ?, ?, NOW(), NOW()) " +
                          "ON DUPLICATE KEY UPDATE similarity_data = VALUES(similarity_data), updated_at = NOW()";
        
        List<Object[]> batchArgs = new ArrayList<>();
        int imported = 0;
        
        org.apache.hadoop.fs.FileStatus[] fileStatuses = fs.listStatus(hdfsPath);
        
        for (org.apache.hadoop.fs.FileStatus status : fileStatuses) {
            if (status.getPath().getName().startsWith("part-")) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(fs.open(status.getPath())))) {
                    
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split("\t");
                        if (parts.length == 2) {
                            String[] ids = parts[0].split(",");
                            int prop1 = Integer.parseInt(ids[0]);
                            int prop2 = Integer.parseInt(ids[1]);
                            
                            batchArgs.add(new Object[]{prop1, prop2, parts[1]});
                            imported++;
                            
                            if (batchArgs.size() >= BATCH_SIZE) {
                                jdbcTemplate.batchUpdate(insertSql, batchArgs);
                                batchArgs.clear();
                            }
                        }
                    }
                }
            }
        }
        
        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(insertSql, batchArgs);
        }
        
        System.out.println("从HDFS导入了 " + imported + " 条相似度数据");
    }
    
    // ==================== MapReduce内部类 ====================
    
    /**
     * Mapper: 解析房源特征并广播
     */
    public static class SimilarityMapper extends Mapper<LongWritable, Text, Text, Text> {
        
        @Override
        protected void map(LongWritable key, Text value, Context context) 
                throws IOException, InterruptedException {
            String line = value.toString();
            String[] parts = line.split("\\|");
            
            if (parts.length >= 10) {
                // 将所有房源数据发送到同一个Reducer进行比较
                context.write(new Text("ALL"), value);
            }
        }
    }
    
    /**
     * Reducer: 计算两两相似度
     */
    public static class SimilarityReducer extends Reducer<Text, Text, Text, Text> {
        
        private static final double THRESHOLD = 0.3;
        
        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) 
                throws IOException, InterruptedException {
            
            List<String[]> properties = new ArrayList<>();
            
            for (Text value : values) {
                properties.add(value.toString().split("\\|"));
            }
            
            System.out.println("Reducer收到 " + properties.size() + " 条房源数据");
            
            // 两两计算相似度
            for (int i = 0; i < properties.size(); i++) {
                for (int j = i + 1; j < properties.size(); j++) {
                    String[] p1 = properties.get(i);
                    String[] p2 = properties.get(j);
                    
                    double similarity = calculateSimilarity(p1, p2);
                    
                    if (similarity > THRESHOLD) {
                        String outKey = p1[0] + "," + p2[0];
                        
                        JSONObject data = new JSONObject();
                        data.put("similarity_score", Math.round(similarity * 10000) / 10000.0);
                        data.put("algorithm", "mapreduce_cosine");
                        data.put("calculated_by", "hadoop_mr");
                        
                        context.write(new Text(outKey), new Text(data.toJSONString()));
                    }
                }
            }
        }
        
        /**
         * 计算两个房源的相似度
         */
        private double calculateSimilarity(String[] p1, String[] p2) {
            try {
                // 提取特征向量
                double[] v1 = new double[9];
                double[] v2 = new double[9];
                
                for (int i = 1; i <= 9; i++) {
                    v1[i-1] = Double.parseDouble(p1[i]);
                    v2[i-1] = Double.parseDouble(p2[i]);
                }
                
                // 归一化
                normalize(v1);
                normalize(v2);
                
                // 计算余弦相似度
                double dotProduct = 0.0;
                double norm1 = 0.0;
                double norm2 = 0.0;
                
                for (int i = 0; i < v1.length; i++) {
                    dotProduct += v1[i] * v2[i];
                    norm1 += v1[i] * v1[i];
                    norm2 += v2[i] * v2[i];
                }
                
                if (norm1 == 0.0 || norm2 == 0.0) {
                    return 0.0;
                }
                
                return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
                
            } catch (Exception e) {
                return 0.0;
            }
        }
        
        /**
         * 简单的Min-Max归一化
         */
        private void normalize(double[] v) {
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            
            for (double val : v) {
                min = Math.min(min, val);
                max = Math.max(max, val);
            }
            
            double range = max - min;
            if (range > 0) {
                for (int i = 0; i < v.length; i++) {
                    v[i] = (v[i] - min) / range;
                }
            }
        }
    }
    
    // ==================== 层次聚类（Bisecting K-Means）====================
    
    /**
     * 使用Bisecting K-Means进行层次聚类
     * 适合需要层次化分组的场景
     */
    @Transactional
    public void calculateHierarchicalClustering() {
        System.out.println("========================================");
        System.out.println("执行Bisecting K-Means层次聚类...");
        System.out.println("========================================");
        
        // 这个方法需要SparkSession，与主服务配合使用
        // 在实际使用中，可以通过注入SparkSession来实现
        System.out.println("层次聚类功能需要配合PropertySimilarityService使用");
    }
    
    /**
     * 融合多种相似度计算结果
     * 将内容相似度和协同过滤相似度进行加权融合
     */
    @Transactional
    public void mergeSimilarityResults() {
        System.out.println("========================================");
        System.out.println("融合多种相似度计算结果...");
        System.out.println("========================================");
        
        // 更新已存在的相似度记录，融合不同算法的结果
        String sql = "UPDATE property_similarity SET " +
                    "similarity_data = JSON_SET(similarity_data, " +
                    "'$.merged_score', " +
                    "ROUND((" +
                    "  COALESCE(CAST(JSON_EXTRACT(similarity_data, '$.similarity_score') AS DECIMAL(8,4)), 0) * 0.7 + " +
                    "  COALESCE(CAST(JSON_EXTRACT(similarity_data, '$.cf_similarity') AS DECIMAL(8,4)), 0) * 0.3" +
                    "), 4)), " +
                    "updated_at = NOW() " +
                    "WHERE JSON_EXTRACT(similarity_data, '$.cf_similarity') IS NOT NULL";
        
        int updated = jdbcTemplate.update(sql);
        System.out.println("融合完成，更新了 " + updated + " 条记录");
    }
}