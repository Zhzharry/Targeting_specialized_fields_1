package com.example.service.hadoop;

import com.example.config.HadoopConfig;
import com.example.hadoop.mapreduce.OthersAlsoViewedJob;
import com.example.hadoop.mapreduce.UserSimilarityJob;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hadoop服务类 (Java 8 兼容版本)
 * 负责运行MapReduce作业、管理数据和缓存结果
 * 
 * 位置：backend/src/main/java/com/example/service/hadoop/HadoopRecommendationService.java
 */
@Service
public class HadoopRecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(HadoopRecommendationService.class);

    @Autowired
    private Configuration hadoopConfiguration;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private HadoopConfig hadoopConfig;

    @Value("${hadoop.data.dir:/tmp/hadoop-data}")
    private String hadoopDataDir;

    // 缓存：用户ID -> 推荐列表
    private final ConcurrentHashMap<Long, List<RecommendationResult>> recommendationCache = 
            new ConcurrentHashMap<Long, List<RecommendationResult>>();
    
    // 缓存：用户对 -> 相似度
    private final ConcurrentHashMap<String, Double> similarityCache = 
            new ConcurrentHashMap<String, Double>();
    
    // 最后更新时间
    private volatile LocalDateTime lastUpdateTime;

    @PostConstruct
    public void init() {
        // 创建必要的目录
        try {
            Files.createDirectories(Paths.get(hadoopDataDir, "input"));
            Files.createDirectories(Paths.get(hadoopDataDir, "output"));
            Files.createDirectories(Paths.get(hadoopDataDir, "tmp"));
            logger.info("Hadoop数据目录初始化完成: {}", hadoopDataDir);
        } catch (IOException e) {
            logger.error("初始化Hadoop数据目录失败", e);
        }
    }

    /**
     * 定时任务：每小时执行一次推荐计算
     */
    @Scheduled(fixedRate = 3600000) // 1小时
    @Async
    public void scheduledRecommendationUpdate() {
        logger.info("开始定时推荐计算任务");
        try {
            runFullRecommendationPipeline();
        } catch (Exception e) {
            logger.error("定时推荐计算任务失败", e);
        }
    }

    /**
     * 运行完整的推荐计算流水线
     */
    public void runFullRecommendationPipeline() throws Exception {
        logger.info("开始运行Hadoop推荐计算流水线");
        
        // 1. 导出用户行为数据到文件
        String behaviorInputPath = exportUserBehaviorData();
        
        // 2. 运行用户相似度计算
        String similarityOutputPath = hadoopDataDir + "/output/similarity_" + System.currentTimeMillis();
        runUserSimilarityJob(behaviorInputPath, similarityOutputPath);
        
        // 3. 运行"其他用户也在看"推荐计算
        String recommendationOutputPath = hadoopDataDir + "/output/recommendation_" + System.currentTimeMillis();
        runOthersAlsoViewedJob(similarityOutputPath, behaviorInputPath, recommendationOutputPath);
        
        // 4. 加载结果到缓存
        loadSimilarityResults(similarityOutputPath);
        loadRecommendationResults(recommendationOutputPath);
        
        // 5. 更新数据库中的相似度数据
        updateSimilarityDatabase();
        
        lastUpdateTime = LocalDateTime.now();
        logger.info("Hadoop推荐计算流水线完成");
    }

    /**
     * 导出用户行为数据到文件
     */
    private String exportUserBehaviorData() throws IOException {
        String outputPath = hadoopDataDir + "/input/user_behavior_" + System.currentTimeMillis() + ".txt";
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            // 导出浏览历史
            String browsingSql = 
                "SELECT user_id, property_id, 1 as behavior_type, " +
                "COALESCE(JSON_EXTRACT(behavior_data, '$.view_count'), 1) as weight, " +
                "UNIX_TIMESTAMP(created_at) as timestamp " +
                "FROM browsing_history " +
                "WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
            
            jdbcTemplate.query(browsingSql, rs -> {
                try {
                    writer.write(String.format("%d\t%d\t%d\t%.2f\t%d%n",
                        rs.getLong("user_id"),
                        rs.getLong("property_id"),
                        rs.getInt("behavior_type"),
                        rs.getDouble("weight"),
                        rs.getLong("timestamp")));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            
            // 导出收藏数据
            String favoritesSql = 
                "SELECT user_id, property_id, 2 as behavior_type, 2.0 as weight, " +
                "UNIX_TIMESTAMP(created_at) as timestamp " +
                "FROM favorites " +
                "WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
            
            jdbcTemplate.query(favoritesSql, rs -> {
                try {
                    writer.write(String.format("%d\t%d\t%d\t%.2f\t%d%n",
                        rs.getLong("user_id"),
                        rs.getLong("property_id"),
                        rs.getInt("behavior_type"),
                        rs.getDouble("weight"),
                        rs.getLong("timestamp")));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            
            // 导出搜索历史
            String searchSql = 
                "SELECT user_id, 0 as property_id, 3 as behavior_type, 0.5 as weight, " +
                "UNIX_TIMESTAMP(created_at) as timestamp " +
                "FROM search_history " +
                "WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
            
            jdbcTemplate.query(searchSql, rs -> {
                try {
                    writer.write(String.format("%d\t%d\t%d\t%.2f\t%d%n",
                        rs.getLong("user_id"),
                        rs.getLong("property_id"),
                        rs.getInt("behavior_type"),
                        rs.getDouble("weight"),
                        rs.getLong("timestamp")));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        
        logger.info("用户行为数据导出完成: {}", outputPath);
        return outputPath;
    }

    /**
     * 运行用户相似度计算MapReduce作业
     */
    private void runUserSimilarityJob(String inputPath, String outputPath) throws Exception {
        logger.info("开始运行用户相似度计算作业");
        
        String[] args = {inputPath, outputPath};
        int exitCode = ToolRunner.run(hadoopConfiguration, new UserSimilarityJob(), args);
        
        if (exitCode != 0) {
            throw new RuntimeException("用户相似度计算作业失败，退出码: " + exitCode);
        }
        
        logger.info("用户相似度计算作业完成");
    }

    /**
     * 运行"其他用户也在看"推荐MapReduce作业
     */
    private void runOthersAlsoViewedJob(String similarityPath, String behaviorPath, String outputPath) throws Exception {
        logger.info("开始运行其他用户也在看推荐作业");
        
        String[] args = {similarityPath, behaviorPath, outputPath};
        int exitCode = ToolRunner.run(hadoopConfiguration, new OthersAlsoViewedJob(), args);
        
        if (exitCode != 0) {
            throw new RuntimeException("其他用户也在看推荐作业失败，退出码: " + exitCode);
        }
        
        logger.info("其他用户也在看推荐作业完成");
    }

    /**
     * 加载相似度结果到缓存
     */
    private void loadSimilarityResults(String outputPath) throws IOException {
        similarityCache.clear();
        
        File outputDir = new File(outputPath);
        if (outputDir.isDirectory()) {
            File[] files = outputDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith("part-");
                }
            });
            
            if (files != null) {
                for (File file : files) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] parts = line.split("\t");
                            if (parts.length >= 2) {
                                similarityCache.put(parts[0], Double.parseDouble(parts[1]));
                            }
                        }
                    }
                }
            }
        }
        
        logger.info("加载了 {} 条用户相似度数据", similarityCache.size());
    }

    /**
     * 加载推荐结果到缓存
     */
    private void loadRecommendationResults(String outputPath) throws IOException {
        recommendationCache.clear();
        
        File outputDir = new File(outputPath);
        if (outputDir.isDirectory()) {
            File[] files = outputDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith("part-");
                }
            });
            
            if (files != null) {
                for (File file : files) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] parts = line.split("\t");
                            if (parts.length >= 4) {
                                Long userId = Long.parseLong(parts[0]);
                                Long propertyId = Long.parseLong(parts[1]);
                                Double score = Double.parseDouble(parts[2]);
                                String reason = parts[3];
                                
                                List<RecommendationResult> userResults = recommendationCache.get(userId);
                                if (userResults == null) {
                                    userResults = new ArrayList<RecommendationResult>();
                                    recommendationCache.put(userId, userResults);
                                }
                                userResults.add(new RecommendationResult(propertyId, score, reason));
                            }
                        }
                    }
                }
            }
        }
        
        // 按分数排序
        for (List<RecommendationResult> results : recommendationCache.values()) {
            Collections.sort(results, new Comparator<RecommendationResult>() {
                @Override
                public int compare(RecommendationResult a, RecommendationResult b) {
                    return Double.compare(b.score, a.score);
                }
            });
        }
        
        logger.info("加载了 {} 个用户的推荐数据", recommendationCache.size());
    }

    /**
     * 更新数据库中的相似度数据
     */
    private void updateSimilarityDatabase() {
        String insertSql = 
            "INSERT INTO user_similarity (user_id1, user_id2, similarity_data, created_at, updated_at) " +
            "VALUES (?, ?, ?, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE " +
            "similarity_data = VALUES(similarity_data), " +
            "updated_at = NOW()";
        
        for (Map.Entry<String, Double> entry : similarityCache.entrySet()) {
            String[] userIds = entry.getKey().split(",");
            if (userIds.length == 2) {
                try {
                    Long userId1 = Long.parseLong(userIds[0]);
                    Long userId2 = Long.parseLong(userIds[1]);
                    String similarityJson = String.format(
                        "{\"similarity_score\": %.4f, \"algorithm\": \"hadoop_mapreduce\", \"version\": \"1.0\"}",
                        entry.getValue());
                    
                    jdbcTemplate.update(insertSql, userId1, userId2, similarityJson);
                } catch (Exception e) {
                    logger.warn("更新用户相似度失败: {}", entry.getKey(), e);
                }
            }
        }
        
        logger.info("数据库相似度数据更新完成");
    }

    /**
     * 获取用户的"其他用户也在看"推荐（从缓存）
     */
    public List<RecommendationResult> getRecommendationsFromCache(Long userId, int limit) {
        List<RecommendationResult> results = recommendationCache.get(userId);
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }
        return results.subList(0, Math.min(limit, results.size()));
    }

    /**
     * 获取相似用户（从缓存）
     */
    public List<SimilarUser> getSimilarUsersFromCache(Long userId, int limit) {
        List<SimilarUser> similarUsers = new ArrayList<SimilarUser>();
        
        for (Map.Entry<String, Double> entry : similarityCache.entrySet()) {
            String[] userIds = entry.getKey().split(",");
            if (userIds.length == 2) {
                Long uid1 = Long.parseLong(userIds[0]);
                Long uid2 = Long.parseLong(userIds[1]);
                
                if (uid1.equals(userId)) {
                    similarUsers.add(new SimilarUser(uid2, entry.getValue()));
                } else if (uid2.equals(userId)) {
                    similarUsers.add(new SimilarUser(uid1, entry.getValue()));
                }
            }
        }
        
        Collections.sort(similarUsers, new Comparator<SimilarUser>() {
            @Override
            public int compare(SimilarUser a, SimilarUser b) {
                return Double.compare(b.similarity, a.similarity);
            }
        });
        
        return similarUsers.subList(0, Math.min(limit, similarUsers.size()));
    }

    /**
     * 检查缓存是否可用
     */
    public boolean isCacheAvailable() {
        return lastUpdateTime != null && !recommendationCache.isEmpty();
    }

    /**
     * 获取最后更新时间
     */
    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    /**
     * 推荐结果内部类
     */
    public static class RecommendationResult {
        public final Long propertyId;
        public final Double score;
        public final String reason;

        public RecommendationResult(Long propertyId, Double score, String reason) {
            this.propertyId = propertyId;
            this.score = score;
            this.reason = reason;
        }
    }

    /**
     * 相似用户内部类
     */
    public static class SimilarUser {
        public final Long userId;
        public final Double similarity;

        public SimilarUser(Long userId, Double similarity) {
            this.userId = userId;
            this.similarity = similarity;
        }
    }
}