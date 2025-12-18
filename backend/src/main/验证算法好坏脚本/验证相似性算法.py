#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
相似性算法验证脚本
用于验证房源相似性和用户相似性计算结果的优劣好坏
"""

import pymysql
import json
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from collections import defaultdict
from typing import Dict, List, Tuple
import warnings
warnings.filterwarnings('ignore')

# 设置中文字体
plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei', 'Arial Unicode MS']
plt.rcParams['axes.unicode_minus'] = False

# 数据库配置
DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'bigdata_user',
    'password': '123456',
    'database': 'bigdata',
    'charset': 'utf8mb4'
}


class SimilarityValidator:
    """相似性算法验证器"""
    
    def __init__(self):
        self.conn = pymysql.connect(**DB_CONFIG)
        self.cursor = self.conn.cursor()
    
    def close(self):
        """关闭数据库连接"""
        self.cursor.close()
        self.conn.close()
    
    def validate_property_similarity(self):
        """验证房源相似性"""
        print("=" * 80)
        print("房源相似性验证")
        print("=" * 80)
        
        # 1. 获取相似度数据
        print("\n[1] 获取房源相似度数据...")
        sql = """
            SELECT 
                property_id1,
                property_id2,
                similarity_data,
                created_at
            FROM property_similarity
            ORDER BY property_id1, property_id2
        """
        self.cursor.execute(sql)
        similarity_data = self.cursor.fetchall()
        
        if not similarity_data:
            print("❌ 未找到房源相似度数据，请先运行相似度计算算法")
            return
        
        print(f"✓ 找到 {len(similarity_data)} 条相似度记录")
        
        # 2. 解析相似度数据
        similarities = []
        for row in similarity_data:
            prop1, prop2, sim_json, created_at = row
            sim_data = json.loads(sim_json)
            similarities.append({
                'property_id1': prop1,
                'property_id2': prop2,
                'similarity_score': sim_data.get('similarity_score', 0),
                'cosine_similarity': sim_data.get('cosine_similarity', 0),
                'euclidean_similarity': sim_data.get('euclidean_similarity', 0),
                'weighted_similarity': sim_data.get('weighted_similarity', 0),
                'same_cluster': sim_data.get('same_cluster', False),
                'algorithm': sim_data.get('algorithm', 'unknown')
            })
        
        df_sim = pd.DataFrame(similarities)
        
        # 3. 基本统计信息
        print("\n[2] 相似度分数统计...")
        print(f"  平均相似度: {df_sim['similarity_score'].mean():.4f}")
        print(f"  中位数相似度: {df_sim['similarity_score'].median():.4f}")
        print(f"  标准差: {df_sim['similarity_score'].std():.4f}")
        print(f"  最小值: {df_sim['similarity_score'].min():.4f}")
        print(f"  最大值: {df_sim['similarity_score'].max():.4f}")
        print(f"  相似度 > 0.5 的比例: {(df_sim['similarity_score'] > 0.5).sum() / len(df_sim) * 100:.2f}%")
        print(f"  相似度 > 0.7 的比例: {(df_sim['similarity_score'] > 0.7).sum() / len(df_sim) * 100:.2f}%")
        
        # 4. 验证同一小区的房源是否更相似
        print("\n[3] 验证同一小区的房源相似度...")
        sql = """
            SELECT 
                ps.property_id1,
                ps.property_id2,
                p1.community_id as community1,
                p2.community_id as community2,
                ps.similarity_data
            FROM property_similarity ps
            JOIN properties p1 ON ps.property_id1 = p1.property_id
            JOIN properties p2 ON ps.property_id2 = p2.property_id
        """
        self.cursor.execute(sql)
        community_data = self.cursor.fetchall()
        
        same_community_sims = []
        diff_community_sims = []
        
        for row in community_data:
            prop1, prop2, comm1, comm2, sim_json = row
            sim_data = json.loads(sim_json)
            score = sim_data.get('similarity_score', 0)
            
            if comm1 == comm2:
                same_community_sims.append(score)
            else:
                diff_community_sims.append(score)
        
        if same_community_sims and diff_community_sims:
            print(f"  同一小区的平均相似度: {np.mean(same_community_sims):.4f} (共 {len(same_community_sims)} 对)")
            print(f"  不同小区的平均相似度: {np.mean(diff_community_sims):.4f} (共 {len(diff_community_sims)} 对)")
            diff = np.mean(same_community_sims) - np.mean(diff_community_sims)
            if diff > 0.1:
                print(f"  ✓ 验证通过：同一小区的房源确实更相似 (差异: {diff:.4f})")
            else:
                print(f"  ⚠ 警告：同一小区和不同小区的相似度差异较小 (差异: {diff:.4f})")
        
        # 5. 验证价格相近的房源是否更相似
        print("\n[4] 验证价格相近的房源相似度...")
        sql = """
            SELECT 
                ps.property_id1,
                ps.property_id2,
                CAST(JSON_EXTRACT(p1.price_info, '$.total_price') AS DECIMAL(12,2)) as price1,
                CAST(JSON_EXTRACT(p2.price_info, '$.total_price') AS DECIMAL(12,2)) as price2,
                ps.similarity_data
            FROM property_similarity ps
            JOIN properties p1 ON ps.property_id1 = p1.property_id
            JOIN properties p2 ON ps.property_id2 = p2.property_id
            WHERE JSON_EXTRACT(p1.price_info, '$.total_price') IS NOT NULL
              AND JSON_EXTRACT(p2.price_info, '$.total_price') IS NOT NULL
        """
        self.cursor.execute(sql)
        price_data = self.cursor.fetchall()
        
        price_diffs = []
        similarities = []
        
        for row in price_data:
            prop1, prop2, price1, price2, sim_json = row
            if price1 and price2:
                price_diff = abs(float(price1) - float(price2))
                sim_data = json.loads(sim_json)
                score = sim_data.get('similarity_score', 0)
                price_diffs.append(price_diff)
                similarities.append(score)
        
        if price_diffs and similarities:
            # 计算价格差异与相似度的相关性
            correlation = np.corrcoef(price_diffs, similarities)[0, 1]
            print(f"  价格差异与相似度的相关系数: {correlation:.4f}")
            if correlation < -0.2:
                print(f"  ✓ 验证通过：价格越接近，相似度越高 (负相关)")
            else:
                print(f"  ⚠ 警告：价格差异与相似度相关性较弱")
        
        # 6. 验证户型相似的房源是否更相似
        print("\n[5] 验证户型相似的房源相似度...")
        sql = """
            SELECT 
                ps.property_id1,
                ps.property_id2,
                CAST(JSON_EXTRACT(p1.layout_info, '$.bedroom_count') AS UNSIGNED) as bedroom1,
                CAST(JSON_EXTRACT(p2.layout_info, '$.bedroom_count') AS UNSIGNED) as bedroom2,
                CAST(JSON_EXTRACT(p1.layout_info, '$.area') AS DECIMAL(10,2)) as area1,
                CAST(JSON_EXTRACT(p2.layout_info, '$.area') AS DECIMAL(10,2)) as area2,
                ps.similarity_data
            FROM property_similarity ps
            JOIN properties p1 ON ps.property_id1 = p1.property_id
            JOIN properties p2 ON ps.property_id2 = p2.property_id
        """
        self.cursor.execute(sql)
        layout_data = self.cursor.fetchall()
        
        same_bedroom_sims = []
        diff_bedroom_sims = []
        
        for row in layout_data:
            prop1, prop2, bed1, bed2, area1, area2, sim_json = row
            sim_data = json.loads(sim_json)
            score = sim_data.get('similarity_score', 0)
            
            if bed1 == bed2:
                same_bedroom_sims.append(score)
            else:
                diff_bedroom_sims.append(score)
        
        if same_bedroom_sims and diff_bedroom_sims:
            print(f"  相同卧室数的平均相似度: {np.mean(same_bedroom_sims):.4f} (共 {len(same_bedroom_sims)} 对)")
            print(f"  不同卧室数的平均相似度: {np.mean(diff_bedroom_sims):.4f} (共 {len(diff_bedroom_sims)} 对)")
            diff = np.mean(same_bedroom_sims) - np.mean(diff_bedroom_sims)
            if diff > 0.05:
                print(f"  ✓ 验证通过：相同户型的房源更相似 (差异: {diff:.4f})")
            else:
                print(f"  ⚠ 警告：户型差异对相似度影响较小 (差异: {diff:.4f})")
        
        # 7. 可视化分析
        print("\n[6] 生成可视化分析图表...")
        self._plot_property_similarity_analysis(df_sim, same_community_sims, diff_community_sims)
        
        print("\n" + "=" * 80)
        print("房源相似性验证完成")
        print("=" * 80)
    
    def validate_user_similarity(self):
        """验证用户相似性"""
        print("\n" + "=" * 80)
        print("用户相似性验证")
        print("=" * 80)
        
        # 1. 获取相似度数据
        print("\n[1] 获取用户相似度数据...")
        sql = """
            SELECT 
                user_id1,
                user_id2,
                similarity_data,
                created_at
            FROM user_similarity
            ORDER BY user_id1, user_id2
        """
        self.cursor.execute(sql)
        similarity_data = self.cursor.fetchall()
        
        if not similarity_data:
            print("❌ 未找到用户相似度数据，请先运行相似度计算算法")
            return
        
        print(f"✓ 找到 {len(similarity_data)} 条相似度记录")
        
        # 2. 解析相似度数据
        similarities = []
        for row in similarity_data:
            user1, user2, sim_json, created_at = row
            sim_data = json.loads(sim_json)
            similarities.append({
                'user_id1': user1,
                'user_id2': user2,
                'similarity_score': sim_data.get('similarity_score', 0),
                'cosine_similarity': sim_data.get('cosine_similarity', 0),
                'euclidean_similarity': sim_data.get('euclidean_similarity', 0),
                'behavior_similarity': sim_data.get('behavior_similarity', 0),
                'direct_similarity': sim_data.get('direct_similarity', 0),
                'propagated_similarity': sim_data.get('propagated_similarity', 0),
                'same_cluster': sim_data.get('same_cluster', False),
                'algorithm': sim_data.get('algorithm', 'unknown')
            })
        
        df_sim = pd.DataFrame(similarities)
        
        # 3. 基本统计信息
        print("\n[2] 相似度分数统计...")
        print(f"  平均相似度: {df_sim['similarity_score'].mean():.4f}")
        print(f"  中位数相似度: {df_sim['similarity_score'].median():.4f}")
        print(f"  标准差: {df_sim['similarity_score'].std():.4f}")
        print(f"  最小值: {df_sim['similarity_score'].min():.4f}")
        print(f"  最大值: {df_sim['similarity_score'].max():.4f}")
        print(f"  相似度 > 0.5 的比例: {(df_sim['similarity_score'] > 0.5).sum() / len(df_sim) * 100:.2f}%")
        print(f"  相似度 > 0.7 的比例: {(df_sim['similarity_score'] > 0.7).sum() / len(df_sim) * 100:.2f}%")
        
        # 4. 验证有相似行为的用户是否更相似
        print("\n[3] 验证有相似行为的用户相似度...")
        sql = """
            SELECT DISTINCT
                u1.user_id as user1,
                u2.user_id as user2,
                COUNT(DISTINCT bh1.property_id) as common_properties
            FROM users u1
            JOIN users u2 ON u1.user_id < u2.user_id
            JOIN browsing_history bh1 ON u1.user_id = bh1.user_id
            JOIN browsing_history bh2 ON u2.user_id = bh2.user_id AND bh1.property_id = bh2.property_id
            GROUP BY u1.user_id, u2.user_id
            HAVING common_properties > 0
        """
        self.cursor.execute(sql)
        common_behavior = self.cursor.fetchall()
        
        common_behavior_dict = {}
        for row in common_behavior:
            user1, user2, count = row
            key = f"{min(user1, user2)},{max(user1, user2)}"
            common_behavior_dict[key] = count
        
        common_behavior_sims = []
        no_common_behavior_sims = []
        
        for _, row in df_sim.iterrows():
            user1, user2 = row['user_id1'], row['user_id2']
            key = f"{min(user1, user2)},{max(user1, user2)}"
            score = row['similarity_score']
            
            if key in common_behavior_dict:
                common_behavior_sims.append(score)
            else:
                no_common_behavior_sims.append(score)
        
        if common_behavior_sims and no_common_behavior_sims:
            print(f"  有共同浏览的平均相似度: {np.mean(common_behavior_sims):.4f} (共 {len(common_behavior_sims)} 对)")
            print(f"  无共同浏览的平均相似度: {np.mean(no_common_behavior_sims):.4f} (共 {len(no_common_behavior_sims)} 对)")
            diff = np.mean(common_behavior_sims) - np.mean(no_common_behavior_sims)
            if diff > 0.1:
                print(f"  ✓ 验证通过：有共同行为的用户更相似 (差异: {diff:.4f})")
            else:
                print(f"  ⚠ 警告：共同行为对相似度影响较小 (差异: {diff:.4f})")
        
        # 5. 验证偏好相似的用户是否更相似
        print("\n[4] 验证偏好相似的用户相似度...")
        sql = """
            SELECT 
                user_id,
                preference_data
            FROM user_preferences
        """
        self.cursor.execute(sql)
        preferences_data = self.cursor.fetchall()
        
        user_preferences = {}
        for row in preferences_data:
            user_id, pref_json = row
            if pref_json:
                user_preferences[user_id] = json.loads(pref_json)
        
        # 计算偏好相似度
        preference_similarities = []
        for _, row in df_sim.iterrows():
            user1, user2 = row['user_id1'], row['user_id2']
            score = row['similarity_score']
            
            if user1 in user_preferences and user2 in user_preferences:
                pref1 = user_preferences[user1]
                pref2 = user_preferences[user2]
                
                # 比较位置偏好
                locations1 = set(pref1.get('locations', []))
                locations2 = set(pref2.get('locations', []))
                location_overlap = len(locations1 & locations2) / max(len(locations1 | locations2), 1)
                
                # 比较价格范围
                price_range1 = pref1.get('price_range', {})
                price_range2 = pref2.get('price_range', {})
                price_overlap = self._calculate_range_overlap(
                    price_range1.get('min', 0), price_range1.get('max', 1000),
                    price_range2.get('min', 0), price_range2.get('max', 1000)
                )
                
                preference_similarities.append({
                    'similarity_score': score,
                    'location_overlap': location_overlap,
                    'price_overlap': price_overlap,
                    'combined_overlap': (location_overlap + price_overlap) / 2
                })
        
        if preference_similarities:
            df_pref = pd.DataFrame(preference_similarities)
            correlation = df_pref['combined_overlap'].corr(df_pref['similarity_score'])
            print(f"  偏好重叠度与相似度的相关系数: {correlation:.4f}")
            if correlation > 0.3:
                print(f"  ✓ 验证通过：偏好越相似，用户相似度越高 (正相关)")
            else:
                print(f"  ⚠ 警告：偏好相似度与用户相似度相关性较弱")
        
        # 6. 检查聚类结果
        print("\n[5] 检查用户聚类结果...")
        sql = """
            SELECT 
                user_id,
                JSON_EXTRACT(user_profile, '$.cluster_id') as cluster_id
            FROM users
            WHERE JSON_EXTRACT(user_profile, '$.cluster_id') IS NOT NULL
        """
        self.cursor.execute(sql)
        cluster_data = self.cursor.fetchall()
        
        if cluster_data:
            clusters = {}
            for user_id, cluster_id in cluster_data:
                if cluster_id:
                    cluster_id = int(cluster_id)
                    if cluster_id not in clusters:
                        clusters[cluster_id] = []
                    clusters[cluster_id].append(user_id)
            
            print(f"  找到 {len(clusters)} 个聚类")
            for cluster_id, users in clusters.items():
                print(f"    聚类 {cluster_id}: {len(users)} 个用户")
            
            # 检查同一聚类内的用户平均相似度
            same_cluster_sims = []
            diff_cluster_sims = []
            
            for _, row in df_sim.iterrows():
                user1, user2 = row['user_id1'], row['user_id2']
                score = row['similarity_score']
                
                cluster1 = None
                cluster2 = None
                for cid, users in clusters.items():
                    if user1 in users:
                        cluster1 = cid
                    if user2 in users:
                        cluster2 = cid
                
                if cluster1 and cluster2:
                    if cluster1 == cluster2:
                        same_cluster_sims.append(score)
                    else:
                        diff_cluster_sims.append(score)
            
            if same_cluster_sims and diff_cluster_sims:
                print(f"  同一聚类内的平均相似度: {np.mean(same_cluster_sims):.4f}")
                print(f"  不同聚类间的平均相似度: {np.mean(diff_cluster_sims):.4f}")
                diff = np.mean(same_cluster_sims) - np.mean(diff_cluster_sims)
                if diff > 0.1:
                    print(f"  ✓ 验证通过：聚类结果合理 (差异: {diff:.4f})")
                else:
                    print(f"  ⚠ 警告：聚类效果不明显 (差异: {diff:.4f})")
        else:
            print("  ⚠ 未找到聚类数据")
        
        # 7. 可视化分析
        print("\n[6] 生成可视化分析图表...")
        self._plot_user_similarity_analysis(df_sim, common_behavior_sims, no_common_behavior_sims)
        
        print("\n" + "=" * 80)
        print("用户相似性验证完成")
        print("=" * 80)
    
    def _calculate_range_overlap(self, min1, max1, min2, max2):
        """计算两个区间的重叠度"""
        overlap_min = max(min1, min2)
        overlap_max = min(max1, max2)
        
        if overlap_min > overlap_max:
            return 0.0
        
        overlap_size = overlap_max - overlap_min
        range1_size = max1 - min1
        range2_size = max2 - min2
        union_size = max(max1, max2) - min(min1, min2)
        
        return overlap_size / union_size if union_size > 0 else 0.0
    
    def _plot_property_similarity_analysis(self, df_sim, same_community_sims, diff_community_sims):
        """绘制房源相似性分析图表"""
        fig, axes = plt.subplots(2, 2, figsize=(15, 12))
        fig.suptitle('房源相似性分析', fontsize=16, fontweight='bold')
        
        # 1. 相似度分布直方图
        axes[0, 0].hist(df_sim['similarity_score'], bins=50, edgecolor='black', alpha=0.7)
        axes[0, 0].set_title('相似度分数分布')
        axes[0, 0].set_xlabel('相似度分数')
        axes[0, 0].set_ylabel('频数')
        axes[0, 0].axvline(df_sim['similarity_score'].mean(), color='r', linestyle='--', label='平均值')
        axes[0, 0].legend()
        
        # 2. 同一小区 vs 不同小区的相似度对比
        if same_community_sims and diff_community_sims:
            axes[0, 1].boxplot([same_community_sims, diff_community_sims], 
                              labels=['同一小区', '不同小区'])
            axes[0, 1].set_title('同一小区 vs 不同小区的相似度对比')
            axes[0, 1].set_ylabel('相似度分数')
        
        # 3. 不同算法类型的相似度对比
        if 'algorithm' in df_sim.columns:
            algorithm_sims = df_sim.groupby('algorithm')['similarity_score'].apply(list)
            if len(algorithm_sims) > 0:
                axes[1, 0].boxplot(algorithm_sims.values, labels=algorithm_sims.index)
                axes[1, 0].set_title('不同算法的相似度分布')
                axes[1, 0].set_ylabel('相似度分数')
                axes[1, 0].tick_params(axis='x', rotation=45)
        
        # 4. 相似度分数箱线图
        axes[1, 1].boxplot([df_sim['similarity_score']], labels=['总体'])
        axes[1, 1].set_title('相似度分数箱线图')
        axes[1, 1].set_ylabel('相似度分数')
        
        plt.tight_layout()
        plt.savefig('property_similarity_analysis.png', dpi=300, bbox_inches='tight')
        print("  ✓ 图表已保存: property_similarity_analysis.png")
        plt.close()
    
    def _plot_user_similarity_analysis(self, df_sim, common_behavior_sims, no_common_behavior_sims):
        """绘制用户相似性分析图表"""
        fig, axes = plt.subplots(2, 2, figsize=(15, 12))
        fig.suptitle('用户相似性分析', fontsize=16, fontweight='bold')
        
        # 1. 相似度分布直方图
        axes[0, 0].hist(df_sim['similarity_score'], bins=50, edgecolor='black', alpha=0.7)
        axes[0, 0].set_title('相似度分数分布')
        axes[0, 0].set_xlabel('相似度分数')
        axes[0, 0].set_ylabel('频数')
        axes[0, 0].axvline(df_sim['similarity_score'].mean(), color='r', linestyle='--', label='平均值')
        axes[0, 0].legend()
        
        # 2. 有共同行为 vs 无共同行为的相似度对比
        if common_behavior_sims and no_common_behavior_sims:
            axes[0, 1].boxplot([common_behavior_sims, no_common_behavior_sims],
                              labels=['有共同行为', '无共同行为'])
            axes[0, 1].set_title('行为相似度对比')
            axes[0, 1].set_ylabel('相似度分数')
        
        # 3. 不同算法类型的相似度对比
        if 'algorithm' in df_sim.columns:
            algorithm_sims = df_sim.groupby('algorithm')['similarity_score'].apply(list)
            if len(algorithm_sims) > 0:
                axes[1, 0].boxplot(algorithm_sims.values, labels=algorithm_sims.index)
                axes[1, 0].set_title('不同算法的相似度分布')
                axes[1, 0].set_ylabel('相似度分数')
                axes[1, 0].tick_params(axis='x', rotation=45)
        
        # 4. 相似度分数箱线图
        axes[1, 1].boxplot([df_sim['similarity_score']], labels=['总体'])
        axes[1, 1].set_title('相似度分数箱线图')
        axes[1, 1].set_ylabel('相似度分数')
        
        plt.tight_layout()
        plt.savefig('user_similarity_analysis.png', dpi=300, bbox_inches='tight')
        print("  ✓ 图表已保存: user_similarity_analysis.png")
        plt.close()


def main():
    """主函数"""
    print("=" * 80)
    print("相似性算法验证工具")
    print("=" * 80)
    print()
    
    validator = SimilarityValidator()
    
    try:
        # 验证房源相似性
        validator.validate_property_similarity()
        
        # 验证用户相似性
        validator.validate_user_similarity()
        
        print("\n" + "=" * 80)
        print("所有验证完成！")
        print("=" * 80)
        
    except Exception as e:
        print(f"\n❌ 验证过程中出现错误: {str(e)}")
        import traceback
        traceback.print_exc()
    
    finally:
        validator.close()


if __name__ == '__main__':
    main()

