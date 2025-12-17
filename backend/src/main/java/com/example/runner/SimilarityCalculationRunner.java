// SimilarityCalculationRunner.java
package com.example.runner;

import com.example.service.PropertySimilarityService;
import com.example.service.PropertySimilarityCFService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 相似度计算启动运行器
 * 
 * 可以通过配置文件控制是否在启动时执行：
 * similarity.calculation.on-startup=true
 * 
 * 也可以通过REST API手动触发
 */
@Component
@Order(100)
@ConditionalOnProperty(name = "similarity.calculation.on-startup", havingValue = "true", matchIfMissing = false)
public class SimilarityCalculationRunner implements CommandLineRunner {
    
    @Autowired
    private PropertySimilarityService similarityService;
    
    @Autowired
    private PropertySimilarityCFService cfService;
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("================================================");
        System.out.println("启动时执行房源相似度计算...");
        System.out.println("================================================");
        
        try {
            // 1. 执行基于内容的相似度计算（包含聚类）
            similarityService.calculatePropertySimilarityFull();
            
            // 2. 执行协同过滤相似度计算
            cfService.calculatePropertySimilarityCF();
            
            // 3. 融合相似度结果
            cfService.mergeSimilarityResults();
            
            System.out.println("================================================");
            System.out.println("所有相似度计算完成！");
            System.out.println("================================================");
            
        } catch (Exception e) {
            System.err.println("相似度计算失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}