package com.example.service.predict_zhz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 调用城市房价模型的服务。
 * 通过 Python 脚本加载 pkl 模型并返回预测结果。
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
    private final Path pythonScript;
    private final String pythonExecutable;

    public HousePricePredictionService(
            @Value("${predictor.model-base-dir:src/main/java/com/example/service/predict_zhz}") String modelBaseDir,
            @Value("${predictor.python-exec:python}") String pythonExecutable) {
        this.modelBaseDir = Paths.get(modelBaseDir).toAbsolutePath().normalize();
        this.pythonScript = this.modelBaseDir.resolve("predict_price.py");
        this.pythonExecutable = pythonExecutable;
    }

    /**
     * 调用对应城市的模型进行预测。
     *
     * @param cityName 城市名称
     * @param features 特征字段
     * @return 预测结果（万元/㎡）
     */
    public double predictPricePerSquareMeter(String cityName, Map<String, Object> features) throws Exception {
        String folderName = resolveCityFolder(cityName);
        Path cityDir = modelBaseDir.resolve(folderName);
        Path modelPath = cityDir.resolve("house_price_model.pkl");
        Path configPath = cityDir.resolve("model_config.json");

        if (!Files.exists(modelPath)) {
            throw new FileNotFoundException("未找到模型文件：" + modelPath);
        }
        if (!Files.exists(configPath)) {
            throw new FileNotFoundException("未找到模型配置：" + configPath);
        }
        if (!Files.exists(pythonScript)) {
            throw new FileNotFoundException("未找到预测脚本：" + pythonScript);
        }

        Map<String, Object> request = new HashMap<String, Object>();
        request.put("modelPath", modelPath.toString());
        request.put("configPath", configPath.toString());
        request.put("features", features);

        ProcessBuilder pb = new ProcessBuilder(pythonExecutable, pythonScript.toString());
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // 向脚本写入请求
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
            objectMapper.writeValue(writer, request);
        }

        String output = readStream(process.getInputStream());
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("预测脚本执行失败：" + output);
        }

        JsonNode node = objectMapper.readTree(output);
        if (!node.has("prediction")) {
            throw new IllegalStateException("预测脚本未返回 prediction 字段：" + output);
        }
        return node.get("prediction").asDouble();
    }

    /**
     * 将输入城市映射到模型文件夹名称。
     */
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

    private String readStream(java.io.InputStream inputStream) throws IOException {
        byte[] buffer = new byte[4096];
        int len;
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        while ((len = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        return baos.toString(StandardCharsets.UTF_8.name());
    }
}

