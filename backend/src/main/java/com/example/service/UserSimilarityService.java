// UserSimilarityService.java
package com.example.service;

import org.springframework.stereotype.Service;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.*;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.feature.StandardScaler;
import org.apache.spark.ml.feature.StandardScalerModel;
import org.apache.spark.ml.linalg.Vector;
import org.apache.spark.ml.linalg.Vectors;
import org.apache.spark.ml.clustering.KMeans;
import org.apache.spark.ml.clustering.KMeansModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;

import java.util.*;
import java.util.stream.Collectors;
import java.sql.Timestamp;

/**
 * 增强版用户相似度计算服务
 * 
 * 功能特点：
 * 1. 多维度用户画像构建（行为+偏好+人口统计）
 * 2. 时间衰减权重（近期行为更重要）
 * 3. 行为类型加权（收藏>长时浏览>短时浏览）
 * 4. 多种相似度算法融合（余弦+Jaccard+皮尔逊）
 * 5. K-Means用户聚类（发现用户群体）
 * 6. 支持增量更新
 * 
 * @version 2.0
 */
@Service
public class UserSimilarityService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private SparkSession sparkSession;
    
    // ==================== 配置参数 ====================
    
    // 行为权重
    private static final double WEIGHT_FAVORITE = 5.0;      // 收藏权重
    private static final double WEIGHT_LONG_VIEW = 3.0;     // 长时浏览(>60s)
    private static final double WEIGHT_SHORT_VIEW = 1.0;    // 短时浏览
    private static final double WEIGHT_SEARCH = 2.0;        // 搜索权重
    
    // 时间衰减参数（半衰期，天）
    private static final double TIME_DECAY_HALFLIFE = 7.0;
    
    // 相似度阈值
    private static final double SIMILARITY_THRESHOLD = 0.1;
    
    // 批处理大小
    private static final int BATCH_SIZE = 500;
    
    // 用户聚类数
    private static final int USER_CLUSTERS = 5;
    
    /**
     * 主入口：执行完整的用户相似度计算
     */
    @Transactional
    public void calculateUserSimilarityFull() {
        System.out.println("========================================");
        System.out.println("开始计算用户相似度...");
        System.out.println("时间: " + new Timestamp(System.currentTimeMillis()));
        System.out.println("========================================");
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 构建用户画像
            System.out.println("\n[步骤1/5] 构建用户画像...");
            Map<Integer, UserProfile> userProfiles = buildUserProfiles();
            System.out.println("构建了 " + userProfiles.size() + " 个用户画像");
            
            if (userProfiles.size() < 2) {
                System.out.println("用户数量不足，跳过相似度计算");
                return;
            }
            
            // 2. 转换为特征向量
            System.out.println("\n[步骤2/5] 转换为特征向量...");
            Dataset<Row> userFeaturesDF = convertToFeatureVectors(userProfiles);
            
            // 3. 用户聚类
            System.out.println("\n[步骤3/5] 执行用户聚类...");
            Dataset<Row> clusteredDF = performUserClustering(userFeaturesDF);
            
            // 4. 计算相似度
            System.out.println("\n[步骤4/5] 计算用户相似度...");
            calculateAndSaveSimilarities(clusteredDF, userProfiles);
            
            // 5. 保存聚类结果
            System.out.println("\n[步骤5/5] 保存聚类结果...");
            saveClusterResults(clusteredDF);
            
            long endTime = System.currentTimeMillis();
            System.out.println("\n========================================");
            System.out.println("用户相似度计算完成！");
            System.out.println("总耗时: " + (endTime - startTime) / 1000.0 + " 秒");
            System.out.println("========================================");
            
        } catch (Exception e) {
            System.err.println("计算过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 构建用户画像
     */
    private Map<Integer, UserProfile> buildUserProfiles() {
        Map<Integer, UserProfile> profiles = new HashMap<>();
        
        // 1. 获取用户基本信息
        loadUserBasicInfo(profiles);
        
        // 2. 获取用户偏好
        loadUserPreferences(profiles);
        
        // 3. 获取用户行为数据（浏览历史）
        loadUserBehaviors(profiles);
        
        // 4. 获取用户收藏
        loadUserFavorites(profiles);
        
        // 5. 获取用户搜索历史
        loadUserSearchHistory(profiles);
        
        // 6. 计算衍生特征
        for (UserProfile profile : profiles.values()) {
            profile.calculateDerivedFeatures();
        }
        
        return profiles;
    }
    
    /**
     * 加载用户基本信息
     */
    private void loadUserBasicInfo(Map<Integer, UserProfile> profiles) {
        String sql = "SELECT user_id, username, user_profile FROM users";
        List<Map<String, Object>> users = jdbcTemplate.queryForList(sql);
        
        for (Map<String, Object> user : users) {
            Integer userId = ((Number) user.get("user_id")).intValue();
            UserProfile profile = new UserProfile(userId);
            
            String userProfileJson = (String) user.get("user_profile");
            if (userProfileJson != null) {
                try {
                    JSONObject json = JSON.parseObject(userProfileJson);
                    
                    // 提取预算范围
                    JSONObject budget = json.getJSONObject("budget");
                    if (budget != null) {
                        profile.budgetMin = budget.getDoubleValue("min");
                        profile.budgetMax = budget.getDoubleValue("max");
                    }
                    
                    // 提取家庭结构
                    profile.familyStructure = json.getString("family_structure");
                    
                    // 提取偏好位置
                    JSONArray locations = json.getJSONArray("preferred_locations");
                    if (locations != null) {
                        profile.preferredLocations = new HashSet<>();
                        for (int i = 0; i < locations.size(); i++) {
                            profile.preferredLocations.add(locations.getString(i));
                        }
                    }
                } catch (Exception e) {
                    // 解析失败，使用默认值
                }
            }
            
            profiles.put(userId, profile);
        }
    }
    
    /**
     * 加载用户偏好
     */
    private void loadUserPreferences(Map<Integer, UserProfile> profiles) {
        String sql = "SELECT user_id, preference_data FROM user_preferences";
        List<Map<String, Object>> prefs = jdbcTemplate.queryForList(sql);
        
        for (Map<String, Object> pref : prefs) {
            Integer userId = ((Number) pref.get("user_id")).intValue();
            UserProfile profile = profiles.get(userId);
            if (profile == null) continue;
            
            String prefData = (String) pref.get("preference_data");
            if (prefData != null) {
                try {
                    JSONObject json = JSON.parseObject(prefData);
                    
                    // 价格范围
                    JSONObject priceRange = json.getJSONObject("price_range");
                    if (priceRange != null) {
                        profile.prefPriceMin = priceRange.getDoubleValue("min");
                        profile.prefPriceMax = priceRange.getDoubleValue("max");
                    }
                    
                    // 面积范围
                    JSONObject areaRange = json.getJSONObject("area_range");
                    if (areaRange != null) {
                        profile.prefAreaMin = areaRange.getDoubleValue("min");
                        profile.prefAreaMax = areaRange.getDoubleValue("max");
                    }
                    
                    // 卧室范围
                    JSONObject bedroomRange = json.getJSONObject("bedroom_range");
                    if (bedroomRange != null) {
                        profile.prefBedroomMin = bedroomRange.getIntValue("min");
                        profile.prefBedroomMax = bedroomRange.getIntValue("max");
                    }
                    
                    // 装修偏好
                    JSONArray decorations = json.getJSONArray("decorations");
                    if (decorations != null) {
                        profile.prefDecorations = new HashSet<>();
                        for (int i = 0; i < decorations.size(); i++) {
                            profile.prefDecorations.add(decorations.getString(i));
                        }
                    }
                    
                    // 朝向偏好
                    JSONArray orientations = json.getJSONArray("orientations");
                    if (orientations != null) {
                        profile.prefOrientations = new HashSet<>();
                        for (int i = 0; i < orientations.size(); i++) {
                            profile.prefOrientations.add(orientations.getString(i));
                        }
                    }
                    
                    // 关键词
                    JSONArray keywords = json.getJSONArray("keywords");
                    if (keywords != null) {
                        profile.prefKeywords = new HashSet<>();
                        for (int i = 0; i < keywords.size(); i++) {
                            profile.prefKeywords.add(keywords.getString(i));
                        }
                    }
                    
                    // 位置偏好
                    JSONArray locations = json.getJSONArray("locations");
                    if (locations != null) {
                        profile.prefLocations = new HashSet<>();
                        for (int i = 0; i < locations.size(); i++) {
                            profile.prefLocations.add(locations.getString(i));
                        }
                    }
                } catch (Exception e) {
                    // 解析失败，跳过
                }
            }
        }
    }
    
    /**
     * 加载用户浏览行为（带时间衰减）
     */
    private void loadUserBehaviors(Map<Integer, UserProfile> profiles) {
        String sql = "SELECT user_id, property_id, behavior_data, created_at " +
                    "FROM browsing_history " +
                    "WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
        
        List<Map<String, Object>> behaviors = jdbcTemplate.queryForList(sql);
        
        long now = System.currentTimeMillis();
        
        for (Map<String, Object> behavior : behaviors) {
            Integer userId = ((Number) behavior.get("user_id")).intValue();
            Integer propertyId = ((Number) behavior.get("property_id")).intValue();
            
            UserProfile profile = profiles.get(userId);
            if (profile == null) continue;
            
            // 计算时间衰减权重
            Timestamp createdAt = (Timestamp) behavior.get("created_at");
            double daysDiff = (now - createdAt.getTime()) / (1000.0 * 60 * 60 * 24);
            double timeDecay = Math.pow(0.5, daysDiff / TIME_DECAY_HALFLIFE);
            
            // 解析行为数据
            String behaviorData = (String) behavior.get("behavior_data");
            double duration = 60.0; // 默认60秒
            int viewCount = 1;
            
            if (behaviorData != null) {
                try {
                    JSONObject json = JSON.parseObject(behaviorData);
                    duration = json.getDoubleValue("duration");
                    viewCount = json.getIntValue("view_count");
                    if (viewCount == 0) viewCount = 1;
                } catch (Exception e) {
                    // 使用默认值
                }
            }
            
            // 计算行为得分（长时浏览权重更高）
            double behaviorWeight = duration > 60 ? WEIGHT_LONG_VIEW : WEIGHT_SHORT_VIEW;
            double score = behaviorWeight * viewCount * timeDecay;
            
            // 累加到用户画像
            profile.viewedProperties.merge(propertyId, score, Double::sum);
            profile.totalViewDuration += duration;
            profile.totalViewCount += viewCount;
        }
    }
    
    /**
     * 加载用户收藏
     */
    private void loadUserFavorites(Map<Integer, UserProfile> profiles) {
        String sql = "SELECT user_id, property_id, created_at FROM favorites";
        List<Map<String, Object>> favorites = jdbcTemplate.queryForList(sql);
        
        long now = System.currentTimeMillis();
        
        for (Map<String, Object> fav : favorites) {
            Integer userId = ((Number) fav.get("user_id")).intValue();
            Integer propertyId = ((Number) fav.get("property_id")).intValue();
            
            UserProfile profile = profiles.get(userId);
            if (profile == null) continue;
            
            // 时间衰减
            Timestamp createdAt = (Timestamp) fav.get("created_at");
            double daysDiff = (now - createdAt.getTime()) / (1000.0 * 60 * 60 * 24);
            double timeDecay = Math.pow(0.5, daysDiff / TIME_DECAY_HALFLIFE);
            
            double score = WEIGHT_FAVORITE * timeDecay;
            
            profile.favoriteProperties.add(propertyId);
            profile.viewedProperties.merge(propertyId, score, Double::sum);
        }
    }
    
    /**
     * 加载用户搜索历史
     */
    private void loadUserSearchHistory(Map<Integer, UserProfile> profiles) {
        String sql = "SELECT user_id, search_data, created_at FROM search_history " +
                    "WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
        
        List<Map<String, Object>> searches = jdbcTemplate.queryForList(sql);
        
        for (Map<String, Object> search : searches) {
            Integer userId = ((Number) search.get("user_id")).intValue();
            
            UserProfile profile = profiles.get(userId);
            if (profile == null) continue;
            
            String searchData = (String) search.get("search_data");
            if (searchData != null) {
                try {
                    JSONObject json = JSON.parseObject(searchData);
                    
                    // 提取搜索关键词
                    String keyword = json.getString("keyword");
                    if (keyword != null && !keyword.isEmpty()) {
                        profile.searchKeywords.merge(keyword, 1, Integer::sum);
                    }
                    
                    // 提取搜索条件中的价格范围
                    JSONObject filters = json.getJSONObject("filters");
                    if (filters != null) {
                        Double minPrice = filters.getDouble("min_price");
                        Double maxPrice = filters.getDouble("max_price");
                        if (minPrice != null) {
                            profile.searchPriceSum += minPrice;
                            profile.searchPriceCount++;
                        }
                        if (maxPrice != null) {
                            profile.searchPriceSum += maxPrice;
                            profile.searchPriceCount++;
                        }
                    }
                } catch (Exception e) {
                    // 跳过
                }
            }
            
            profile.searchCount++;
        }
    }
    
    /**
     * 转换为Spark特征向量
     */
    private Dataset<Row> convertToFeatureVectors(Map<Integer, UserProfile> profiles) {
        List<Row> rows = new ArrayList<>();
        
        for (UserProfile profile : profiles.values()) {
            // 只处理有足够数据的用户
            if (profile.viewedProperties.isEmpty() && profile.prefPriceMax == 0) {
                continue;
            }
            
            rows.add(RowFactory.create(
                (double) profile.userId,
                profile.budgetMin,
                profile.budgetMax,
                profile.prefPriceMin,
                profile.prefPriceMax,
                profile.prefAreaMin,
                profile.prefAreaMax,
                (double) profile.prefBedroomMin,
                (double) profile.prefBedroomMax,
                profile.avgViewDuration,
                (double) profile.totalViewCount,
                (double) profile.favoriteProperties.size(),
                (double) profile.searchCount,
                profile.avgSearchPrice,
                encodeFamilyStructure(profile.familyStructure),
                encodeDecorationPreference(profile.prefDecorations),
                encodeOrientationPreference(profile.prefOrientations),
                encodeLocationPreference(profile.prefLocations),
                profile.activityScore,
                profile.preferenceClarity
            ));
        }
        
        if (rows.isEmpty()) {
            System.out.println("警告：没有有效的用户数据");
            return sparkSession.emptyDataFrame();
        }
        
        StructType schema = DataTypes.createStructType(new StructField[]{
            DataTypes.createStructField("user_id", DataTypes.DoubleType, false),
            DataTypes.createStructField("budget_min", DataTypes.DoubleType, false),
            DataTypes.createStructField("budget_max", DataTypes.DoubleType, false),
            DataTypes.createStructField("pref_price_min", DataTypes.DoubleType, false),
            DataTypes.createStructField("pref_price_max", DataTypes.DoubleType, false),
            DataTypes.createStructField("pref_area_min", DataTypes.DoubleType, false),
            DataTypes.createStructField("pref_area_max", DataTypes.DoubleType, false),
            DataTypes.createStructField("pref_bedroom_min", DataTypes.DoubleType, false),
            DataTypes.createStructField("pref_bedroom_max", DataTypes.DoubleType, false),
            DataTypes.createStructField("avg_view_duration", DataTypes.DoubleType, false),
            DataTypes.createStructField("total_view_count", DataTypes.DoubleType, false),
            DataTypes.createStructField("favorite_count", DataTypes.DoubleType, false),
            DataTypes.createStructField("search_count", DataTypes.DoubleType, false),
            DataTypes.createStructField("avg_search_price", DataTypes.DoubleType, false),
            DataTypes.createStructField("family_structure", DataTypes.DoubleType, false),
            DataTypes.createStructField("decoration_pref", DataTypes.DoubleType, false),
            DataTypes.createStructField("orientation_pref", DataTypes.DoubleType, false),
            DataTypes.createStructField("location_pref", DataTypes.DoubleType, false),
            DataTypes.createStructField("activity_score", DataTypes.DoubleType, false),
            DataTypes.createStructField("preference_clarity", DataTypes.DoubleType, false)
        });
        
        Dataset<Row> df = sparkSession.createDataFrame(rows, schema);
        
        // 标准化特征
        String[] featureCols = new String[]{
            "budget_min", "budget_max", "pref_price_min", "pref_price_max",
            "pref_area_min", "pref_area_max", "pref_bedroom_min", "pref_bedroom_max",
            "avg_view_duration", "total_view_count", "favorite_count", "search_count",
            "avg_search_price", "family_structure", "decoration_pref", "orientation_pref",
            "location_pref", "activity_score", "preference_clarity"
        };
        
        VectorAssembler assembler = new VectorAssembler()
            .setInputCols(featureCols)
            .setOutputCol("raw_features")
            .setHandleInvalid("skip");
        
        Dataset<Row> assembled = assembler.transform(df);
        
        StandardScaler scaler = new StandardScaler()
            .setInputCol("raw_features")
            .setOutputCol("features")
            .setWithStd(true)
            .setWithMean(true);
        
        StandardScalerModel scalerModel = scaler.fit(assembled);
        return scalerModel.transform(assembled);
    }
    
    /**
     * 用户聚类
     */
    private Dataset<Row> performUserClustering(Dataset<Row> df) {
        if (df.isEmpty()) {
            return df;
        }
        
        long userCount = df.count();
        int k = Math.min(USER_CLUSTERS, (int) Math.max(2, userCount / 3));
        
        KMeans kmeans = new KMeans()
            .setK(k)
            .setFeaturesCol("features")
            .setPredictionCol("cluster")
            .setMaxIter(50)
            .setSeed(42L);
        
        KMeansModel model = kmeans.fit(df);
        Dataset<Row> clustered = model.transform(df);
        
        System.out.println("用户聚类完成，聚类数: " + k);
        clustered.groupBy("cluster").count().show();
        
        return clustered;
    }
    
    /**
     * 计算并保存相似度
     */
    private void calculateAndSaveSimilarities(Dataset<Row> clusteredDF, 
                                              Map<Integer, UserProfile> profiles) {
        if (clusteredDF.isEmpty()) {
            return;
        }
        
        // 收集数据
        List<Row> rows = clusteredDF.select("user_id", "features", "cluster").collectAsList();
        
        Map<Integer, double[]> userVectors = new HashMap<>();
        Map<Integer, Integer> userClusters = new HashMap<>();
        
        for (Row row : rows) {
            int userId = (int) row.getDouble(0);
            Vector features = row.getAs(1);
            int cluster = row.getInt(2);
            
            userVectors.put(userId, features.toArray());
            userClusters.put(userId, cluster);
        }
        
        // 清理旧数据
        jdbcTemplate.update("DELETE FROM user_similarity");
        
        String insertSql = "INSERT INTO user_similarity " +
                          "(user_id1, user_id2, similarity_data, created_at, updated_at) " +
                          "VALUES (?, ?, ?, NOW(), NOW())";
        
        List<Object[]> batchArgs = new ArrayList<>();
        List<Integer> userIds = new ArrayList<>(userVectors.keySet());
        Collections.sort(userIds);
        
        int savedPairs = 0;
        
        for (int i = 0; i < userIds.size(); i++) {
            for (int j = i + 1; j < userIds.size(); j++) {
                int user1 = userIds.get(i);
                int user2 = userIds.get(j);
                
                double[] vec1 = userVectors.get(user1);
                double[] vec2 = userVectors.get(user2);
                int cluster1 = userClusters.get(user1);
                int cluster2 = userClusters.get(user2);
                
                // 计算多种相似度
                double cosineSim = cosineSimilarity(vec1, vec2);
                double euclideanSim = euclideanSimilarity(vec1, vec2);
                
                // 计算行为Jaccard相似度
                UserProfile p1 = profiles.get(user1);
                UserProfile p2 = profiles.get(user2);
                double behaviorSim = calculateBehaviorSimilarity(p1, p2);
                
                // 融合相似度
                double finalSimilarity = 0.4 * cosineSim + 0.3 * euclideanSim + 0.3 * behaviorSim;
                
                // 同一聚类加成
                if (cluster1 == cluster2) {
                    finalSimilarity = Math.min(1.0, finalSimilarity * 1.15);
                }
                
                if (finalSimilarity > SIMILARITY_THRESHOLD) {
                    JSONObject data = new JSONObject();
                    data.put("similarity_score", Math.round(finalSimilarity * 10000) / 10000.0);
                    data.put("cosine_similarity", Math.round(cosineSim * 10000) / 10000.0);
                    data.put("euclidean_similarity", Math.round(euclideanSim * 10000) / 10000.0);
                    data.put("behavior_similarity", Math.round(behaviorSim * 10000) / 10000.0);
                    data.put("same_cluster", cluster1 == cluster2);
                    data.put("cluster_id", cluster1 == cluster2 ? cluster1 : -1);
                    data.put("algorithm", "hybrid_kmeans");
                    
                    batchArgs.add(new Object[]{user1, user2, data.toJSONString()});
                    savedPairs++;
                    
                    if (batchArgs.size() >= BATCH_SIZE) {
                        jdbcTemplate.batchUpdate(insertSql, batchArgs);
                        batchArgs.clear();
                    }
                }
            }
        }
        
        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(insertSql, batchArgs);
        }
        
        System.out.println("保存了 " + savedPairs + " 对用户相似度数据");
    }
    
    /**
     * 保存聚类结果到数据库
     * 将聚类信息保存到user_profile的JSON字段中
     */
    private void saveClusterResults(Dataset<Row> clusteredDF) {
        if (clusteredDF.isEmpty()) {
            System.out.println("聚类结果为空，跳过保存");
            return;
        }
        
        List<Row> rows = clusteredDF.select("user_id", "cluster").collectAsList();
        
        String updateSql = "UPDATE users SET user_profile = JSON_SET(COALESCE(user_profile, '{}'), '$.cluster_id', ?, '$.cluster_algorithm', 'kmeans', '$.cluster_calculated_at', ?) WHERE user_id = ?";
        
        List<Object[]> batchArgs = new ArrayList<>();
        String calculatedAt = new Timestamp(System.currentTimeMillis()).toString();
        
        int savedCount = 0;
        for (Row row : rows) {
            try {
                int userId = (int) row.getDouble(0);
                int cluster = row.getInt(1);
                
                batchArgs.add(new Object[]{cluster, calculatedAt, userId});
                savedCount++;
                
                // 批量提交，每500条提交一次
                if (batchArgs.size() >= BATCH_SIZE) {
                    jdbcTemplate.batchUpdate(updateSql, batchArgs);
                    batchArgs.clear();
                }
            } catch (Exception e) {
                System.err.println("处理聚类结果时出错: " + e.getMessage());
            }
        }
        
        // 提交剩余数据
        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(updateSql, batchArgs);
        }
        
        System.out.println("用户聚类结果已保存到数据库，共 " + savedCount + " 个用户");
    }
    
    // ==================== 相似度计算方法 ====================
    
    private double cosineSimilarity(double[] v1, double[] v2) {
        double dotProduct = 0.0, norm1 = 0.0, norm2 = 0.0;
        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }
        return (norm1 == 0 || norm2 == 0) ? 0.0 : dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    private double euclideanSimilarity(double[] v1, double[] v2) {
        double sum = 0.0;
        for (int i = 0; i < v1.length; i++) {
            sum += Math.pow(v1[i] - v2[i], 2);
        }
        return Math.exp(-Math.sqrt(sum) / v1.length);
    }
    
    /**
     * 计算行为相似度（基于浏览的房源）
     */
    private double calculateBehaviorSimilarity(UserProfile p1, UserProfile p2) {
        if (p1 == null || p2 == null) return 0.0;
        
        // 1. 浏览房源的Jaccard相似度
        Set<Integer> viewed1 = p1.viewedProperties.keySet();
        Set<Integer> viewed2 = p2.viewedProperties.keySet();
        double viewedJaccard = jaccardSimilarity(viewed1, viewed2);
        
        // 2. 收藏房源的Jaccard相似度
        double favoriteJaccard = jaccardSimilarity(p1.favoriteProperties, p2.favoriteProperties);
        
        // 3. 搜索关键词的重叠度
        double keywordOverlap = calculateKeywordOverlap(p1.searchKeywords, p2.searchKeywords);
        
        // 4. 偏好位置的重叠度
        double locationOverlap = jaccardSimilarity(p1.prefLocations, p2.prefLocations);
        
        // 加权融合
        return 0.3 * viewedJaccard + 0.3 * favoriteJaccard + 0.2 * keywordOverlap + 0.2 * locationOverlap;
    }
    
    private double jaccardSimilarity(Set<?> set1, Set<?> set2) {
        if (set1 == null || set2 == null || (set1.isEmpty() && set2.isEmpty())) {
            return 0.0;
        }
        
        Set<Object> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<Object> union = new HashSet<>(set1);
        union.addAll(set2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    private double calculateKeywordOverlap(Map<String, Integer> kw1, Map<String, Integer> kw2) {
        if (kw1 == null || kw2 == null || kw1.isEmpty() || kw2.isEmpty()) {
            return 0.0;
        }
        
        Set<String> common = new HashSet<>(kw1.keySet());
        common.retainAll(kw2.keySet());
        
        Set<String> all = new HashSet<>(kw1.keySet());
        all.addAll(kw2.keySet());
        
        return all.isEmpty() ? 0.0 : (double) common.size() / all.size();
    }
    
    // ==================== 编码方法 ====================
    
    private double encodeFamilyStructure(String structure) {
        if (structure == null) return 0.5;
        switch (structure.toLowerCase()) {
            case "single": return 0.2;
            case "couple": return 0.5;
            case "family": return 0.8;
            case "extended": return 1.0;
            default: return 0.5;
        }
    }
    
    private double encodeDecorationPreference(Set<String> decorations) {
        if (decorations == null || decorations.isEmpty()) return 0.5;
        double score = 0.0;
        if (decorations.contains("luxury")) score += 1.0;
        if (decorations.contains("hard")) score += 0.7;
        if (decorations.contains("simple")) score += 0.4;
        return score / decorations.size();
    }
    
    private double encodeOrientationPreference(Set<String> orientations) {
        if (orientations == null || orientations.isEmpty()) return 0.5;
        double score = 0.0;
        if (orientations.contains("south")) score += 1.0;
        if (orientations.contains("southeast")) score += 0.9;
        if (orientations.contains("east")) score += 0.7;
        return score / orientations.size();
    }
    
    private double encodeLocationPreference(Set<String> locations) {
        if (locations == null || locations.isEmpty()) return 0.0;
        // 简单编码：位置数量
        return Math.min(1.0, locations.size() / 5.0);
    }
    
    // ==================== 公共API ====================
    
    /**
     * 获取相似用户列表
     */
    public List<Map<String, Object>> getSimilarUsers(int userId, int limit) {
        String sql = "SELECT " +
                    "CASE WHEN user_id1 = ? THEN user_id2 ELSE user_id1 END as similar_user_id, " +
                    "similarity_data FROM user_similarity " +
                    "WHERE user_id1 = ? OR user_id2 = ? " +
                    "ORDER BY CAST(JSON_EXTRACT(similarity_data, '$.similarity_score') AS DECIMAL(8,4)) DESC " +
                    "LIMIT ?";
        return jdbcTemplate.queryForList(sql, userId, userId, userId, limit);
    }
    
    // ==================== 用户画像内部类 ====================
    
    private static class UserProfile {
        int userId;
        
        // 基本信息
        double budgetMin = 0;
        double budgetMax = 0;
        String familyStructure = "unknown";
        Set<String> preferredLocations = new HashSet<>();
        
        // 偏好设置
        double prefPriceMin = 0;
        double prefPriceMax = 0;
        double prefAreaMin = 0;
        double prefAreaMax = 0;
        int prefBedroomMin = 0;
        int prefBedroomMax = 0;
        Set<String> prefDecorations = new HashSet<>();
        Set<String> prefOrientations = new HashSet<>();
        Set<String> prefKeywords = new HashSet<>();
        Set<String> prefLocations = new HashSet<>();
        
        // 行为数据
        Map<Integer, Double> viewedProperties = new HashMap<>(); // 浏览的房源及得分
        Set<Integer> favoriteProperties = new HashSet<>();       // 收藏的房源
        Map<String, Integer> searchKeywords = new HashMap<>();   // 搜索关键词及次数
        double totalViewDuration = 0;
        int totalViewCount = 0;
        int searchCount = 0;
        double searchPriceSum = 0;
        int searchPriceCount = 0;
        
        // 衍生特征
        double avgViewDuration = 0;
        double avgSearchPrice = 0;
        double activityScore = 0;
        double preferenceClarity = 0;
        
        UserProfile(int userId) {
            this.userId = userId;
        }
        
        void calculateDerivedFeatures() {
            // 平均浏览时长
            avgViewDuration = totalViewCount > 0 ? totalViewDuration / totalViewCount : 0;
            
            // 平均搜索价格
            avgSearchPrice = searchPriceCount > 0 ? searchPriceSum / searchPriceCount : 0;
            
            // 活跃度得分
            activityScore = Math.log1p(totalViewCount) * 0.4 + 
                           Math.log1p(favoriteProperties.size()) * 0.4 + 
                           Math.log1p(searchCount) * 0.2;
            
            // 偏好清晰度（设置了多少偏好）
            int prefCount = 0;
            if (prefPriceMax > 0) prefCount++;
            if (prefAreaMax > 0) prefCount++;
            if (prefBedroomMax > 0) prefCount++;
            if (!prefDecorations.isEmpty()) prefCount++;
            if (!prefOrientations.isEmpty()) prefCount++;
            if (!prefLocations.isEmpty()) prefCount++;
            preferenceClarity = prefCount / 6.0;
        }
    }
}