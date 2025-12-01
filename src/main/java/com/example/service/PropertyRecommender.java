package com.example.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Service
public class PropertyRecommender {
    
    @Autowired
    private DataSource dataSource;
    
    /**
     * 猜你喜欢 - 综合推荐算法
     */
    public List<Integer> guessYouLike(int userId, int limit) {
        List<Integer> recommendations = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection()) {
            // 1. 基于用户偏好的内容推荐 (权重50%)
            List<Integer> contentBasedRecs = recommendByPreference(conn, userId, (int)(limit * 0.5));
            
            // 2. 基于协同过滤的推荐 (权重30%)
            List<Integer> cfRecs = recommendByCollaborativeFiltering(conn, userId, (int)(limit * 0.3));
            
            // 3. 基于热门度的推荐 (权重20%)
            List<Integer> popularRecs = recommendByPopularity(conn, (int)(limit * 0.2));
            
            // 合并并去重
            Set<Integer> allRecs = new LinkedHashSet<>();
            allRecs.addAll(contentBasedRecs);
            allRecs.addAll(cfRecs);
            allRecs.addAll(popularRecs);
            
            recommendations = new ArrayList<>(allRecs);
            if (recommendations.size() > limit) {
                recommendations = recommendations.subList(0, limit);
            }
            
            // 保存推荐结果
            saveRecommendations(conn, userId, recommendations, "guess_you_like");
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return recommendations;
    }
    
    /**
     * 基于用户偏好的内容推荐
     */
    public List<Integer> recommendByPreference(int userId, int limit) {
        try (Connection conn = dataSource.getConnection()) {
            return recommendByPreference(conn, userId, limit);
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    private List<Integer> recommendByPreference(Connection conn, int userId, int limit) {
        List<Integer> recommendations = new ArrayList<>();
        
        try {
            JSONObject preference = getUserPreference(conn, userId);
            if (preference == null) {
                return recommendations;
            }
            
            StringBuilder sql = new StringBuilder("""
                SELECT p.property_id, p.title, p.price_info, p.layout_info,
                       c.name as community_name, c.location_info
                FROM properties p
                JOIN communities c ON p.community_id = c.community_id
                WHERE p.status = 'for_sale'
                """);
            
            List<Object> params = new ArrayList<>();
            
            // 构建基于偏好的筛选条件
            buildPreferenceFilters(sql, params, preference);
            
            sql.append(" ORDER BY p.favorite_count DESC, p.view_count DESC LIMIT ?");
            params.add(limit);
            
            recommendations = executePropertyQuery(conn, sql.toString(), params);
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return recommendations;
    }
    
    /**
     * 构建偏好筛选条件
     */
    private void buildPreferenceFilters(StringBuilder sql, List<Object> params, JSONObject preference) {
        // 价格范围筛选
        JSONObject priceRange = preference.getJSONObject("price_range");
        if (priceRange != null) {
            sql.append(" AND JSON_EXTRACT(p.price_info, '$.total_price') BETWEEN ? AND ?");
            params.add(priceRange.getDouble("min"));
            params.add(priceRange.getDouble("max"));
        }
        
        // 面积范围筛选
        JSONObject areaRange = preference.getJSONObject("area_range");
        if (areaRange != null) {
            sql.append(" AND JSON_EXTRACT(p.layout_info, '$.area') BETWEEN ? AND ?");
            params.add(areaRange.getDouble("min"));
            params.add(areaRange.getDouble("max"));
        }
        
        // 卧室数量筛选
        JSONObject bedroomRange = preference.getJSONObject("bedroom_range");
        if (bedroomRange != null) {
            sql.append(" AND JSON_EXTRACT(p.layout_info, '$.bedroom_count') BETWEEN ? AND ?");
            params.add(bedroomRange.getInteger("min"));
            params.add(bedroomRange.getInteger("max"));
        }
        
        // 位置筛选
        JSONArray locations = preference.getJSONArray("locations");
        if (locations != null && !locations.isEmpty()) {
            sql.append(" AND (");
            for (int i = 0; i < locations.size(); i++) {
                if (i > 0) sql.append(" OR ");
                sql.append("JSON_EXTRACT(c.location_info, '$.district') LIKE ?");
                params.add("%" + locations.getString(i) + "%");
            }
            sql.append(")");
        }
        
        // 朝向筛选
        JSONArray orientations = preference.getJSONArray("orientations");
        if (orientations != null && !orientations.isEmpty()) {
            sql.append(" AND (");
            for (int i = 0; i < orientations.size(); i++) {
                if (i > 0) sql.append(" OR ");
                sql.append("JSON_EXTRACT(p.layout_info, '$.orientation') = ?");
                params.add(orientations.getString(i));
            }
            sql.append(")");
        }
    }
    
    /**
     * 基于协同过滤的推荐
     */
    public List<Integer> recommendByCollaborativeFiltering(int userId, int limit) {
        try (Connection conn = dataSource.getConnection()) {
            return recommendByCollaborativeFiltering(conn, userId, limit);
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    private List<Integer> recommendByCollaborativeFiltering(Connection conn, int userId, int limit) {
        List<Integer> recommendations = new ArrayList<>();
        
        try {
            // 找到相似用户
            String similarUsersSql = """
                SELECT user_id2, similarity_score
                FROM user_similarity 
                WHERE user_id1 = ? AND similarity_score > 0.5
                ORDER BY similarity_score DESC
                LIMIT 10
                """;
            
            List<Integer> similarUserIds = new ArrayList<>();
            try (PreparedStatement pstmt = conn.prepareStatement(similarUsersSql)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    similarUserIds.add(rs.getInt("user_id2"));
                }
            }
            
            if (similarUserIds.isEmpty()) {
                return recommendations;
            }
            
            // 获取相似用户交互过的房源
            String placeholders = String.join(",", Collections.nCopies(similarUserIds.size(), "?"));
            String sql = String.format("""
                SELECT DISTINCT p.property_id, 
                       COUNT(bh.history_id) as view_count,
                       COUNT(f.favorite_id) as favorite_count
                FROM properties p
                LEFT JOIN browsing_history bh ON p.property_id = bh.property_id AND bh.user_id IN (%s)
                LEFT JOIN favorites f ON p.property_id = f.property_id AND f.user_id IN (%s)
                WHERE p.status = 'for_sale'
                AND p.property_id NOT IN (
                    SELECT property_id FROM browsing_history WHERE user_id = ?
                    UNION
                    SELECT property_id FROM favorites WHERE user_id = ?
                )
                GROUP BY p.property_id
                ORDER BY (COUNT(bh.history_id) * 0.6 + COUNT(f.favorite_id) * 0.4) DESC
                LIMIT ?
                """, placeholders, placeholders);
            
            List<Object> params = new ArrayList<>();
            params.addAll(similarUserIds);
            params.addAll(similarUserIds);
            params.add(userId);
            params.add(userId);
            params.add(limit);
            
            recommendations = executePropertyQuery(conn, sql, params);
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return recommendations;
    }
    
    /**
     * 基于热门度的推荐
     */
    public List<Integer> recommendByPopularity(int limit) {
        try (Connection conn = dataSource.getConnection()) {
            return recommendByPopularity(conn, limit);
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    private List<Integer> recommendByPopularity(Connection conn, int limit) {
        List<Integer> recommendations = new ArrayList<>();
        
        try {
            String sql = """
                SELECT property_id
                FROM properties
                WHERE status = 'for_sale'
                ORDER BY (favorite_count * 0.4 + view_count * 0.3 + 
                         (SELECT COUNT(*) FROM browsing_history bh WHERE bh.property_id = properties.property_id) * 0.3) DESC
                LIMIT ?
                """;
            
            List<Object> params = Arrays.asList(limit);
            recommendations = executePropertyQuery(conn, sql, params);
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return recommendations;
    }
    
    /**
     * 相似房源推荐
     */
    public List<Integer> recommendSimilarProperties(int propertyId, int limit) {
        List<Integer> recommendations = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection()) {
            // 获取当前房源信息
            String propertySql = """
                SELECT p.price_info, p.layout_info, c.location_info
                FROM properties p
                JOIN communities c ON p.community_id = c.community_id
                WHERE p.property_id = ?
                """;
            
            JSONObject priceInfo = null;
            JSONObject layoutInfo = null;
            JSONObject locationInfo = null;
            
            try (PreparedStatement pstmt = conn.prepareStatement(propertySql)) {
                pstmt.setInt(1, propertyId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    priceInfo = JSONObject.parseObject(rs.getString("price_info"));
                    layoutInfo = JSONObject.parseObject(rs.getString("layout_info"));
                    locationInfo = JSONObject.parseObject(rs.getString("location_info"));
                }
            }
            
            if (priceInfo == null || layoutInfo == null) {
                return recommendations;
            }
            
            // 基于房源特征推荐相似房源
            String sql = """
                SELECT p.property_id,
                       ABS(JSON_EXTRACT(p.price_info, '$.total_price') - ?) / ? as price_similarity,
                       ABS(JSON_EXTRACT(p.layout_info, '$.area') - ?) / ? as area_similarity,
                       CASE WHEN JSON_EXTRACT(p.layout_info, '$.bedroom_count') = ? THEN 1 ELSE 0 END as bedroom_match,
                       CASE WHEN JSON_EXTRACT(c.location_info, '$.district') = ? THEN 1 ELSE 0 END as location_match
                FROM properties p
                JOIN communities c ON p.community_id = c.community_id
                WHERE p.status = 'for_sale' AND p.property_id != ?
                ORDER BY (price_similarity * 0.3 + area_similarity * 0.2 + bedroom_match * 0.3 + location_match * 0.2) DESC
                LIMIT ?
                """;
            
            double currentPrice = priceInfo.getDoubleValue("total_price");
            double currentArea = layoutInfo.getDoubleValue("area");
            int currentBedroom = layoutInfo.getIntValue("bedroom_count");
            String currentDistrict = locationInfo != null ? locationInfo.getString("district") : "";
            
            List<Object> params = Arrays.asList(
                currentPrice, currentPrice, currentArea, currentArea,
                currentBedroom, currentDistrict, propertyId, limit
            );
            
            recommendations = executePropertyQuery(conn, sql, params);
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return recommendations;
    }
    
    /**
     * 其他用户也在看
     */
    public List<Integer> otherUsersAlsoViewed(int userId, int limit) {
        List<Integer> recommendations = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection()) {
            // 找到相似用户
            String similarUsersSql = """
                SELECT user_id2 
                FROM user_similarity 
                WHERE user_id1 = ? AND similarity_score > 0.6
                ORDER BY similarity_score DESC
                LIMIT 5
                """;
            
            List<Integer> similarUserIds = new ArrayList<>();
            try (PreparedStatement pstmt = conn.prepareStatement(similarUsersSql)) {
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    similarUserIds.add(rs.getInt("user_id2"));
                }
            }
            
            if (similarUserIds.isEmpty()) {
                return recommendations;
            }
            
            // 获取相似用户最近浏览的房源
            String placeholders = String.join(",", Collections.nCopies(similarUserIds.size(), "?"));
            String sql = String.format("""
                SELECT DISTINCT p.property_id
                FROM properties p
                JOIN browsing_history bh ON p.property_id = bh.property_id
                WHERE bh.user_id IN (%s)
                AND p.property_id NOT IN (
                    SELECT property_id FROM browsing_history WHERE user_id = ?
                )
                AND p.status = 'for_sale'
                ORDER BY bh.created_at DESC
                LIMIT ?
                """, placeholders);
            
            List<Object> params = new ArrayList<>();
            params.addAll(similarUserIds);
            params.add(userId);
            params.add(limit);
            
            recommendations = executePropertyQuery(conn, sql, params);
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return recommendations;
    }
    
    // 辅助方法
    private List<Integer> executePropertyQuery(Connection conn, String sql, List<Object> params) throws SQLException {
        List<Integer> propertyIds = new ArrayList<>();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                propertyIds.add(rs.getInt("property_id"));
            }
        }
        
        return propertyIds;
    }
    
    private JSONObject getUserPreference(Connection conn, int userId) throws SQLException {
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
    
    private void saveRecommendations(Connection conn, int userId, List<Integer> propertyIds, String recommendationType) throws SQLException {
        String sql = """
            INSERT INTO user_recommendations (user_id, property_id, recommendation_data, created_at)
            VALUES (?, ?, ?, NOW())
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int propertyId : propertyIds) {
                JSONObject recData = new JSONObject();
                recData.put("recommendation_type", recommendationType);
                recData.put("score", 0.8);
                recData.put("reason", "基于您的偏好和行为推荐");
                recData.put("is_viewed", false);
                recData.put("is_clicked", false);
                recData.put("generated_at", new java.util.Date().toString());
                
                pstmt.setInt(1, userId);
                pstmt.setInt(2, propertyId);
                pstmt.setString(3, recData.toJSONString());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }
}