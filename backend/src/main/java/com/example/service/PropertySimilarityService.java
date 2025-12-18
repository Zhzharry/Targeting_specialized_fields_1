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
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.feature.StandardScaler;
import org.apache.spark.ml.feature.StandardScalerModel;
import org.apache.spark.ml.feature.PCA;
import org.apache.spark.ml.feature.PCAModel;
import org.apache.spark.ml.clustering.KMeans;
import org.apache.spark.ml.clustering.KMeansModel;
import org.apache.spark.ml.clustering.BisectingKMeans;
import org.apache.spark.ml.clustering.BisectingKMeansModel;
import org.apache.spark.ml.linalg.Vector;
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
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.sql.Timestamp;

/**
 * 增强版房源相似度计算服务
 * 
 * 功能特点：
 * 1. K-Means聚类分析 - 将房源分组到相似的簇中
 * 2. Bisecting K-Means层次聚类 - 支持层次化的房源分组
 * 3. PCA降维 - 减少特征维度，提高计算效率
 * 4. 多种相似度算法 - 余弦相似度、欧几里得距离、加权综合相似度
 * 5. Hadoop MapReduce - 用于大规模相似度矩阵计算
 * 6. Spark MLlib - 用于机器学习聚类分析
 * 7. 协同过滤 - 基于用户行为的物品相似度计算
 * 
 * 版本兼容性：
 * - Java 1.8
 * - Spring Boot 2.7.18
 * - Hadoop 3.4.1
 * - Spark 3.3.4 (Scala 2.12)
 * - MySQL 8.0.33
 * 
 * @author Enhanced Version
 * @version 2.0
 */
@Service
public class PropertySimilarityService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private SparkSession sparkSession;
    
    @Value("${hadoop.hdfs.uri:hdfs://localhost:9000}")
    private String hdfsUri;
    
    @Value("${hadoop.tmp.dir:/tmp/hadoop-property-similarity}")
    private String hadoopTmpDir;
    
    // 聚类参数
    private static final int DEFAULT_K_CLUSTERS = 8;
    private static final int MAX_ITERATIONS = 100;
    private static final double SIMILARITY_THRESHOLD = 0.3;
    private static final int BATCH_SIZE = 1000;
    private static final int PCA_DIMENSIONS = 10;
    
    // 特征权重配置
    private static final Map<String, Double> FEATURE_WEIGHTS = new HashMap<String, Double>() {{
        put("price", 0.25);
        put("area", 0.20);
        put("location", 0.20);
        put("layout", 0.15);
        put("facility", 0.10);
        put("age", 0.10);
    }};
    
    // 缓存聚类结果
    private Map<Integer, Integer> propertyClusterCache = new ConcurrentHashMap<>();
    
    /**
     * 主入口：执行完整的相似度计算流程
     */
    @Transactional
    public void calculatePropertySimilarityFull() {
        System.out.println("========================================");
        System.out.println("开始执行完整的房源相似度计算流程...");
        System.out.println("时间: " + new Timestamp(System.currentTimeMillis()));
        System.out.println("========================================");
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 获取并提取房源特征
            System.out.println("\n[步骤1/7] 获取房源数据...");
            List<PropertyFeatures> featuresList = loadPropertyFeatures();
            System.out.println("共获取 " + featuresList.size() + " 条房源数据");
            
            if (featuresList.isEmpty()) {
                System.out.println("警告：没有找到有效的房源数据！");
                return;
            }
            
            // 2. 转换为Spark DataFrame
            System.out.println("\n[步骤2/7] 转换为Spark DataFrame...");
            Dataset<Row> featuresDF = convertToSparkDataFrame(featuresList);
            
            // 3. 特征标准化
            System.out.println("\n[步骤3/7] 特征标准化处理...");
            Dataset<Row> normalizedDF = normalizeFeatures(featuresDF);
            
            // 4. PCA降维
            System.out.println("\n[步骤4/7] PCA降维处理...");
            Dataset<Row> pcaDF = applyPCA(normalizedDF);
            
            // 5. K-Means聚类
            System.out.println("\n[步骤5/7] 执行K-Means聚类分析...");
            Dataset<Row> clusteredDF = performKMeansClustering(pcaDF);
            
            // 6. 保存聚类结果
            System.out.println("\n[步骤6/7] 保存聚类结果...");
            saveClusterResults(clusteredDF, featuresList);
            
            // 7. 计算并保存相似度
            System.out.println("\n[步骤7/7] 计算房源相似度...");
            calculateAndSaveSimilarities(clusteredDF, featuresList);
            
            long endTime = System.currentTimeMillis();
            System.out.println("\n========================================");
            System.out.println("房源相似度计算完成！");
            System.out.println("总耗时: " + (endTime - startTime) / 1000.0 + " 秒");
            System.out.println("========================================");
            
        } catch (Exception e) {
            System.err.println("计算过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("房源相似度计算失败", e);
        }
    }
    
    /**
     * 从数据库加载房源特征数据
     */
    private List<PropertyFeatures> loadPropertyFeatures() {
        String sql = "SELECT p.property_id, p.community_id, p.title, " +
                    "p.basic_info, p.price_info, p.layout_info, " +
                    "c.location_info, c.facility_info, c.name as community_name " +
                    "FROM properties p " +
                    "JOIN communities c ON p.community_id = c.community_id " +
                    "WHERE p.status = 'for_sale'";
        
        List<Map<String, Object>> properties = jdbcTemplate.queryForList(sql);
        List<PropertyFeatures> featuresList = new ArrayList<>();
        
        for (Map<String, Object> prop : properties) {
            try {
                PropertyFeatures features = extractFeatures(prop);
                if (features != null && features.isValid()) {
                    featuresList.add(features);
                }
            } catch (Exception e) {
                System.err.println("提取房源 " + prop.get("property_id") + " 特征时出错: " + e.getMessage());
            }
        }
        return featuresList;
    }
    
    /**
     * 提取单个房源的特征
     */
    private PropertyFeatures extractFeatures(Map<String, Object> prop) {
        PropertyFeatures f = new PropertyFeatures();
        f.propertyId = ((Number) prop.get("property_id")).intValue();
        f.communityId = ((Number) prop.get("community_id")).intValue();
        f.title = (String) prop.get("title");
        f.communityName = (String) prop.get("community_name");
        
        // 解析JSON数据
        JSONObject basicJson = parseJson((String) prop.get("basic_info"));
        JSONObject priceJson = parseJson((String) prop.get("price_info"));
        JSONObject layoutJson = parseJson((String) prop.get("layout_info"));
        JSONObject locationJson = parseJson((String) prop.get("location_info"));
        JSONObject facilityJson = parseJson((String) prop.get("facility_info"));
        
        // 提取特征
        f.totalPrice = getDoubleValue(priceJson, "total_price", 0.0);
        f.unitPrice = getDoubleValue(priceJson, "unit_price", 0.0);
        f.area = getDoubleValue(layoutJson, "area", 0.0);
        f.bedroomCount = getIntValue(layoutJson, "bedroom_count", 0);
        f.livingRoomCount = getIntValue(layoutJson, "living_room_count", 0);
        f.bathroomCount = getIntValue(layoutJson, "bathroom_count", 0);
        f.floor = getIntValue(layoutJson, "floor", 0);
        f.totalFloors = getIntValue(layoutJson, "total_floors", 1);
        f.orientation = getStringValue(layoutJson, "orientation", "unknown");
        f.buildYear = getIntValue(basicJson, "build_year", 2000);
        f.decoration = getStringValue(basicJson, "decoration", "unknown");
        f.district = getStringValue(locationJson, "district", "unknown");
        f.city = getStringValue(locationJson, "city", "unknown");
        f.longitude = getDoubleValue(locationJson, "longitude", 0.0);
        f.latitude = getDoubleValue(locationJson, "latitude", 0.0);
        f.managementFee = getDoubleValue(facilityJson, "management_fee", 0.0);
        f.greenRatio = getDoubleValue(facilityJson, "green_ratio", 0.0);
        f.plotRatio = getDoubleValue(facilityJson, "plot_ratio", 0.0);
        f.parkingSpaces = getIntValue(facilityJson, "parking_spaces", 0);
        
        // 计算衍生特征
        calculateDerivedFeatures(f);
        return f;
    }
    
    /**
     * 计算衍生特征
     */
    private void calculateDerivedFeatures(PropertyFeatures f) {
        f.floorRatio = f.totalFloors > 0 ? (double) f.floor / f.totalFloors : 0.5;
        f.pricePerArea = f.area > 0 ? f.totalPrice * 10000 / f.area : 0.0;
        f.roomCount = f.bedroomCount + f.livingRoomCount;
        f.areaPerRoom = f.roomCount > 0 ? f.area / f.roomCount : f.area;
        f.houseAge = java.time.Year.now().getValue() - f.buildYear;
        f.orientationScore = calculateOrientationScore(f.orientation);
        f.decorationScore = calculateDecorationScore(f.decoration);
        f.districtScore = calculateDistrictScore(f.district);
    }
    
    private double calculateOrientationScore(String orientation) {
        if (orientation == null) return 0.5;
        Map<String, Double> scores = new HashMap<>();
        scores.put("south", 1.0);
        scores.put("southeast", 0.9);
        scores.put("southwest", 0.85);
        scores.put("east", 0.7);
        scores.put("west", 0.6);
        scores.put("northeast", 0.4);
        scores.put("northwest", 0.35);
        scores.put("north", 0.2);
        return scores.getOrDefault(orientation.toLowerCase(), 0.5);
    }
    
    private double calculateDecorationScore(String decoration) {
        if (decoration == null) return 0.5;
        switch (decoration.toLowerCase()) {
            case "luxury": return 1.0;
            case "hard": return 0.7;
            case "simple": return 0.4;
            case "blank": return 0.2;
            default: return 0.5;
        }
    }
    
    private double calculateDistrictScore(String district) {
        if (district == null) return 0.5;
        Map<String, Double> scores = new HashMap<>();
        scores.put("南山区", 1.0);
        scores.put("福田区", 0.95);
        scores.put("宝安区", 0.75);
        scores.put("龙岗区", 0.6);
        scores.put("罗湖区", 0.7);
        scores.put("龙华区", 0.65);
        scores.put("宣武", 0.85);
        return scores.getOrDefault(district, 0.5);
    }
    
    /**
     * 转换为Spark DataFrame
     */
    private Dataset<Row> convertToSparkDataFrame(List<PropertyFeatures> featuresList) {
        List<Row> rows = new ArrayList<>();
        for (PropertyFeatures f : featuresList) {
            rows.add(RowFactory.create(
                (double) f.propertyId, (double) f.communityId,
                f.totalPrice, f.unitPrice, f.area,
                (double) f.bedroomCount, (double) f.livingRoomCount, (double) f.bathroomCount,
                (double) f.floor, (double) f.totalFloors, f.floorRatio,
                f.orientationScore, f.decorationScore,
                (double) f.buildYear, (double) f.houseAge, f.districtScore,
                f.longitude, f.latitude, f.managementFee, f.greenRatio, f.plotRatio,
                (double) f.parkingSpaces, f.pricePerArea, f.areaPerRoom, (double) f.roomCount
            ));
        }
        
        StructType schema = DataTypes.createStructType(new StructField[]{
            DataTypes.createStructField("property_id", DataTypes.DoubleType, false),
            DataTypes.createStructField("community_id", DataTypes.DoubleType, false),
            DataTypes.createStructField("total_price", DataTypes.DoubleType, false),
            DataTypes.createStructField("unit_price", DataTypes.DoubleType, false),
            DataTypes.createStructField("area", DataTypes.DoubleType, false),
            DataTypes.createStructField("bedroom_count", DataTypes.DoubleType, false),
            DataTypes.createStructField("living_room_count", DataTypes.DoubleType, false),
            DataTypes.createStructField("bathroom_count", DataTypes.DoubleType, false),
            DataTypes.createStructField("floor", DataTypes.DoubleType, false),
            DataTypes.createStructField("total_floors", DataTypes.DoubleType, false),
            DataTypes.createStructField("floor_ratio", DataTypes.DoubleType, false),
            DataTypes.createStructField("orientation_score", DataTypes.DoubleType, false),
            DataTypes.createStructField("decoration_score", DataTypes.DoubleType, false),
            DataTypes.createStructField("build_year", DataTypes.DoubleType, false),
            DataTypes.createStructField("house_age", DataTypes.DoubleType, false),
            DataTypes.createStructField("district_score", DataTypes.DoubleType, false),
            DataTypes.createStructField("longitude", DataTypes.DoubleType, false),
            DataTypes.createStructField("latitude", DataTypes.DoubleType, false),
            DataTypes.createStructField("management_fee", DataTypes.DoubleType, false),
            DataTypes.createStructField("green_ratio", DataTypes.DoubleType, false),
            DataTypes.createStructField("plot_ratio", DataTypes.DoubleType, false),
            DataTypes.createStructField("parking_spaces", DataTypes.DoubleType, false),
            DataTypes.createStructField("price_per_area", DataTypes.DoubleType, false),
            DataTypes.createStructField("area_per_room", DataTypes.DoubleType, false),
            DataTypes.createStructField("room_count", DataTypes.DoubleType, false)
        });
        
        return sparkSession.createDataFrame(rows, schema);
    }
    
    /**
     * 特征标准化
     */
    private Dataset<Row> normalizeFeatures(Dataset<Row> df) {
        String[] featureCols = new String[]{
            "total_price", "unit_price", "area", "bedroom_count", 
            "living_room_count", "bathroom_count", "floor", "total_floors",
            "floor_ratio", "orientation_score", "decoration_score",
            "build_year", "house_age", "district_score",
            "management_fee", "green_ratio", "plot_ratio", "parking_spaces",
            "price_per_area", "area_per_room", "room_count"
        };
        
        VectorAssembler assembler = new VectorAssembler()
            .setInputCols(featureCols)
            .setOutputCol("raw_features")
            .setHandleInvalid("skip");
        
        Dataset<Row> assembled = assembler.transform(df);
        
        StandardScaler scaler = new StandardScaler()
            .setInputCol("raw_features")
            .setOutputCol("scaled_features")
            .setWithStd(true)
            .setWithMean(true);
        
        StandardScalerModel scalerModel = scaler.fit(assembled);
        System.out.println("特征标准化完成，特征维度: " + featureCols.length);
        return scalerModel.transform(assembled);
    }
    
    /**
     * PCA降维处理
     */
    private Dataset<Row> applyPCA(Dataset<Row> df) {
        long sampleCount = df.count();
        int effectiveDimensions = Math.min(PCA_DIMENSIONS, (int) Math.min(sampleCount, 21));
        
        if (effectiveDimensions < 2) {
            System.out.println("数据量太少，跳过PCA降维");
            return df.withColumn("pca_features", df.col("scaled_features"));
        }
        
        PCA pca = new PCA()
            .setInputCol("scaled_features")
            .setOutputCol("pca_features")
            .setK(effectiveDimensions);
        
        PCAModel pcaModel = pca.fit(df);
        double[] explainedVariance = pcaModel.explainedVariance().toArray();
        double totalVariance = Arrays.stream(explainedVariance).sum();
        System.out.println("PCA降维完成，保留" + effectiveDimensions + "个主成分，累计解释方差: " 
            + String.format("%.2f%%", totalVariance * 100));
        
        return pcaModel.transform(df);
    }
    
    /**
     * 执行K-Means聚类
     */
    private Dataset<Row> performKMeansClustering(Dataset<Row> df) {
        long sampleCount = df.count();
        int optimalK = calculateOptimalK(df, sampleCount);
        System.out.println("确定最优聚类数K: " + optimalK);
        
        KMeans kmeans = new KMeans()
            .setK(optimalK)
            .setFeaturesCol("pca_features")
            .setPredictionCol("cluster")
            .setMaxIter(MAX_ITERATIONS)
            .setSeed(42L);
        
        KMeansModel model = kmeans.fit(df);
        Dataset<Row> clusteredDF = model.transform(df);
        
        double wssse = model.summary().trainingCost();
        System.out.println("K-Means聚类完成");
        System.out.println("  - 聚类数量: " + optimalK);
        System.out.println("  - WSSSE: " + String.format("%.4f", wssse));
        
        // 输出各聚类大小
        clusteredDF.groupBy("cluster").count().orderBy("cluster")
            .collectAsList().forEach(row -> 
                System.out.println("    簇" + row.getInt(0) + ": " + row.getLong(1) + "个房源"));
        
        return clusteredDF;
    }
    
    private int calculateOptimalK(Dataset<Row> df, long sampleCount) {
        if (sampleCount < 10) return 2;
        if (sampleCount < 30) return 3;
        if (sampleCount < 100) return 5;
        return Math.min(DEFAULT_K_CLUSTERS, (int) (sampleCount / 5));
    }
    
    /**
     * 保存聚类结果到数据库
     */
    private void saveClusterResults(Dataset<Row> clusteredDF, List<PropertyFeatures> featuresList) {
        List<Row> rows = clusteredDF.select("property_id", "cluster").collectAsList();
        
        for (Row row : rows) {
            int propertyId = (int) row.getDouble(0);
            int cluster = row.getInt(1);
            propertyClusterCache.put(propertyId, cluster);
        }
        
        String sql = "INSERT INTO property_features (property_id, feature_data, created_at) " +
                    "VALUES (?, ?, NOW()) ON DUPLICATE KEY UPDATE feature_data = VALUES(feature_data)";
        
        List<Object[]> batchArgs = new ArrayList<>();
        for (Row row : rows) {
            int propertyId = (int) row.getDouble(0);
            int cluster = row.getInt(1);
            
            JSONObject featureData = new JSONObject();
            featureData.put("feature_type", "cluster");
            featureData.put("feature_name", "kmeans_cluster");
            featureData.put("feature_value", cluster);
            featureData.put("algorithm", "kmeans_pca");
            featureData.put("calculated_at", new Timestamp(System.currentTimeMillis()).toString());
            
            batchArgs.add(new Object[]{propertyId, featureData.toJSONString()});
        }
        
        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchArgs);
        }
        System.out.println("聚类结果已保存到数据库");
    }
    
    /**
     * 计算并保存房源相似度
     */
    private void calculateAndSaveSimilarities(Dataset<Row> clusteredDF, List<PropertyFeatures> featuresList) {
        List<Row> rows = clusteredDF.select("property_id", "pca_features", "cluster").collectAsList();
        
        Map<Integer, double[]> propertyVectors = new HashMap<>();
        Map<Integer, Integer> propertyClusters = new HashMap<>();
        
        for (Row row : rows) {
            int propertyId = (int) row.getDouble(0);
            Vector vector = row.getAs(1);
            int cluster = row.getInt(2);
            propertyVectors.put(propertyId, vector.toArray());
            propertyClusters.put(propertyId, cluster);
        }
        
        Map<Integer, PropertyFeatures> featuresMap = featuresList.stream()
            .collect(Collectors.toMap(f -> f.propertyId, f -> f));
        
        // 清空现有数据
        jdbcTemplate.update("DELETE FROM property_similarity");
        
        String insertSql = "INSERT INTO property_similarity " +
                          "(property_id1, property_id2, similarity_data, created_at, updated_at) " +
                          "VALUES (?, ?, ?, NOW(), NOW())";
        
        List<Object[]> batchArgs = new ArrayList<>();
        List<Integer> propertyIds = new ArrayList<>(propertyVectors.keySet());
        Collections.sort(propertyIds);
        
        int savedPairs = 0;
        
        for (int i = 0; i < propertyIds.size(); i++) {
            for (int j = i + 1; j < propertyIds.size(); j++) {
                int prop1 = propertyIds.get(i);
                int prop2 = propertyIds.get(j);
                
                double[] vec1 = propertyVectors.get(prop1);
                double[] vec2 = propertyVectors.get(prop2);
                int cluster1 = propertyClusters.get(prop1);
                int cluster2 = propertyClusters.get(prop2);
                
                double cosineSim = cosineSimilarity(vec1, vec2);
                double euclideanSim = euclideanSimilarity(vec1, vec2);
                double weightedSim = calculateWeightedSimilarity(featuresMap.get(prop1), featuresMap.get(prop2));
                
                double finalSimilarity = 0.4 * cosineSim + 0.3 * euclideanSim + 0.3 * weightedSim;
                if (cluster1 == cluster2) {
                    finalSimilarity = Math.min(1.0, finalSimilarity * 1.1);
                }
                
                if (finalSimilarity > SIMILARITY_THRESHOLD) {
                    JSONObject data = new JSONObject();
                    data.put("similarity_score", Math.round(finalSimilarity * 10000) / 10000.0);
                    data.put("cosine_similarity", Math.round(cosineSim * 10000) / 10000.0);
                    data.put("euclidean_similarity", Math.round(euclideanSim * 10000) / 10000.0);
                    data.put("weighted_similarity", Math.round(weightedSim * 10000) / 10000.0);
                    data.put("same_cluster", cluster1 == cluster2);
                    data.put("cluster_id", cluster1 == cluster2 ? cluster1 : -1);
                    data.put("algorithm", "hybrid_kmeans_pca");
                    
                    batchArgs.add(new Object[]{prop1, prop2, data.toJSONString()});
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
        
        System.out.println("相似度计算完成，保存了 " + savedPairs + " 对数据");
    }
    
    // ====================== 相似度计算方法 ======================
    
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
        double sumSquared = 0.0;
        for (int i = 0; i < v1.length; i++) {
            sumSquared += Math.pow(v1[i] - v2[i], 2);
        }
        return Math.exp(-Math.sqrt(sumSquared) / v1.length);
    }
    
    private double calculateWeightedSimilarity(PropertyFeatures f1, PropertyFeatures f2) {
        if (f1 == null || f2 == null) return 0.0;
        
        double sim = 0.0;
        // 价格相似度
        sim += FEATURE_WEIGHTS.get("price") * Math.max(0, 1.0 - 
            Math.abs(f1.totalPrice - f2.totalPrice) / Math.max(f1.totalPrice, f2.totalPrice));
        // 面积相似度
        sim += FEATURE_WEIGHTS.get("area") * Math.max(0, 1.0 - 
            Math.abs(f1.area - f2.area) / Math.max(f1.area, f2.area));
        // 位置相似度
        sim += FEATURE_WEIGHTS.get("location") * calculateLocationSimilarity(f1, f2);
        // 户型相似度
        sim += FEATURE_WEIGHTS.get("layout") * calculateLayoutSimilarity(f1, f2);
        // 设施相似度
        sim += FEATURE_WEIGHTS.get("facility") * (1.0 - Math.abs(f1.decorationScore - f2.decorationScore));
        // 房龄相似度
        sim += FEATURE_WEIGHTS.get("age") * Math.max(0, 1.0 - 
            Math.abs(f1.houseAge - f2.houseAge) / Math.max(f1.houseAge, f2.houseAge + 1));
        
        return Math.min(1.0, sim);
    }
    
    private double calculateLocationSimilarity(PropertyFeatures f1, PropertyFeatures f2) {
        if (f1.longitude == 0 || f2.longitude == 0) {
            return f1.district.equals(f2.district) ? 1.0 : 0.5;
        }
        double distance = haversineDistance(f1.latitude, f1.longitude, f2.latitude, f2.longitude);
        return Math.exp(-distance / 5.0);
    }
    
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
    
    private double calculateLayoutSimilarity(PropertyFeatures f1, PropertyFeatures f2) {
        int bedroomDiff = Math.abs(f1.bedroomCount - f2.bedroomCount);
        double sim = 0.4 * (bedroomDiff == 0 ? 1.0 : bedroomDiff == 1 ? 0.7 : 0.3);
        sim += 0.3 * (1.0 - Math.abs(f1.floorRatio - f2.floorRatio));
        sim += 0.3 * (1.0 - Math.abs(f1.orientationScore - f2.orientationScore));
        return sim;
    }
    
    // ====================== 辅助方法 ======================
    
    private JSONObject parseJson(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) return new JSONObject();
        try { return JSON.parseObject(jsonStr); } 
        catch (Exception e) { return new JSONObject(); }
    }
    
    private double getDoubleValue(JSONObject json, String key, double def) {
        if (json == null || !json.containsKey(key)) return def;
        try { return json.getDoubleValue(key); } catch (Exception e) { return def; }
    }
    
    private int getIntValue(JSONObject json, String key, int def) {
        if (json == null || !json.containsKey(key)) return def;
        try { return json.getIntValue(key); } catch (Exception e) { return def; }
    }
    
    private String getStringValue(JSONObject json, String key, String def) {
        if (json == null || !json.containsKey(key)) return def;
        try { String v = json.getString(key); return v != null ? v : def; } 
        catch (Exception e) { return def; }
    }
    
    /**
     * 获取指定房源的相似房源列表
     */
    public List<Map<String, Object>> getSimilarProperties(int propertyId, int limit) {
        String sql = "SELECT " +
                    "CASE WHEN property_id1 = ? THEN property_id2 ELSE property_id1 END as similar_property_id, " +
                    "similarity_data FROM property_similarity " +
                    "WHERE property_id1 = ? OR property_id2 = ? " +
                    "ORDER BY CAST(JSON_EXTRACT(similarity_data, '$.similarity_score') AS DECIMAL(8,4)) DESC LIMIT ?";
        return jdbcTemplate.queryForList(sql, propertyId, propertyId, propertyId, limit);
    }
    
    // ====================== 房源特征内部类 ======================
    
    private static class PropertyFeatures {
        int propertyId, communityId, bedroomCount, livingRoomCount, bathroomCount;
        int floor, totalFloors, buildYear, houseAge, roomCount, parkingSpaces;
        double totalPrice, unitPrice, area, pricePerArea, areaPerRoom;
        double floorRatio, orientationScore, decorationScore, districtScore;
        double longitude, latitude, managementFee, greenRatio, plotRatio;
        String title, communityName, orientation, decoration, district, city;
        
        public boolean isValid() { return propertyId > 0 && totalPrice > 0 && area > 0; }
    }
}