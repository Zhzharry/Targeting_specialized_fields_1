package com.example.service;

import org.springframework.stereotype.Service;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.*;
import org.apache.spark.ml.linalg.Matrix;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.stat.Correlation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import java.sql.Timestamp;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Service
public class UserSimilarityService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private SparkSession sparkSession;
    
    /**
     * 基于用户行为计算用户相似度（协同过滤）
     * 
     */
    public void calculateUserSimilarityCF() {
        // 1. 从数据库获取用户行为数据
        String sql = "SELECT user_id, property_id, " +
                    "COALESCE(JSON_EXTRACT(behavior_data, '$.duration'), 0) as duration, " +
                    "COALESCE(JSON_EXTRACT(behavior_data, '$.count'), 1) as view_count " +
                    "FROM browsing_history " +
                    "WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
        
        List<Map<String, Object>> behaviorData = jdbcTemplate.queryForList(sql);
        
        if (behaviorData.isEmpty()) {
            System.out.println("没有用户行为数据");
            return;
        }
        
        // 2. 转换为Spark DataFrame
        List<Row> rows = new ArrayList<>();
        for (Map<String, Object> row : behaviorData) {
            rows.add(RowFactory.create(
                row.get("user_id").toString(),
                row.get("property_id").toString(),
                Double.parseDouble(row.get("duration").toString()),
                Double.parseDouble(row.get("view_count").toString())
            ));
        }
        
        // 创建schema
        StructType schema = DataTypes.createStructType(Arrays.asList(
            DataTypes.createStructField("user_id", 
                DataTypes.StringType, false),
            DataTypes.createStructField("property_id", 
                DataTypes.StringType, false),
            DataTypes.createStructField("duration", 
                DataTypes.DoubleType, false),
            DataTypes.createStructField("view_count", 
                DataTypes.DoubleType, false)
        ));
        
        Dataset<Row> behaviorDF = sparkSession.createDataFrame(rows, schema);
        
        // 3. 构建用户-房源评分矩阵
        Dataset<Row> userItemMatrix = behaviorDF
            .withColumn("score", 
                functions.col("duration").multiply(0.3)
                .plus(functions.col("view_count").multiply(0.7)))
            .groupBy("user_id", "property_id")
            .agg(functions.sum("score").alias("score"))
            .groupBy("user_id")
            .pivot("property_id")
            .agg(functions.first("score"))
            .na().fill(0.0);
        
        // 检查是否有足够的数据
        if (userItemMatrix.count() < 2) {
            System.out.println("用户数量不足，无法计算相似度");
            return;
        }
        
        // 4. 转换为特征向量
        String[] featureCols = userItemMatrix.columns();
        featureCols = Arrays.copyOfRange(featureCols, 1, featureCols.length);
        
        if (featureCols.length == 0) {
            System.out.println("没有房源特征数据");
            return;
        }
        
        VectorAssembler assembler = new VectorAssembler()
            .setInputCols(featureCols)
            .setOutputCol("features");
        
        Dataset<Row> featureDF = assembler.transform(userItemMatrix);
        
        // 5. 计算相关性（使用皮尔逊相关系数）
        Row corrRow = Correlation.corr(featureDF, "features", "pearson").head();
        Matrix corrMatrix = (Matrix) corrRow.get(0);
        
        // 6. 获取用户ID列表
        List<String> userIds = userItemMatrix.select("user_id")
            .as(org.apache.spark.sql.Encoders.STRING())
            .collectAsList();
        
        // 7. 保存相似度到数据库
        saveUserSimilarities(userIds, corrMatrix);
    }
    
    /**
     * 基于用户偏好计算用户相似度（基于内容）
     */
    public void calculateUserSimilarityContent() {
        // 1. 获取用户偏好数据
        String sql = "SELECT u.user_id, up.preference_data " +
                    "FROM users u " +
                    "LEFT JOIN user_preferences up ON u.user_id = up.user_id " +
                    "WHERE up.preference_data IS NOT NULL";
        
        List<Map<String, Object>> preferences = jdbcTemplate.queryForList(sql);
        
        if (preferences.isEmpty()) {
            System.out.println("没有用户偏好数据");
            return;
        }
        
        // 2. 提取特征向量
        Map<Integer, Map<String, Double>> userFeatures = new HashMap<>();
        
        for (Map<String, Object> pref : preferences) {
            Integer userId = (Integer) pref.get("user_id");
            String preferenceData = (String) pref.get("preference_data");
            
            // 解析JSON并提取特征（这里简化处理）
            Map<String, Double> features = extractFeaturesFromPreferences(preferenceData);
            userFeatures.put(userId, features);
        }
        
        // 3. 计算用户间相似度
        List<Integer> userIds = new ArrayList<>(userFeatures.keySet());
        
        for (int i = 0; i < userIds.size(); i++) {
            for (int j = i + 1; j < userIds.size(); j++) {
                Integer user1 = userIds.get(i);
                Integer user2 = userIds.get(j);
                
                double similarity = calculateCosineSimilarity(
                    userFeatures.get(user1), 
                    userFeatures.get(user2)
                );
                
                // 4. 保存到数据库
                if (similarity > 0.1) { // 只保存有意义的相似度
                    saveUserSimilarity(user1, user2, similarity);
                }
            }
        }
    }
    
    private Map<String, Double> extractFeaturesFromPreferences(String preferenceData) {
        Map<String, Double> features = new HashMap<>();
        
        try {
            // 这里简化处理，实际应该用JSON解析器
            // 提取价格偏好
            features.put("price_min", extractDoubleValue(preferenceData, "min", "price_range"));
            features.put("price_max", extractDoubleValue(preferenceData, "max", "price_range"));
            
            // 提取面积偏好
            features.put("area_min", extractDoubleValue(preferenceData, "min", "area_range"));
            features.put("area_max", extractDoubleValue(preferenceData, "max", "area_range"));
            
            // 提取卧室数量偏好
            features.put("bedroom_min", extractDoubleValue(preferenceData, "min", "bedroom_range"));
            features.put("bedroom_max", extractDoubleValue(preferenceData, "max", "bedroom_range"));
            
            // 添加默认值防止空值
            if (features.get("price_min") == 0) features.put("price_min", 200.0);
            if (features.get("price_max") == 0) features.put("price_max", 800.0);
            if (features.get("area_min") == 0) features.put("area_min", 60.0);
            if (features.get("area_max") == 0) features.put("area_max", 150.0);
            if (features.get("bedroom_min") == 0) features.put("bedroom_min", 1.0);
            if (features.get("bedroom_max") == 0) features.put("bedroom_max", 4.0);
            
        } catch (Exception e) {
            System.err.println("解析用户偏好数据失败: " + e.getMessage());
            // 设置默认特征值
            features.put("price_min", 200.0);
            features.put("price_max", 800.0);
            features.put("area_min", 60.0);
            features.put("area_max", 150.0);
            features.put("bedroom_min", 1.0);
            features.put("bedroom_max", 4.0);
        }
        
        return features;
    }
    
    private double extractDoubleValue(String json, String key, String parent) {
        if (json == null) return 0.0;
        Pattern pattern = Pattern.compile("\"" + parent + "\":\\s*\\{[^}]*\"" + key + "\":\\s*(\\d+\\.?\\d*)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return 0.0;
    }
    
    private double calculateCosineSimilarity(Map<String, Double> vec1, Map<String, Double> vec2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        // 获取所有特征键
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(vec1.keySet());
        allKeys.addAll(vec2.keySet());
        
        for (String key : allKeys) {
            double v1 = vec1.getOrDefault(key, 0.0);
            double v2 = vec2.getOrDefault(key, 0.0);
            
            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    private void saveUserSimilarities(List<String> userIds, Matrix correlation) {
        // 清理旧数据
        jdbcTemplate.update("DELETE FROM user_similarity WHERE created_at < DATE_SUB(NOW(), INTERVAL 7 DAY)");
        
        // 批量插入新数据
        String insertSql = "INSERT INTO user_similarity " +
                        "(user_id1, user_id2, similarity_data, created_at, updated_at) " +
                        "VALUES (?, ?, ?, NOW(), NOW()) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "similarity_data = VALUES(similarity_data), " +
                        "updated_at = NOW()";
        
        List<Object[]> batchArgs = new ArrayList<>();
        
        // 获取相关系数矩阵 - 修正这里：Matrix.toArray() 返回的是 double[] 不是 double[][]
        int numRows = correlation.numRows();
        int numCols = correlation.numCols();
        double[] corrValues = correlation.toArray(); // 这是一个一维数组
        
        System.out.println("相关系数矩阵维度: " + numRows + " x " + numCols);
        
        // 将一维数组按二维矩阵的方式访问
        for (int i = 0; i < numRows; i++) {
            for (int j = i + 1; j < numCols; j++) {
                // 在一维数组中的索引：i * numCols + j
                int index = i * numCols + j;
                if (index < corrValues.length) {
                    double similarity = corrValues[index];
                    
                    if (!Double.isNaN(similarity) && similarity > 0.1) {
                        try {
                            int user1 = Integer.parseInt(userIds.get(i));
                            int user2 = Integer.parseInt(userIds.get(j));
                            
                            String similarityData = String.format(
                                "{\"similarity_score\": %.4f, \"algorithm\": \"pearson_cf\", " +
                                "\"features_used\": [\"browsing_duration\", \"view_count\"]}",
                                similarity
                            );
                            
                            // 确保user_id1 < user_id2以保持一致性
                            int minId = Math.min(user1, user2);
                            int maxId = Math.max(user1, user2);
                            
                            batchArgs.add(new Object[]{minId, maxId, similarityData});
                        } catch (Exception e) {
                            System.err.println("处理相似度数据错误: " + e.getMessage());
                        }
                    }
                }
            }
        }
        
        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(insertSql, batchArgs);
            System.out.println("保存了 " + batchArgs.size() + " 条用户相似度记录");
        } else {
            System.out.println("没有找到有意义的相似度数据");
        }
    }
    
    private void saveUserSimilarity(int user1, int user2, double similarity) {
        String sql = "INSERT INTO user_similarity " +
                    "(user_id1, user_id2, similarity_data, created_at, updated_at) " +
                    "VALUES (?, ?, ?, NOW(), NOW()) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "similarity_data = VALUES(similarity_data), " +
                    "updated_at = NOW()";
        
        String similarityData = String.format(
            "{\"similarity_score\": %.4f, \"algorithm\": \"cosine_content\", " +
            "\"features_used\": [\"price_range\", \"area_range\", \"bedroom_range\"]}",
            similarity
        );
        
        // 确保user_id1 < user_id2以保持一致性
        int minId = Math.min(user1, user2);
        int maxId = Math.max(user1, user2);
        
        jdbcTemplate.update(sql, minId, maxId, similarityData);
    }
    
    /**
     * 综合计算用户相似度（结合协同过滤和基于内容的方法）
     */
    public void calculateUserSimilarityComprehensive() {
        System.out.println("开始计算用户相似度...");
        
        // 先计算基于内容的相似度
        try {
            calculateUserSimilarityContent();
            System.out.println("基于内容相似度计算完成");
        } catch (Exception e) {
            System.err.println("基于内容计算失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 再计算协同过滤相似度
        try {
            calculateUserSimilarityCF();
            System.out.println("协同过滤相似度计算完成");
        } catch (Exception e) {
            System.err.println("协同过滤计算失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("用户相似度计算全部完成");
    }
}