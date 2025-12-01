// PropertySimilarityService.java
package com.example.service;

import org.springframework.stereotype.Service;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.functions;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.feature.MinMaxScaler;
import org.apache.spark.ml.feature.StandardScaler;
import org.apache.spark.ml.linalg.DenseVector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Service
public class PropertySimilarityService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private SparkSession sparkSession;
    
    /**
     * 基于房源特征计算相似度
     */
    public void calculatePropertySimilarity() {
        System.out.println("开始计算房源相似度...");
        
        // 1. 获取房源数据
        String sql = "SELECT p.property_id, p.community_id, " +
                    "p.basic_info, p.price_info, p.layout_info, " +
                    "c.location_info, c.facility_info " +
                    "FROM properties p " +
                    "JOIN communities c ON p.community_id = c.community_id " +
                    "WHERE p.status = 'for_sale'";
        
        List<Map<String, Object>> properties = jdbcTemplate.queryForList(sql);
        
        // 2. 提取特征向量
        List<PropertyFeatures> featuresList = extractPropertyFeatures(properties);
        
        // 3. 转换为Spark DataFrame
        Dataset<Row> featuresDF = convertToDataFrame(featuresList);
        
        // 4. 特征标准化
        featuresDF = normalizeFeatures(featuresDF);
        
        // 5. 计算相似度矩阵
        calculateAndSaveSimilarities(featuresDF, featuresList);
        
        System.out.println("房源相似度计算完成");
    }
    
    private List<PropertyFeatures> extractPropertyFeatures(List<Map<String, Object>> properties) {
        List<PropertyFeatures> featuresList = new ArrayList<>();
        
        for (Map<String, Object> prop : properties) {
            PropertyFeatures features = new PropertyFeatures();
            features.propertyId = (Integer) prop.get("property_id");
            
            // 解析JSON数据
            String basicInfo = (String) prop.get("basic_info");
            String priceInfo = (String) prop.get("price_info");
            String layoutInfo = (String) prop.get("layout_info");
            String locationInfo = (String) prop.get("location_info");
            String facilityInfo = (String) prop.get("facility_info");
            
            // 提取数值特征
            features.totalPrice = extractDouble(priceInfo, "total_price");
            features.unitPrice = extractDouble(priceInfo, "unit_price");
            features.area = extractDouble(layoutInfo, "area");
            features.bedroomCount = extractInt(layoutInfo, "bedroom_count");
            features.livingRoomCount = extractInt(layoutInfo, "living_room_count");
            features.floor = extractInt(layoutInfo, "floor");
            features.totalFloors = extractInt(layoutInfo, "total_floors");
            
            // 提取分类特征（one-hot编码）
            features.orientation = extractOrientation(layoutInfo);
            features.decoration = extractDecoration(basicInfo);
            features.buildYear = extractInt(basicInfo, "build_year");
            
            // 地理位置特征
            features.district = extractDistrict(locationInfo);
            features.longitude = extractDouble(locationInfo, "longitude");
            features.latitude = extractDouble(locationInfo, "latitude");
            
            // 小区特征
            features.managementFee = extractDouble(facilityInfo, "management_fee");
            features.greenRatio = extractDouble(facilityInfo, "green_ratio");
            features.parkingSpaces = extractInt(facilityInfo, "parking_spaces");
            
            // 特征工程：计算衍生特征
            features.floorRatio = (double) features.floor / Math.max(1, features.totalFloors);
            features.pricePerArea = features.totalPrice / Math.max(1, features.area);
            features.roomCount = features.bedroomCount + features.livingRoomCount;
            
            featuresList.add(features);
        }
        
        return featuresList;
    }
    
    private double extractDouble(String json, String key) {
        if (json == null) return 0.0;
        Pattern pattern = Pattern.compile("\"" + key + "\":\\s*(\\d+\\.?\\d*)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return 0.0;
    }
    
    private int extractInt(String json, String key) {
        if (json == null) return 0;
        Pattern pattern = Pattern.compile("\"" + key + "\":\\s*(\\d+)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }
    
    private String extractOrientation(String layoutInfo) {
        // 简化处理，实际应该用JSON解析
        if (layoutInfo == null) return "unknown";
        if (layoutInfo.contains("south")) return "south";
        if (layoutInfo.contains("north")) return "north";
        if (layoutInfo.contains("east")) return "east";
        if (layoutInfo.contains("west")) return "west";
        return "unknown";
    }
    
    private String extractDecoration(String basicInfo) {
        if (basicInfo == null) return "unknown";
        if (basicInfo.contains("luxury")) return "luxury";
        if (basicInfo.contains("hard")) return "hard";
        if (basicInfo.contains("simple")) return "simple";
        return "unknown";
    }
    
    private String extractDistrict(String locationInfo) {
        if (locationInfo == null) return "unknown";
        Pattern pattern = Pattern.compile("\"district\":\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(locationInfo);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "unknown";
    }
    
    private Dataset<Row> convertToDataFrame(List<PropertyFeatures> featuresList) {
        List<Row> rows = new ArrayList<>();
        
        for (PropertyFeatures features : featuresList) {
            rows.add(RowFactory.create(
                (double) features.propertyId,
                features.totalPrice,
                features.unitPrice,
                features.area,
                (double) features.bedroomCount,
                (double) features.livingRoomCount,
                (double) features.floor,
                (double) features.totalFloors,
                encodeOrientation(features.orientation),
                encodeDecoration(features.decoration),
                (double) features.buildYear,
                encodeDistrict(features.district),
                features.longitude,
                features.latitude,
                features.managementFee,
                features.greenRatio,
                (double) features.parkingSpaces,
                features.floorRatio,
                features.pricePerArea,
                (double) features.roomCount
            ));
        }
        
        // 创建schema
        StructType schema = DataTypes.createStructType(new StructField[]{
            DataTypes.createStructField("property_id", DataTypes.DoubleType, false),
            DataTypes.createStructField("total_price", DataTypes.DoubleType, false),
            DataTypes.createStructField("unit_price", DataTypes.DoubleType, false),
            DataTypes.createStructField("area", DataTypes.DoubleType, false),
            DataTypes.createStructField("bedroom_count", DataTypes.DoubleType, false),
            DataTypes.createStructField("living_room_count", DataTypes.DoubleType, false),
            DataTypes.createStructField("floor", DataTypes.DoubleType, false),
            DataTypes.createStructField("total_floors", DataTypes.DoubleType, false),
            DataTypes.createStructField("orientation", DataTypes.DoubleType, false),
            DataTypes.createStructField("decoration", DataTypes.DoubleType, false),
            DataTypes.createStructField("build_year", DataTypes.DoubleType, false),
            DataTypes.createStructField("district", DataTypes.DoubleType, false),
            DataTypes.createStructField("longitude", DataTypes.DoubleType, false),
            DataTypes.createStructField("latitude", DataTypes.DoubleType, false),
            DataTypes.createStructField("management_fee", DataTypes.DoubleType, false),
            DataTypes.createStructField("green_ratio", DataTypes.DoubleType, false),
            DataTypes.createStructField("parking_spaces", DataTypes.DoubleType, false),
            DataTypes.createStructField("floor_ratio", DataTypes.DoubleType, false),
            DataTypes.createStructField("price_per_area", DataTypes.DoubleType, false),
            DataTypes.createStructField("room_count", DataTypes.DoubleType, false)
        });
        
        return sparkSession.createDataFrame(rows, schema);
    }
    
    private double encodeOrientation(String orientation) {
        switch (orientation) {
            case "south": return 1.0;
            case "southeast": return 0.8;
            case "east": return 0.6;
            case "northeast": return 0.4;
            case "north": return 0.2;
            default: return 0.0;
        }
    }
    
    private double encodeDecoration(String decoration) {
        switch (decoration) {
            case "luxury": return 1.0;
            case "hard": return 0.7;
            case "simple": return 0.4;
            default: return 0.0;
        }
    }
    
    private double encodeDistrict(String district) {
        // 简化处理，实际应该根据地区热度编码
        Map<String, Double> districtMap = new HashMap<>();
        districtMap.put("南山区", 1.0);
        districtMap.put("福田区", 0.9);
        districtMap.put("宝安区", 0.7);
        districtMap.put("龙岗区", 0.5);
        districtMap.put("罗湖区", 0.6);
        
        return districtMap.getOrDefault(district, 0.3);
    }
    
    private Dataset<Row> normalizeFeatures(Dataset<Row> df) {
        // 选择数值型特征列
        String[] featureCols = new String[]{
            "total_price", "unit_price", "area", "bedroom_count", 
            "living_room_count", "floor", "total_floors", "build_year",
            "management_fee", "green_ratio", "parking_spaces",
            "floor_ratio", "price_per_area", "room_count"
        };
        
        // 使用MinMaxScaler归一化
        VectorAssembler assembler = new VectorAssembler()
            .setInputCols(featureCols)
            .setOutputCol("raw_features");
        
        Dataset<Row> assembled = assembler.transform(df);
        
        MinMaxScaler scaler = new MinMaxScaler()
            .setInputCol("raw_features")
            .setOutputCol("scaled_features")
            .setMin(0.0)
            .setMax(1.0);
        
        return scaler.fit(assembled).transform(assembled);
    }
    
    private void calculateAndSaveSimilarities(Dataset<Row> featuresDF, 
                                             List<PropertyFeatures> featuresList) {
        // 收集特征向量
        List<Row> rows = featuresDF.select("property_id", "scaled_features").collectAsList();
        Map<Integer, DenseVector> propertyVectors = new HashMap<>();
        
        for (Row row : rows) {
            int propertyId = (int) row.getDouble(0);
            DenseVector vector = (DenseVector) row.get(1);
            propertyVectors.put(propertyId, vector);
        }
        
        // 计算相似度
        String insertSql = "INSERT INTO property_similarity " +
                          "(property_id1, property_id2, similarity_data, created_at, updated_at) " +
                          "VALUES (?, ?, ?, NOW(), NOW()) " +
                          "ON DUPLICATE KEY UPDATE " +
                          "similarity_data = VALUES(similarity_data), " +
                          "updated_at = NOW()";
        
        List<Object[]> batchArgs = new ArrayList<>();
        List<Integer> propertyIds = new ArrayList<>(propertyVectors.keySet());
        
        for (int i = 0; i < propertyIds.size(); i++) {
            for (int j = i + 1; j < propertyIds.size(); j++) {
                int prop1 = propertyIds.get(i);
                int prop2 = propertyIds.get(j);
                
                DenseVector vec1 = propertyVectors.get(prop1);
                DenseVector vec2 = propertyVectors.get(prop2);
                
                double similarity = cosineSimilarity(vec1, vec2);
                
                // 只保存有意义的相似度
                if (similarity > 0.3) {
                    String similarityData = String.format(
                        "{\"similarity_score\": %.4f, \"algorithm\": \"cosine_vector\", " +
                        "\"feature_weights\": {\"price\": 0.3, \"area\": 0.2, \"location\": 0.2, " +
                        "\"layout\": 0.15, \"facility\": 0.15}}",
                        similarity
                    );
                    
                    batchArgs.add(new Object[]{prop1, prop2, similarityData});
                }
            }
            
            // 批量插入，每1000条提交一次
            if (batchArgs.size() >= 1000) {
                jdbcTemplate.batchUpdate(insertSql, batchArgs);
                batchArgs.clear();
            }
        }
        
        // 插入剩余数据
        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(insertSql, batchArgs);
        }
    }
    
    private double cosineSimilarity(DenseVector v1, DenseVector v2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        double[] values1 = v1.values();
        double[] values2 = v2.values();
        
        for (int i = 0; i < values1.length; i++) {
            dotProduct += values1[i] * values2[i];
            norm1 += values1[i] * values1[i];
            norm2 += values2[i] * values2[i];
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    /**
     * 基于用户行为计算房源相似度（物品-物品协同过滤）
     */
    public void calculatePropertySimilarityCF() {
        // 从浏览历史计算房源共现相似度
        String sql = "SELECT user_id, property_id, COUNT(*) as view_count " +
                    "FROM browsing_history " +
                    "WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
                    "GROUP BY user_id, property_id";
        
        List<Map<String, Object>> views = jdbcTemplate.queryForList(sql);
        
        // 构建用户-物品矩阵
        Map<Integer, Map<Integer, Double>> userItemMatrix = new HashMap<>();
        
        for (Map<String, Object> view : views) {
            Integer userId = (Integer) view.get("user_id");
            Integer propertyId = (Integer) view.get("property_id");
            Double count = ((Number) view.get("view_count")).doubleValue();
            
            userItemMatrix.computeIfAbsent(userId, k -> new HashMap<>())
                         .put(propertyId, count);
        }
        
        // 计算物品-物品相似度（基于共现）
        calculateItemItemSimilarity(userItemMatrix);
    }
    
    private void calculateItemItemSimilarity(Map<Integer, Map<Integer, Double>> userItemMatrix) {
        // 获取所有房源ID
        Set<Integer> allProperties = new HashSet<>();
        for (Map<Integer, Double> items : userItemMatrix.values()) {
            allProperties.addAll(items.keySet());
        }
        
        List<Integer> propertyList = new ArrayList<>(allProperties);
        Map<Integer, Integer> propertyIndex = new HashMap<>();
        
        for (int i = 0; i < propertyList.size(); i++) {
            propertyIndex.put(propertyList.get(i), i);
        }
        
        // 构建共现矩阵
        int n = propertyList.size();
        double[][] cooccurrence = new double[n][n];
        double[] itemPopularity = new double[n];
        
        for (Map<Integer, Double> items : userItemMatrix.values()) {
            List<Integer> userItems = new ArrayList<>(items.keySet());
            
            for (int i = 0; i < userItems.size(); i++) {
                int idx1 = propertyIndex.get(userItems.get(i));
                itemPopularity[idx1] += 1;
                
                for (int j = i + 1; j < userItems.size(); j++) {
                    int idx2 = propertyIndex.get(userItems.get(j));
                    cooccurrence[idx1][idx2] += 1;
                    cooccurrence[idx2][idx1] += 1;
                }
            }
        }
        
        // 计算Jaccard相似度并保存
        String insertSql = "INSERT INTO property_similarity " +
                          "(property_id1, property_id2, similarity_data, created_at, updated_at) " +
                          "VALUES (?, ?, ?, NOW(), NOW()) " +
                          "ON DUPLICATE KEY UPDATE " +
                          "similarity_data = VALUES(similarity_data), " +
                          "updated_at = NOW()";
        
        List<Object[]> batchArgs = new ArrayList<>();
        
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (cooccurrence[i][j] > 0) {
                    double jaccard = cooccurrence[i][j] / 
                                    (itemPopularity[i] + itemPopularity[j] - cooccurrence[i][j]);
                    
                    if (jaccard > 0.1) {
                        int prop1 = propertyList.get(i);
                        int prop2 = propertyList.get(j);
                        
                        String similarityData = String.format(
                            "{\"similarity_score\": %.4f, \"algorithm\": \"jaccard_cf\", " +
                            "\"cooccurrence_count\": %.0f, \"calculation_method\": \"user_behavior\"}",
                            jaccard, cooccurrence[i][j]
                        );
                        
                        batchArgs.add(new Object[]{prop1, prop2, similarityData});
                    }
                }
            }
        }
        
        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(insertSql, batchArgs);
        }
    }
    
    // 房源特征内部类
    private static class PropertyFeatures {
        int propertyId;
        double totalPrice;
        double unitPrice;
        double area;
        int bedroomCount;
        int livingRoomCount;
        int floor;
        int totalFloors;
        String orientation;
        String decoration;
        int buildYear;
        String district;
        double longitude;
        double latitude;
        double managementFee;
        double greenRatio;
        int parkingSpaces;
        double floorRatio;
        double pricePerArea;
        int roomCount;
    }
}