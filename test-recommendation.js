#!/usr/bin/env node

/**
 * 推荐API测试脚本
 * 用于验证相似度NaN问题是否已修复
 */

const axios = require('axios');

const BASE_URL = 'http://localhost:8080';

async function testRecommendationAPI() {
    try {
        console.log('🔍 测试推荐API...');

        // 测试获取推荐房源
        const response = await axios.get(`${BASE_URL}/api/others-also-viewed`, {
            params: { userId: 1, limit: 10 }
        });

        console.log(`✅ API响应成功，获取到 ${response.data.length} 个推荐房源`);

        // 检查每个房源的score字段
        let validScores = 0;
        let invalidScores = 0;

        response.data.forEach((property, index) => {
            const score = property.score;
            if (score !== null && score !== undefined && !isNaN(score) && score >= 0 && score <= 1) {
                validScores++;
                console.log(`  ✅ 房源 ${index + 1}: 相似度 ${(score * 100).toFixed(1)}%`);
            } else {
                invalidScores++;
                console.log(`  ❌ 房源 ${index + 1}: 相似度无效 (${score})`);
            }
        });

        console.log(`\n📊 测试结果:`);
        console.log(`  有效分数: ${validScores}`);
        console.log(`  无效分数: ${invalidScores}`);

        if (invalidScores === 0) {
            console.log('🎉 所有相似度分数都有效！NaN问题已修复。');
            return true;
        } else {
            console.log('⚠️  仍有无效的相似度分数，需要进一步检查。');
            return false;
        }

    } catch (error) {
        console.error('❌ API测试失败:', error.message);
        return false;
    }
}

// 如果直接运行此脚本
if (require.main === module) {
    testRecommendationAPI().then(success => {
        process.exit(success ? 0 : 1);
    });
}

module.exports = { testRecommendationAPI };