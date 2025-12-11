package com.example.service.predict_zhz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
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
 * 调用房价模型的服务，封装城市映射与Python脚本调用逻辑。
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
        this.pythonScript = this.modelBaseDir.resolve("predict_price.py");
        this.pythonExecutable = pythonExecutable;
    }

    /**
     * 执行预测，返回万元/㎡。
     */
    public double predict(String city, Map<String, Object> features) throws Exception {
        String folderName = resolveCityFolder(city);
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

        ProcessBuilder builder = new ProcessBuilder(pythonExecutable, pythonScript.toString());
        // Java 8 兼容：重定向 stderr 到 null 设备（Windows 使用 NUL，Linux/Mac 使用 /dev/null）
        String os = System.getProperty("os.name").toLowerCase();
        File nullFile;
        if (os.contains("win")) {
            // Windows 使用 NUL
            nullFile = new File("NUL");
        } else {
            // Linux/Mac 使用 /dev/null
            nullFile = new File("/dev/null");
        }
        builder.redirectError(nullFile);  // 丢弃 stderr 输出（Java 8 兼容）
        Process process = builder.start();

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
            objectMapper.writeValue(writer, request);
        }

        String output = readStream(process);
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

    private String readStream(Process process) throws IOException {
        byte[] buffer = new byte[4096];
        int len;
        java.io.InputStream inputStream = process.getInputStream();
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        while ((len = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        return baos.toString(StandardCharsets.UTF_8.name());
    }
}

