package com.example.service.predict_zhz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ai.onnxruntime.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 房价预测服务 - 使用ONNX Runtime加载和运行模型
 * 不再依赖Python脚本，完全使用Java实现
 */
@Service
public class HousePricePredictionService {

    private static final Map<String, String> CITY_FOLDER_MAP;

    static {
        Map<String, String> mapping = new HashMap<String, String>();
        mapping.put("北京", "beijng");
        mapping.put("beijing", "beijng");
        mapping.put("bj", "beijng");
        mapping.put("上海", "shanghai");
        mapping.put("shanghai", "shanghai");
        mapping.put("sh", "shanghai");
        mapping.put("天津", "tianjin");
        mapping.put("tianjin", "tianjin");
        mapping.put("tj", "tianjin");
        mapping.put("石家庄", "shijiazhuang");
        mapping.put("shijiazhuang", "shijiazhuang");
        mapping.put("sjz", "shijiazhuang");
        CITY_FOLDER_MAP = Collections.unmodifiableMap(mapping);
    }

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path modelBaseDir;
    
    // 缓存已加载的ONNX模型和配置
    private final Map<String, OrtSession> sessionCache = new HashMap<>();
    private final Map<String, ModelConfig> configCache = new HashMap<>();

    public HousePricePredictionService(
            @Value("${predictor.model-base-dir:src/main/java/com/example/service/predict_zhz}") String modelBaseDir) {
        // 处理路径：如果是相对路径，从当前工作目录解析
        Path basePath = Paths.get(modelBaseDir);
        if (!basePath.isAbsolute()) {
            Path currentDir = Paths.get("").toAbsolutePath().normalize();
            // 检查当前目录是否是 backend 目录
            String currentDirName = currentDir.getFileName().toString();
            if ("backend".equals(currentDirName)) {
                // 当前目录就是 backend，直接使用
                this.modelBaseDir = currentDir.resolve(modelBaseDir).normalize();
            } else {
                // 当前目录不是 backend，尝试查找 backend 目录
                Path backendDir = currentDir.resolve("backend");
                if (Files.exists(backendDir) && Files.isDirectory(backendDir)) {
                    this.modelBaseDir = backendDir.resolve(modelBaseDir).normalize();
                } else {
                    // 如果找不到 backend 目录，假设当前目录就是项目根目录
                    this.modelBaseDir = currentDir.resolve(modelBaseDir).normalize();
                }
            }
        } else {
            this.modelBaseDir = basePath.normalize();
        }
    }

    /**
     * 执行预测，返回万元/㎡。
     */
    public double predict(String city, Map<String, Object> features) throws Exception {
        System.out.println("\n========== House Price Prediction Request ==========");
        System.out.println("City: " + city);
        System.out.println("Input Features:");
        for (Map.Entry<String, Object> entry : features.entrySet()) {
            System.out.println("  - " + entry.getKey() + ": " + entry.getValue());
        }
        
        String folderName = resolveCityFolder(city);
        Path cityDir = modelBaseDir.resolve(folderName);
        Path onnxModelPath = cityDir.resolve("house_price_model.onnx");
        Path configPath = cityDir.resolve("model_config.json");

        System.out.println("\n[Step 1] Resolving model paths:");
        System.out.println("  - City folder: " + folderName);
        System.out.println("  - Model path: " + onnxModelPath);
        System.out.println("  - Config path: " + configPath);

        if (!Files.exists(onnxModelPath)) {
            System.err.println("[ERROR] ONNX model file not found: " + onnxModelPath);
            throw new FileNotFoundException("未找到ONNX模型文件：" + onnxModelPath + 
                    "\n请先运行 convert_model_to_onnx.py 脚本将pkl模型转换为ONNX格式");
        }
        if (!Files.exists(configPath)) {
            System.err.println("[ERROR] Model config file not found: " + configPath);
            throw new FileNotFoundException("未找到模型配置：" + configPath);
        }
        
        System.out.println("[Step 2] Model files found, loading model and config...");

        // 获取或加载模型会话和配置
        boolean isNewSession = !sessionCache.containsKey(folderName);
        OrtSession session = getOrLoadSession(folderName, onnxModelPath);
        if (isNewSession) {
            System.out.println("  - ONNX model loaded successfully (new session)");
        } else {
            System.out.println("  - Using cached ONNX session");
        }
        
        ModelConfig config = getOrLoadConfig(folderName, configPath);
        System.out.println("  - Model config loaded: " + config.featureColumns.size() + " features");

        // 构建特征向量
        System.out.println("\n[Step 3] Building feature vector...");
        float[] featureVector = buildFeatureVector(config, features);
        System.out.println("  - Feature vector size: " + featureVector.length);
        System.out.println("  - First 10 features: " + java.util.Arrays.toString(
                java.util.Arrays.copyOf(featureVector, Math.min(10, featureVector.length))));

        // 执行预测
        System.out.println("\n[Step 4] Running ONNX model prediction...");
        long startTime = System.currentTimeMillis();
        
        try (OnnxTensor inputTensor = OnnxTensor.createTensor(
                OrtEnvironment.getEnvironment(), 
                new float[][]{featureVector})) {
            
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("float_input", inputTensor);
            
            try (OrtSession.Result result = session.run(inputs)) {
                // ONNX模型输出通常是二维数组 [1, 1]
                float[][] output = (float[][]) result.get(0).getValue();
                double predictedPrice = output[0][0];
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                
                System.out.println("\n[Step 5] Prediction completed:");
                System.out.println("  - Predicted price: " + String.format("%.4f", predictedPrice) + " 万元/㎡");
                System.out.println("  - Prediction time: " + duration + " ms");
                System.out.println("==============================================\n");
                
                return predictedPrice;
            }
        }
    }

    /**
     * 获取或加载ONNX会话（带缓存）
     */
    private OrtSession getOrLoadSession(String folderName, Path onnxModelPath) throws Exception {
        synchronized (sessionCache) {
            if (sessionCache.containsKey(folderName)) {
                System.out.println("  - Using cached ONNX session for city: " + folderName);
                return sessionCache.get(folderName);
            }
            
            System.out.println("  - Loading new ONNX model from: " + onnxModelPath);
            long modelLoadStart = System.currentTimeMillis();
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            OrtSession session = env.createSession(onnxModelPath.toString(), opts);
            long modelLoadEnd = System.currentTimeMillis();
            System.out.println("  - Model loaded in " + (modelLoadEnd - modelLoadStart) + " ms");
            sessionCache.put(folderName, session);
            return session;
        }
    }

    /**
     * 获取或加载模型配置（带缓存）
     */
    private ModelConfig getOrLoadConfig(String folderName, Path configPath) throws Exception {
        synchronized (configCache) {
            if (configCache.containsKey(folderName)) {
                return configCache.get(folderName);
            }
            
            JsonNode configNode = objectMapper.readTree(configPath.toFile());
            ModelConfig config = new ModelConfig();
            config.featureColumns = new ArrayList<>();
            config.meanValues = new HashMap<>();
            config.scalerInfo = configNode.get("scaler_info");
            
            // 读取特征列
            JsonNode featureColumnsNode = configNode.get("feature_columns");
            if (featureColumnsNode != null && featureColumnsNode.isArray()) {
                for (JsonNode col : featureColumnsNode) {
                    config.featureColumns.add(col.asText());
                }
            }
            
            // 读取均值
            if (config.scalerInfo != null && config.scalerInfo.has("means")) {
                JsonNode meansNode = config.scalerInfo.get("means");
                meansNode.fields().forEachRemaining(entry -> {
                    JsonNode valueNode = entry.getValue();
                    if (!valueNode.isNull()) {
                        config.meanValues.put(entry.getKey(), valueNode.asDouble());
                    }
                });
            }
            
            configCache.put(folderName, config);
            return config;
        }
    }

    /**
     * 构建特征向量
     */
    private float[] buildFeatureVector(ModelConfig config, Map<String, Object> features) {
        float[] vector = new float[config.featureColumns.size()];
        int providedCount = 0;
        int defaultCount = 0;
        
        for (int i = 0; i < config.featureColumns.size(); i++) {
            String column = config.featureColumns.get(i);
            Object value = features.get(column);
            
            if (value == null) {
                // 使用均值填充
                Double meanValue = config.meanValues.get(column);
                vector[i] = meanValue != null ? meanValue.floatValue() : 0.0f;
                defaultCount++;
            } else if (value instanceof Number) {
                vector[i] = ((Number) value).floatValue();
                providedCount++;
            } else {
                // 尝试转换为数字
                try {
                    vector[i] = Float.parseFloat(value.toString());
                    providedCount++;
                } catch (NumberFormatException e) {
                    // 转换失败，使用均值或0
                    Double meanValue = config.meanValues.get(column);
                    vector[i] = meanValue != null ? meanValue.floatValue() : 0.0f;
                    defaultCount++;
                }
            }
        }
        
        System.out.println("  - Features provided: " + providedCount);
        System.out.println("  - Features using defaults: " + defaultCount);
        System.out.println("  - Total features: " + vector.length);
        
        return vector;
    }

    private String resolveCityFolder(String cityName) {
        if (cityName == null) {
            throw new IllegalArgumentException("city 不能为空");
        }
        String normalized = cityName.trim().toLowerCase(Locale.ROOT);
        String folder = CITY_FOLDER_MAP.get(normalized);
        if (folder == null) {
            folder = CITY_FOLDER_MAP.get(cityName.trim());
        }
        if (folder == null) {
            throw new IllegalArgumentException("当前城市暂不支持预测：" + cityName);
        }
        return folder;
    }

    /**
     * 模型配置内部类
     */
    private static class ModelConfig {
        List<String> featureColumns;
        Map<String, Double> meanValues;
        JsonNode scalerInfo;
    }

    /**
     * 清理资源（可选，在应用关闭时调用）
     */
    public void cleanup() {
        synchronized (sessionCache) {
            for (OrtSession session : sessionCache.values()) {
                try {
                    session.close();
                } catch (Exception e) {
                    // 忽略关闭错误
                }
            }
            sessionCache.clear();
            configCache.clear();
        }
    }
}
