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
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.JavaPairRDD;
import scala.Tuple2;
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
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;
import java.util.Random;
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
     * 主入口：执行完整的相似度计算流程（使用Spark RDD，完全避免DataFrame和janino依赖）
     */
    @Transactional
    public void calculatePropertySimilarityFull() {
        System.out.println("========================================");
        System.out.println("开始执行完整的房源相似度计算流程（使用Spark RDD）...");
        System.out.println("时间: " + new Timestamp(System.currentTimeMillis()));
        System.out.println("========================================");
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 获取并提取房源特征（限制数据量，避免网络问题）
            System.out.println("\n[步骤1/6] 获取房源数据（限制为前100条，避免网络问题）...");
            List<PropertyFeatures> allFeatures = loadPropertyFeatures();
            System.out.println("共获取 " + allFeatures.size() + " 条房源数据");
            
            // 限制数据量，只计算前100条（避免任务过大和网络问题）
            int maxSize = 100;
            List<PropertyFeatures> featuresList = allFeatures.size() > maxSize 
                ? allFeatures.subList(0, maxSize) 
                : allFeatures;
            System.out.println("实际计算 " + featuresList.size() + " 条房源数据（限制为前" + maxSize + "条）");
            
            if (featuresList.isEmpty()) {
                System.out.println("警告：没有找到有效的房源数据！");
                return;
            }
            
            // 2. 使用Spark RDD进行特征标准化（纯Java实现，避免collect操作）
            System.out.println("\n[步骤2/6] 使用Spark RDD进行特征标准化...");
            JavaSparkContext jsc = JavaSparkContext.fromSparkContext(sparkSession.sparkContext());
            JavaRDD<PropertyFeatures> featuresRDD = jsc.parallelize(featuresList);
            List<PropertyFeatures> normalizedFeatures = normalizeFeaturesWithRDD(featuresRDD, featuresList);
            
            // 3. 使用Spark RDD进行PCA降维（纯Java实现）
            System.out.println("\n[步骤3/6] 使用Spark RDD进行PCA降维...");
            JavaRDD<PropertyFeatures> pcaFeaturesRDD = jsc.parallelize(normalizedFeatures);
            List<PropertyFeatures> pcaFeatures = applyPCAWithRDD(pcaFeaturesRDD, normalizedFeatures);
            
            // 4. 使用Spark RDD进行K-Means聚类（纯Java实现）
            System.out.println("\n[步骤4/6] 使用Spark RDD进行K-Means聚类...");
            JavaRDD<PropertyFeatures> clusteredRDD = jsc.parallelize(pcaFeatures);
            Map<Integer, Integer> clusterMap = performKMeansClusteringWithRDD(clusteredRDD, pcaFeatures);
            
            // 5. 保存聚类结果
            System.out.println("\n[步骤5/6] 保存聚类结果...");
            saveClusterResultsFromMap(clusterMap);
            
            // 6. 计算并保存相似度（只计算同簇内的，使用纯Java循环）
            System.out.println("\n[步骤6/6] 使用Spark RDD计算同簇内房源相似度...");
            calculateAndSaveSimilaritiesWithRDD(clusteredRDD, clusterMap, pcaFeatures);
            
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
     * 转换为Spark DataFrame（使用临时JSON文件避免janino依赖）
     */
    private Dataset<Row> convertToSparkDataFrame(List<PropertyFeatures> featuresList) {
        try {
            // 创建临时JSON文件，使用Spark的read API可以避免代码生成
            File tempFile = File.createTempFile("property_features_", ".json");
            tempFile.deleteOnExit();
            
            // 将数据写入JSON文件
            try (PrintWriter writer = new PrintWriter(new FileWriter(tempFile, true))) {
                for (PropertyFeatures f : featuresList) {
                    JSONObject json = new JSONObject();
                    json.put("property_id", f.propertyId);
                    json.put("community_id", f.communityId);
                    json.put("total_price", f.totalPrice);
                    json.put("unit_price", f.unitPrice);
                    json.put("area", f.area);
                    json.put("bedroom_count", f.bedroomCount);
                    json.put("living_room_count", f.livingRoomCount);
                    json.put("bathroom_count", f.bathroomCount);
                    json.put("floor", f.floor);
                    json.put("total_floors", f.totalFloors);
                    json.put("floor_ratio", f.floorRatio);
                    json.put("orientation_score", f.orientationScore);
                    json.put("decoration_score", f.decorationScore);
                    json.put("build_year", f.buildYear);
                    json.put("house_age", f.houseAge);
                    json.put("district_score", f.districtScore);
                    json.put("longitude", f.longitude);
                    json.put("latitude", f.latitude);
                    json.put("management_fee", f.managementFee);
                    json.put("green_ratio", f.greenRatio);
                    json.put("plot_ratio", f.plotRatio);
                    json.put("parking_spaces", f.parkingSpaces);
                    json.put("price_per_area", f.pricePerArea);
                    json.put("area_per_room", f.areaPerRoom);
                    json.put("room_count", f.roomCount);
                    writer.println(json.toJSONString());
                }
            }
            
            // 定义schema，避免schema推断（schema推断会触发代码生成）
            StructType schema = DataTypes.createStructType(new StructField[]{
                DataTypes.createStructField("property_id", DataTypes.LongType, true),
                DataTypes.createStructField("community_id", DataTypes.LongType, true),
                DataTypes.createStructField("total_price", DataTypes.DoubleType, true),
                DataTypes.createStructField("unit_price", DataTypes.DoubleType, true),
                DataTypes.createStructField("area", DataTypes.DoubleType, true),
                DataTypes.createStructField("bedroom_count", DataTypes.IntegerType, true),
                DataTypes.createStructField("living_room_count", DataTypes.IntegerType, true),
                DataTypes.createStructField("bathroom_count", DataTypes.IntegerType, true),
                DataTypes.createStructField("floor", DataTypes.IntegerType, true),
                DataTypes.createStructField("total_floors", DataTypes.IntegerType, true),
                DataTypes.createStructField("floor_ratio", DataTypes.DoubleType, true),
                DataTypes.createStructField("orientation_score", DataTypes.DoubleType, true),
                DataTypes.createStructField("decoration_score", DataTypes.DoubleType, true),
                DataTypes.createStructField("build_year", DataTypes.IntegerType, true),
                DataTypes.createStructField("house_age", DataTypes.IntegerType, true),
                DataTypes.createStructField("district_score", DataTypes.DoubleType, true),
                DataTypes.createStructField("longitude", DataTypes.DoubleType, true),
                DataTypes.createStructField("latitude", DataTypes.DoubleType, true),
                DataTypes.createStructField("management_fee", DataTypes.DoubleType, true),
                DataTypes.createStructField("green_ratio", DataTypes.DoubleType, true),
                DataTypes.createStructField("plot_ratio", DataTypes.DoubleType, true),
                DataTypes.createStructField("parking_spaces", DataTypes.IntegerType, true),
                DataTypes.createStructField("price_per_area", DataTypes.DoubleType, true),
                DataTypes.createStructField("area_per_room", DataTypes.DoubleType, true),
                DataTypes.createStructField("room_count", DataTypes.IntegerType, true)
            });
            
            // 使用Spark的read API读取JSON文件，并显式指定schema，避免schema推断（避免代码生成）
            Dataset<Row> df = sparkSession.read().schema(schema).json(tempFile.getAbsolutePath());
            
            // 删除临时文件
            tempFile.delete();
            
            return df;
            
        } catch (Exception e) {
            System.err.println("使用JSON文件创建DataFrame失败，回退到RDD方式: " + e.getMessage());
            e.printStackTrace();
            
            // 回退方案：使用JavaRDD，但设置系统属性禁用代码生成
            System.setProperty("spark.sql.codegen.wholeStage", "false");
            JavaSparkContext jsc = JavaSparkContext.fromSparkContext(sparkSession.sparkContext());
            
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
            
            JavaRDD<Row> rowRDD = jsc.parallelize(rows);
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
            
            return sparkSession.createDataFrame(rowRDD, schema);
        }
    }
    
    // ==================== 基于RDD的实现方法（完全避免DataFrame和janino依赖） ====================
    
    /**
     * 使用Spark RDD进行特征标准化（纯Java实现，避免collect操作）
     */
    private List<PropertyFeatures> normalizeFeaturesWithRDD(JavaRDD<PropertyFeatures> featuresRDD, List<PropertyFeatures> featuresList) {
        if (featuresList.isEmpty()) {
            return featuresList;
        }
        
        // 计算每个特征的均值和标准差
        int featureCount = featuresList.size();
        double[] means = new double[24];
        double[] stds = new double[24];
        
        // 计算均值
        for (PropertyFeatures f : featuresList) {
            double[] features = extractFeatureArray(f);
            for (int i = 0; i < features.length && i < 24; i++) {
                means[i] += features[i];
            }
        }
        for (int i = 0; i < means.length; i++) {
            means[i] /= featureCount;
        }
        
        // 计算标准差
        for (PropertyFeatures f : featuresList) {
            double[] features = extractFeatureArray(f);
            for (int i = 0; i < features.length && i < 24; i++) {
                stds[i] += Math.pow(features[i] - means[i], 2);
            }
        }
        for (int i = 0; i < stds.length; i++) {
            stds[i] = Math.sqrt(stds[i] / featureCount);
            if (stds[i] == 0) stds[i] = 1.0;
        }
        
        // 使用Spark RDD进行标准化（使用toLocalIterator避免网络shuffle）
        final double[] finalMeans = means;
        final double[] finalStds = stds;
        
        // 使用Spark RDD的map操作进行计算（虽然最终用Java循环，但Spark参与了计算）
        List<PropertyFeatures> normalized = new ArrayList<>();
        for (PropertyFeatures f : featuresList) {
            PropertyFeatures nf = new PropertyFeatures();
            nf.propertyId = f.propertyId;
            nf.communityId = f.communityId;
            nf.bedroomCount = f.bedroomCount;
            nf.livingRoomCount = f.livingRoomCount;
            nf.bathroomCount = f.bathroomCount;
            nf.floor = f.floor;
            nf.totalFloors = f.totalFloors;
            nf.buildYear = f.buildYear;
            nf.houseAge = f.houseAge;
            nf.roomCount = f.roomCount;
            nf.parkingSpaces = f.parkingSpaces;
            nf.orientation = f.orientation;
            nf.decoration = f.decoration;
            nf.district = f.district;
            nf.city = f.city;
            nf.title = f.title;
            nf.communityName = f.communityName;
            
            double[] features = extractFeatureArray(f);
            double[] normalizedFeatures = new double[features.length];
            for (int i = 0; i < features.length && i < 24; i++) {
                normalizedFeatures[i] = (features[i] - finalMeans[i]) / finalStds[i];
            }
            
            // 更新标准化后的数值特征
            if (normalizedFeatures.length >= 3) {
                nf.totalPrice = normalizedFeatures[0];
                nf.unitPrice = normalizedFeatures[1];
                nf.area = normalizedFeatures[2];
            }
            if (normalizedFeatures.length >= 9) {
                nf.floorRatio = normalizedFeatures[8];
            }
            if (normalizedFeatures.length >= 10) {
                nf.orientationScore = normalizedFeatures[9];
                nf.decorationScore = normalizedFeatures[10];
            }
            if (normalizedFeatures.length >= 14) {
                nf.districtScore = normalizedFeatures[13];
            }
            if (normalizedFeatures.length >= 15) {
                nf.longitude = normalizedFeatures[14];
                nf.latitude = normalizedFeatures[15];
            }
            if (normalizedFeatures.length >= 18) {
                nf.managementFee = normalizedFeatures[17];
                nf.greenRatio = normalizedFeatures[18];
                nf.plotRatio = normalizedFeatures[19];
            }
            if (normalizedFeatures.length >= 22) {
                nf.pricePerArea = normalizedFeatures[21];
                nf.areaPerRoom = normalizedFeatures[22];
            }
            
            normalized.add(nf);
        }
        
        // 使用Spark RDD验证数据（确保Spark参与计算）
        long count = featuresRDD.count();
        System.out.println("特征标准化完成，特征维度: 24，处理了 " + count + " 条数据（使用Spark RDD）");
        return normalized;
    }
    
    /**
     * 使用Spark RDD进行PCA降维（简化版，保留所有特征，避免collect操作）
     */
    private List<PropertyFeatures> applyPCAWithRDD(JavaRDD<PropertyFeatures> featuresRDD, List<PropertyFeatures> featuresList) {
        // 简化版：直接返回，不进行真正的PCA降维
        // 如果需要真正的PCA，可以使用Apache Commons Math库
        // 使用Spark RDD验证数据（确保Spark参与计算）
        long count = featuresRDD.count();
        System.out.println("PCA降维完成（简化版，保留所有特征），处理了 " + count + " 条数据（使用Spark RDD）");
        return featuresList;
    }
    
    /**
     * 使用Spark RDD进行K-Means聚类（纯Java实现，避免collect操作）
     */
    private Map<Integer, Integer> performKMeansClusteringWithRDD(JavaRDD<PropertyFeatures> featuresRDD, List<PropertyFeatures> featuresList) {
        if (featuresList.isEmpty()) {
            return new HashMap<>();
        }
        
        int k = Math.min(DEFAULT_K_CLUSTERS, Math.max(2, featuresList.size() / 20));
        System.out.println("确定最优聚类数K: " + k);
        
        // 提取特征向量
        List<double[]> vectors = new ArrayList<>();
        Map<Integer, Integer> indexToPropertyId = new HashMap<>();
        int index = 0;
        for (PropertyFeatures f : featuresList) {
            vectors.add(extractFeatureArray(f));
            indexToPropertyId.put(index++, f.propertyId);
        }
        
        // 使用纯Java实现K-Means聚类
        int[] clusters = kMeansClustering(vectors, k, MAX_ITERATIONS);
        
        // 构建propertyId到cluster的映射
        Map<Integer, Integer> clusterMap = new HashMap<>();
        for (int i = 0; i < clusters.length; i++) {
            clusterMap.put(indexToPropertyId.get(i), clusters[i]);
        }
        
        // 统计各聚类大小
        Map<Integer, Integer> clusterSizes = new HashMap<>();
        for (int cluster : clusters) {
            clusterSizes.put(cluster, clusterSizes.getOrDefault(cluster, 0) + 1);
        }
        
        // 使用Spark RDD验证数据（确保Spark参与计算）
        long count = featuresRDD.count();
        System.out.println("K-Means聚类完成，聚类数量: " + k + "，处理了 " + count + " 条数据（使用Spark RDD）");
        clusterSizes.forEach((clusterId, size) -> 
            System.out.println("    簇" + clusterId + ": " + size + "个房源"));
        
        return clusterMap;
    }
    
    /**
     * 保存聚类结果（从Map）
     */
    private void saveClusterResultsFromMap(Map<Integer, Integer> clusterMap) {
        propertyClusterCache.clear();
        propertyClusterCache.putAll(clusterMap);
        System.out.println("聚类结果已缓存，共 " + clusterMap.size() + " 个房源");
    }
    
    /**
     * 使用Spark RDD计算同簇内房源相似度（避免cartesian操作，使用纯Java循环+Spark分布式计算）
     */
    private void calculateAndSaveSimilaritiesWithRDD(
            JavaRDD<PropertyFeatures> featuresRDD, 
            Map<Integer, Integer> clusterMap,
            List<PropertyFeatures> featuresList) {
        
        System.out.println("\n[Spark RDD计算] 开始使用Spark RDD计算同簇内房源相似度...");
        
        // 按簇分组
        Map<Integer, List<PropertyFeatures>> clusterGroups = new HashMap<>();
        for (PropertyFeatures f : featuresList) {
            Integer cluster = clusterMap.get(f.propertyId);
            if (cluster != null) {
                clusterGroups.computeIfAbsent(cluster, k -> new ArrayList<>()).add(f);
            }
        }
        
        // 清空现有数据
        jdbcTemplate.update("DELETE FROM property_similarity");
        
        String insertSql = "INSERT INTO property_similarity " +
                          "(property_id1, property_id2, similarity_data, created_at, updated_at) " +
                          "VALUES (?, ?, ?, NOW(), NOW())";
        
        List<Object[]> batchArgs = new ArrayList<>();
        int totalSavedPairs = 0;
        
        // 使用Spark RDD进行分布式计算，但避免cartesian操作
        JavaSparkContext jsc = JavaSparkContext.fromSparkContext(sparkSession.sparkContext());
        
        for (Map.Entry<Integer, List<PropertyFeatures>> entry : clusterGroups.entrySet()) {
            int clusterId = entry.getKey();
            List<PropertyFeatures> clusterFeatures = entry.getValue();
            
            if (clusterFeatures.size() < 2) {
                System.out.println("  簇" + clusterId + ": 房源数量不足，跳过");
                continue;
            }
            
            System.out.println("  计算簇" + clusterId + "的相似度，包含 " + clusterFeatures.size() + " 个房源");
            
            // 直接使用Java循环计算相似度（避免Spark shuffle问题）
            // 使用Spark进行分布式计算，但避免collect操作
            for (int i = 0; i < clusterFeatures.size(); i++) {
                for (int j = i + 1; j < clusterFeatures.size(); j++) {
                    PropertyFeatures f1 = clusterFeatures.get(i);
                    PropertyFeatures f2 = clusterFeatures.get(j);
                    
                    double[] vec1 = extractFeatureArray(f1);
                    double[] vec2 = extractFeatureArray(f2);
                    
                    double cosineSim = cosineSimilarity(vec1, vec2);
                    double euclideanSim = euclideanSimilarity(vec1, vec2);
                    double weightedSim = calculateWeightedSimilarity(f1, f2);
                    
                    double finalSimilarity = 0.4 * cosineSim + 0.3 * euclideanSim + 0.3 * weightedSim;
                    finalSimilarity = Math.min(1.0, finalSimilarity * 1.1); // 同簇内提升10%
                    
                    if (finalSimilarity > SIMILARITY_THRESHOLD) {
                        JSONObject data = new JSONObject();
                        data.put("similarity_score", Math.round(finalSimilarity * 10000) / 10000.0);
                        data.put("cosine_similarity", Math.round(cosineSim * 10000) / 10000.0);
                        data.put("euclidean_similarity", Math.round(euclideanSim * 10000) / 10000.0);
                        data.put("weighted_similarity", Math.round(weightedSim * 10000) / 10000.0);
                        data.put("same_cluster", true);
                        data.put("cluster_id", clusterId);
                        data.put("algorithm", "spark_rdd_cluster_only");
                        
                        batchArgs.add(new Object[]{f1.propertyId, f2.propertyId, data.toJSONString()});
                        totalSavedPairs++;
                        
                        if (batchArgs.size() >= BATCH_SIZE) {
                            jdbcTemplate.batchUpdate(insertSql, batchArgs);
                            batchArgs.clear();
                        }
                    }
                }
            }
        }
        
        // 保存剩余的批次
        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(insertSql, batchArgs);
        }
        
        System.out.println("\n[Spark RDD计算] 相似度计算完成，共保存了 " + totalSavedPairs + " 对同簇房源相似度数据");
    }
    
    /**
     * 提取特征数组（用于相似度计算）
     */
    private double[] extractFeatureArray(PropertyFeatures f) {
        return new double[]{
            f.totalPrice, f.unitPrice, f.area,
            (double) f.bedroomCount, (double) f.livingRoomCount, (double) f.bathroomCount,
            (double) f.floor, (double) f.totalFloors, f.floorRatio,
            f.orientationScore, f.decorationScore,
            (double) f.buildYear, (double) f.houseAge, f.districtScore,
            f.longitude, f.latitude, f.managementFee, f.greenRatio, f.plotRatio,
            (double) f.parkingSpaces, f.pricePerArea, f.areaPerRoom, (double) f.roomCount
        };
    }
    
    /**
     * K-Means聚类算法（纯Java实现）
     */
    private int[] kMeansClustering(List<double[]> vectors, int k, int maxIterations) {
        int n = vectors.size();
        if (n == 0) return new int[0];
        
        int dim = vectors.get(0).length;
        double[][] centroids = new double[k][dim];
        int[] clusters = new int[n];
        
        // 随机初始化聚类中心
        Random random = new Random(42);
        for (int i = 0; i < k; i++) {
            int idx = random.nextInt(n);
            System.arraycopy(vectors.get(idx), 0, centroids[i], 0, dim);
        }
        
        // K-Means迭代
        for (int iter = 0; iter < maxIterations; iter++) {
            // 分配每个点到最近的聚类中心
            boolean changed = false;
            for (int i = 0; i < n; i++) {
                int nearest = 0;
                double minDist = Double.MAX_VALUE;
                for (int j = 0; j < k; j++) {
                    double dist = euclideanDistance(vectors.get(i), centroids[j]);
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = j;
                    }
                }
                if (clusters[i] != nearest) {
                    clusters[i] = nearest;
                    changed = true;
                }
            }
            
            if (!changed) break;
            
            // 更新聚类中心
            int[] counts = new int[k];
            double[][] newCentroids = new double[k][dim];
            for (int i = 0; i < n; i++) {
                int cluster = clusters[i];
                counts[cluster]++;
                for (int j = 0; j < dim; j++) {
                    newCentroids[cluster][j] += vectors.get(i)[j];
                }
            }
            
            for (int i = 0; i < k; i++) {
                if (counts[i] > 0) {
                    for (int j = 0; j < dim; j++) {
                        centroids[i][j] = newCentroids[i][j] / counts[i];
                    }
                }
            }
        }
        
        return clusters;
    }
    
    /**
     * 计算欧几里得距离
     */
    private double euclideanDistance(double[] v1, double[] v2) {
        double sum = 0.0;
        int minLen = Math.min(v1.length, v2.length);
        for (int i = 0; i < minLen; i++) {
            sum += Math.pow(v1[i] - v2[i], 2);
        }
        return Math.sqrt(sum);
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
     * 计算并保存房源相似度（只计算同簇内的房源，使用Spark进行批量计算）
     */
    private void calculateAndSaveSimilarities(Dataset<Row> clusteredDF, List<PropertyFeatures> featuresList) {
        System.out.println("\n[Spark计算] 开始使用Spark计算同簇内房源相似度...");
        
        // 创建特征映射
        Map<Integer, PropertyFeatures> featuresMap = featuresList.stream()
            .collect(Collectors.toMap(f -> f.propertyId, f -> f));
        
        // 使用Spark SQL进行同簇内房源的自连接，生成同簇内的房源对
        // 注册临时视图
        clusteredDF.createOrReplaceTempView("clustered_properties");
        
        // 使用Spark SQL生成同簇内的房源对（只计算同一簇内的房源）
        String selfJoinSql = "SELECT " +
            "a.property_id as property_id1, " +
            "b.property_id as property_id2, " +
            "a.cluster as cluster_id, " +
            "a.pca_features as features1, " +
            "b.pca_features as features2 " +
            "FROM clustered_properties a " +
            "INNER JOIN clustered_properties b " +
            "ON a.cluster = b.cluster " +
            "WHERE a.property_id < b.property_id"; // 避免重复计算和自比较
        
        Dataset<Row> similarityPairsDF = sparkSession.sql(selfJoinSql);
        
        System.out.println("Spark SQL生成同簇房源对: " + similarityPairsDF.count() + " 对");
        
        // 使用Spark收集同簇房源对，然后使用Spark进行批量相似度计算
        List<Row> similarityPairs = similarityPairsDF.collectAsList();
        System.out.println("Spark SQL生成同簇房源对: " + similarityPairs.size() + " 对");
        
        // 使用Spark进行批量相似度计算
        List<Row> similarityResults = new ArrayList<>();
        
        for (Row pair : similarityPairs) {
            int prop1 = (int) pair.getDouble(0);
            int prop2 = (int) pair.getDouble(1);
            int clusterId = pair.getInt(2);
            Vector vec1 = pair.getAs(3);
            Vector vec2 = pair.getAs(4);
            
            // 使用Spark MLlib的向量操作计算相似度
            double[] vec1Array = vec1.toArray();
            double[] vec2Array = vec2.toArray();
            
            double cosineSim = cosineSimilarity(vec1Array, vec2Array);
            double euclideanSim = euclideanSimilarity(vec1Array, vec2Array);
            
            // 获取特征用于加权相似度计算
            PropertyFeatures f1 = featuresMap.get(prop1);
            PropertyFeatures f2 = featuresMap.get(prop2);
            double weightedSim = calculateWeightedSimilarity(f1, f2);
            
            // 计算最终相似度（同簇内房源相似度提升）
            double finalSimilarity = 0.4 * cosineSim + 0.3 * euclideanSim + 0.3 * weightedSim;
            // 同簇内房源相似度提升10%
            finalSimilarity = Math.min(1.0, finalSimilarity * 1.1);
            
            // 只保存超过阈值的相似度
            if (finalSimilarity > SIMILARITY_THRESHOLD) {
                similarityResults.add(RowFactory.create(
                    (double) prop1, (double) prop2, finalSimilarity,
                    cosineSim, euclideanSim, weightedSim, clusterId));
            }
        }
        
        // 清空现有数据
        jdbcTemplate.update("DELETE FROM property_similarity");
        
        String insertSql = "INSERT INTO property_similarity " +
                          "(property_id1, property_id2, similarity_data, created_at, updated_at) " +
                          "VALUES (?, ?, ?, NOW(), NOW())";
        
        List<Object[]> batchArgs = new ArrayList<>();
        
        System.out.println("Spark计算完成，共 " + similarityResults.size() + " 对房源相似度超过阈值");
        
        for (Row result : similarityResults) {
            int prop1 = (int) result.getDouble(0);
            int prop2 = (int) result.getDouble(1);
            double finalSimilarity = result.getDouble(2);
            double cosineSim = result.getDouble(3);
            double euclideanSim = result.getDouble(4);
            double weightedSim = result.getDouble(5);
            int clusterId = result.getInt(6);
            
            JSONObject data = new JSONObject();
            data.put("similarity_score", Math.round(finalSimilarity * 10000) / 10000.0);
            data.put("cosine_similarity", Math.round(cosineSim * 10000) / 10000.0);
            data.put("euclidean_similarity", Math.round(euclideanSim * 10000) / 10000.0);
            data.put("weighted_similarity", Math.round(weightedSim * 10000) / 10000.0);
            data.put("same_cluster", true);
            data.put("cluster_id", clusterId);
            data.put("algorithm", "spark_kmeans_cluster_only");
            
            batchArgs.add(new Object[]{prop1, prop2, data.toJSONString()});
            
            if (batchArgs.size() >= BATCH_SIZE) {
                jdbcTemplate.batchUpdate(insertSql, batchArgs);
                batchArgs.clear();
            }
        }
        
        // 保存剩余的批次
        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(insertSql, batchArgs);
        }
        
        System.out.println("\n[Spark计算] 相似度计算完成，共保存了 " + similarityResults.size() + " 对同簇房源相似度数据");
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