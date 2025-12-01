// UserSimilarityCalculator.java
package com.example.service;

import com.example.service.config.DatabaseConfig;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.sql.*;
import java.util.*;

public class UserSimilarityCalculator {
    private static final double SIMILARITY_THRESHOLD = 0.3;
    
    /**
     * 计算两个用户的综合相似度
     */
    public double calculateUserSimilarity(int userId1, int userId2) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            // 1. 偏好相似度 (权重40%)
            double preferenceSimilarity = calculatePreferenceSimilarity(conn, userId1, userId2);
            
            // 2. 行为相似度 (权重35%)
            double behaviorSimilarity = calculateBehaviorSimilarity(conn, userId1, userId2);
            
            // 3. 收藏相似度 (权重25%)
            double favoriteSimilarity = calculateFavoriteSimilarity(conn, userId1, userId2);
            
            // 加权计算总相似度
            return 0.4 * preferenceSimilarity + 0.35 * behaviorSimilarity + 0.25 * favoriteSimilarity;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return 0.0;
        }
    }
    
    /**
     * 基于用户偏好的相似度计算
     */
    private double calculatePreferenceSimilarity(Connection conn, int userId1, int userId2) throws SQLException {
        JSONObject pref1 = getUserPreferenceData(conn, userId1);
        JSONObject pref2 = getUserPreferenceData(conn, userId2);
        
        if (pref1 == null || pref2 == null) {
            return 0.0;
        }
        
        Map<String, Double> vector1 = buildPreferenceVector(pref1);
        Map<String, Double> vector2 = buildPreferenceVector(pref2);
        
        return calculateCosineSimilarity(vector1, vector2);
    }
    
    /**
     * 构建偏好特征向量
     */
    private Map<String, Double> buildPreferenceVector(JSONObject preferenceData) {
        Map<String, Double> vector = new HashMap<>();
        
        // 价格范围特征
        JSONObject priceRange = preferenceData.getJSONObject("price_range");
        if (priceRange != null) {
            double priceAvg = (priceRange.getDoubleValue("min") + priceRange.getDoubleValue("max")) / 2;
            vector.put("price", priceAvg / 1000.0); // 归一化
        }
        
        // 面积范围特征
        JSONObject areaRange = preferenceData.getJSONObject("area_range");
        if (areaRange != null) {
            double areaAvg = (areaRange.getDoubleValue("min") + areaRange.getDoubleValue("max")) / 2;
            vector.put("area", areaAvg / 200.0); // 归一化
        }
        
        // 卧室数量特征
        JSONObject bedroomRange = preferenceData.getJSONObject("bedroom_range");
        if (bedroomRange != null) {
            double bedroomAvg = (bedroomRange.getDoubleValue("min") + bedroomRange.getDoubleValue("max")) / 2;
            vector.put("bedroom", bedroomAvg / 5.0); // 归一化
        }
        
        // 位置偏好 (one-hot编码)
        JSONArray locations = preferenceData.getJSONArray("locations");
        if (locations != null) {
            for (int i = 0; i < locations.size(); i++) {
                vector.put("location_" + locations.getString(i), 1.0);
            }
        }
        
        // 房型偏好 (one-hot编码)
        JSONArray houseTypes = preferenceData.getJSONArray("house_types");
        if (houseTypes != null) {
            for (int i = 0; i < houseTypes.size(); i++) {
                vector.put("house_type_" + houseTypes.getString(i), 1.0);
            }
        }
        
        // 关键词偏好
        JSONArray keywords = preferenceData.getJSONArray("keywords");
        if (keywords != null) {
            for (int i = 0; i < keywords.size(); i++) {
                vector.put("keyword_" + keywords.getString(i), 1.0);
            }
        }
        
        return vector;
    }
    
    /**
     * 计算余弦相似度
     */
    private double calculateCosineSimilarity(Map<String, Double> vector1, Map<String, Double> vector2) {
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(vector1.keySet());
        allKeys.addAll(vector2.keySet());
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (String key : allKeys) {
            double v1 = vector1.getOrDefault(key, 0.0);
            double v2 = vector2.getOrDefault(key, 0.0);
            
            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }
        
        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    /**
     * 基于浏览行为的相似度计算
     */
    private double calculateBehaviorSimilarity(Connection conn, int userId1, int userId2) throws SQLException {
        String sql = """
            SELECT 
                COUNT(DISTINCT CASE WHEN bh1.property_id = bh2.property_id THEN bh1.property_id END) as common_views,
                COUNT(DISTINCT bh1.property_id) as total_views1,
                COUNT(DISTINCT bh2.property_id) as total_views2
            FROM browsing_history bh1
            CROSS JOIN browsing_history bh2 
            WHERE bh1.user_id = ? AND bh2.user_id = ?
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId1);
            pstmt.setInt(2, userId2);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int commonViews = rs.getInt("common_views");
                int totalViews1 = rs.getInt("total_views1");
                int totalViews2 = rs.getInt("total_views2");
                
                if (totalViews1 == 0 && totalViews2 == 0) return 0.0;
                
                // Jaccard相似度
                return (double) commonViews / (totalViews1 + totalViews2 - commonViews);
            }
        }
        return 0.0;
    }
    
    /**
     * 基于收藏行为的相似度计算
     */
    private double calculateFavoriteSimilarity(Connection conn, int userId1, int userId2) throws SQLException {
        String sql = """
            SELECT 
                COUNT(DISTINCT CASE WHEN f1.property_id = f2.property_id THEN f1.property_id END) as common_favorites,
                COUNT(DISTINCT f1.property_id) as total_favorites1,
                COUNT(DISTINCT f2.property_id) as total_favorites2
            FROM favorites f1
            CROSS JOIN favorites f2 
            WHERE f1.user_id = ? AND f2.user_id = ?
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId1);
            pstmt.setInt(2, userId2);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int commonFavorites = rs.getInt("common_favorites");
                int totalFavorites1 = rs.getInt("total_favorites1");
                int totalFavorites2 = rs.getInt("total_favorites2");
                
                if (totalFavorites1 == 0 && totalFavorites2 == 0) return 0.0;
                
                // Jaccard相似度
                return (double) commonFavorites / (totalFavorites1 + totalFavorites2 - commonFavorites);
            }
        }
        return 0.0;
    }
    
    /**
     * 获取用户偏好数据
     */
    private JSONObject getUserPreferenceData(Connection conn, int userId) throws SQLException {
        String sql = "SELECT preference_data FROM user_preferences WHERE user_id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String preferenceJson = rs.getString("preference_data");
                return preferenceJson != null ? JSONObject.parseObject(preferenceJson) : null;
            }
        }
        return null;
    }
    
    /**
     * 批量计算并保存用户相似度
     */
    public void calculateAndSaveAllUserSimilarities() {
        try (Connection conn = DatabaseConfig.getConnection()) {
            // 获取所有用户ID
            List<Integer> userIds = getAllUserIds(conn);
            
            // 计算每对用户的相似度
            for (int i = 0; i < userIds.size(); i++) {
                for (int j = i + 1; j < userIds.size(); j++) {
                    int userId1 = userIds.get(i);
                    int userId2 = userIds.get(j);
                    
                    double similarity = calculateUserSimilarity(userId1, userId2);
                    
                    if (similarity > SIMILARITY_THRESHOLD) {
                        saveUserSimilarity(conn, userId1, userId2, similarity);
                    }
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 保存用户相似度到数据库
     */
    private void saveUserSimilarity(Connection conn, int userId1, int userId2, double similarity) throws SQLException {
        String sql = """
            INSERT INTO user_similarity (user_id1, user_id2, similarity_data, created_at)
            VALUES (?, ?, ?, NOW())
            ON DUPLICATE KEY UPDATE similarity_data = ?, created_at = NOW()
            """;
        
        JSONObject similarityData = new JSONObject();
        similarityData.put("similarity_type", "comprehensive");
        similarityData.put("similarity_score", similarity);
        similarityData.put("algorithm_version", "v2.0");
        similarityData.put("calculated_at", new Date().toString());
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId1);
            pstmt.setInt(2, userId2);
            pstmt.setString(3, similarityData.toJSONString());
            pstmt.setString(4, similarityData.toJSONString());
            pstmt.executeUpdate();
        }
    }
    
    private List<Integer> getAllUserIds(Connection conn) throws SQLException {
        List<Integer> userIds = new ArrayList<>();
        String sql = "SELECT user_id FROM users";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                userIds.add(rs.getInt("user_id"));
            }
        }
        return userIds;
    }
}