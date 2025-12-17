// SimilarityController.java
package com.example.controller;

import com.example.service.PropertySimilarityService;
import com.example.service.PropertySimilarityCFService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 房源相似度计算REST API控制器
 */
@RestController
@RequestMapping("/api/similarity")
@CrossOrigin(origins = "*")
public class SimilarityController {
    
    @Autowired
    private PropertySimilarityService similarityService;
    
    @Autowired
    private PropertySimilarityCFService cfService;
    
    /**
     * 手动触发完整的相似度计算
     * POST /api/similarity/calculate
     */
    @PostMapping("/calculate")
    public ResponseEntity<Map<String, Object>> calculateSimilarity() {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            // 异步执行计算
            CompletableFuture.runAsync(() -> {
                similarityService.calculatePropertySimilarityFull();
                cfService.calculatePropertySimilarityCF();
                cfService.mergeSimilarityResults();
            });
            
            result.put("success", true);
            result.put("message", "相似度计算任务已启动，请稍后查看结果");
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "计算失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
    
    /**
     * 只执行基于内容的相似度计算
     * POST /api/similarity/calculate/content
     */
    @PostMapping("/calculate/content")
    public ResponseEntity<Map<String, Object>> calculateContentSimilarity() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            long startTime = System.currentTimeMillis();
            similarityService.calculatePropertySimilarityFull();
            long endTime = System.currentTimeMillis();
            
            result.put("success", true);
            result.put("message", "基于内容的相似度计算完成");
            result.put("duration_ms", endTime - startTime);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "计算失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
    
    /**
     * 只执行协同过滤相似度计算
     * POST /api/similarity/calculate/cf
     */
    @PostMapping("/calculate/cf")
    public ResponseEntity<Map<String, Object>> calculateCFSimilarity() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            long startTime = System.currentTimeMillis();
            cfService.calculatePropertySimilarityCF();
            long endTime = System.currentTimeMillis();
            
            result.put("success", true);
            result.put("message", "协同过滤相似度计算完成");
            result.put("duration_ms", endTime - startTime);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "计算失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
    
    /**
     * 使用Hadoop MapReduce计算相似度（大数据量场景）
     * POST /api/similarity/calculate/mapreduce
     */
    @PostMapping("/calculate/mapreduce")
    public ResponseEntity<Map<String, Object>> calculateWithMapReduce() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            long startTime = System.currentTimeMillis();
            cfService.calculateSimilarityWithMapReduce();
            long endTime = System.currentTimeMillis();
            
            result.put("success", true);
            result.put("message", "MapReduce相似度计算完成");
            result.put("duration_ms", endTime - startTime);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "计算失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
    
    /**
     * 获取指定房源的相似房源列表
     * GET /api/similarity/properties/{propertyId}?limit=10
     */
    @GetMapping("/properties/{propertyId}")
    public ResponseEntity<Map<String, Object>> getSimilarProperties(
            @PathVariable int propertyId,
            @RequestParam(defaultValue = "10") int limit) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<Map<String, Object>> similarProperties = 
                similarityService.getSimilarProperties(propertyId, limit);
            
            result.put("success", true);
            result.put("property_id", propertyId);
            result.put("similar_properties", similarProperties);
            result.put("count", similarProperties.size());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
    
    /**
     * 融合多种相似度结果
     * POST /api/similarity/merge
     */
    @PostMapping("/merge")
    public ResponseEntity<Map<String, Object>> mergeSimilarityResults() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            cfService.mergeSimilarityResults();
            
            result.put("success", true);
            result.put("message", "相似度结果融合完成");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "融合失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
    
    /**
     * 获取相似度计算统计信息
     * GET /api/similarity/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics(
            @Autowired org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 总相似度记录数
            Integer totalPairs = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM property_similarity", Integer.class);
            
            // 平均相似度
            Double avgSimilarity = jdbcTemplate.queryForObject(
                "SELECT AVG(CAST(JSON_EXTRACT(similarity_data, '$.similarity_score') AS DECIMAL(8,4))) " +
                "FROM property_similarity", Double.class);
            
            // 同一聚类的记录数
            Integer sameClusterPairs = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM property_similarity " +
                "WHERE JSON_EXTRACT(similarity_data, '$.same_cluster') = true", Integer.class);
            
            // 有协同过滤数据的记录数
            Integer cfPairs = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM property_similarity " +
                "WHERE JSON_EXTRACT(similarity_data, '$.cf_similarity') IS NOT NULL", Integer.class);
            
            result.put("success", true);
            result.put("total_similarity_pairs", totalPairs);
            result.put("average_similarity_score", avgSimilarity);
            result.put("same_cluster_pairs", sameClusterPairs);
            result.put("cf_similarity_pairs", cfPairs);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取统计信息失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
}