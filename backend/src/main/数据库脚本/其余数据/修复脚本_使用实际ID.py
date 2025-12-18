#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
修复SQL脚本中的外键引用问题
自动查询实际存在的ID并生成正确的SQL语句
"""

import pymysql
import json

# 数据库配置
DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'bigdata_user',
    'password': '123456',
    'database': 'bigdata',
    'charset': 'utf8mb4'
}


def get_actual_ids(conn):
    """获取实际存在的用户ID和房源ID"""
    cursor = conn.cursor()
    
    # 获取所有用户ID
    cursor.execute("SELECT user_id, username FROM users ORDER BY user_id")
    users = {row[1]: row[0] for row in cursor.fetchall()}
    user_ids = list(users.values())
    
    # 获取所有房源ID
    cursor.execute("SELECT property_id FROM properties ORDER BY property_id")
    property_ids = [row[0] for row in cursor.fetchall()]
    
    cursor.close()
    return users, user_ids, property_ids


def generate_browsing_history_sql(users, user_ids, property_ids):
    """生成浏览历史SQL"""
    sql = "-- ============================================\n"
    sql += "-- 浏览历史表数据注入脚本（修复版）\n"
    sql += "-- ============================================\n\n"
    sql += "USE `bigdata`;\n\n"
    sql += "INSERT INTO `browsing_history` (`user_id`, `property_id`, `behavior_data`, `created_at`) VALUES\n"
    
    values = []
    # 为前几个用户生成浏览记录
    for i, user_id in enumerate(user_ids[:10]):
        # 每个用户浏览3-5个房源
        num_properties = min(5, len(property_ids))
        for j in range(num_properties):
            prop_id = property_ids[j % len(property_ids)]
            duration = 100 + (i * 20) + (j * 10)
            view_count = 2 + j
            behavior_data = json.dumps({
                "duration_seconds": duration,
                "view_count": view_count,
                "last_view_time": "2024-12-01 10:30:00"
            }, ensure_ascii=False)
            values.append(f"({user_id}, {prop_id}, '{behavior_data}', '2024-11-28 10:30:00')")
    
    sql += ",\n".join(values) + ";\n"
    return sql


def generate_favorites_sql(users, user_ids, property_ids):
    """生成收藏SQL"""
    sql = "-- ============================================\n"
    sql += "-- 收藏表数据注入脚本（修复版）\n"
    sql += "-- ============================================\n\n"
    sql += "USE `bigdata`;\n\n"
    sql += "INSERT INTO `favorites` (`user_id`, `property_id`, `favorite_data`, `created_at`) VALUES\n"
    
    values = []
    # 为前几个用户生成收藏记录
    for i, user_id in enumerate(user_ids[:10]):
        # 每个用户收藏2-3个房源
        num_favorites = min(3, len(property_ids))
        for j in range(num_favorites):
            prop_id = property_ids[j % len(property_ids)]
            notes = ["位置好，交通便利", "价格合适，考虑中", "精装修，可以直接入住"]
            tags_list = [["地铁", "学区"], ["性价比"], ["精装", "新房"]]
            favorite_data = json.dumps({
                "note": notes[j % len(notes)],
                "tags": tags_list[j % len(tags_list)]
            }, ensure_ascii=False)
            values.append(f"({user_id}, {prop_id}, '{favorite_data}', '2024-11-28 10:35:00')")
    
    sql += ",\n".join(values) + ";\n"
    return sql


def generate_search_history_sql(users, user_ids):
    """生成搜索历史SQL"""
    sql = "-- ============================================\n"
    sql += "-- 搜索历史表数据注入脚本（修复版）\n"
    sql += "-- ============================================\n\n"
    sql += "USE `bigdata`;\n\n"
    sql += "INSERT INTO `search_history` (`user_id`, `search_data`, `created_at`) VALUES\n"
    
    values = []
    keywords_list = [
        ["三房", "南山区"],
        ["四房", "学区房"],
        ["两房", "宝安区"],
        ["三房", "精装修"],
        ["四房", "大户型"]
    ]
    
    for i, user_id in enumerate(user_ids[:10]):
        keywords = keywords_list[i % len(keywords_list)]
        search_data = json.dumps({
            "keywords": keywords,
            "filters": {
                "price_min": 300 + i * 50,
                "price_max": 600 + i * 50,
                "area_min": 80,
                "area_max": 120
            }
        }, ensure_ascii=False)
        values.append(f"({user_id}, '{search_data}', '2024-11-28 10:00:00')")
    
    sql += ",\n".join(values) + ";\n"
    return sql


def generate_user_preferences_sql(users, user_ids):
    """生成用户偏好SQL"""
    sql = "-- ============================================\n"
    sql += "-- 用户偏好表数据注入脚本（修复版）\n"
    sql += "-- ============================================\n\n"
    sql += "USE `bigdata`;\n\n"
    sql += "INSERT INTO `user_preferences` (`user_id`, `preference_data`, `created_at`, `updated_at`) VALUES\n"
    
    values = []
    for i, user_id in enumerate(user_ids[3:13]):  # 从第4个用户开始（user_id=4）
        preference_data = json.dumps({
            "keywords": ["地铁", "精装"],
            "locations": ["南山区", "罗湖区"],
            "area_range": {"max": 100, "min": 70},
            "decorations": ["hard", "luxury"],
            "house_types": ["apartment"],
            "price_range": {"max": 700, "min": 400},
            "orientations": ["south", "southeast"],
            "bedroom_range": {"max": 3, "min": 2}
        }, ensure_ascii=False)
        values.append(f"({user_id}, '{preference_data}', NOW(), NOW())")
    
    sql += ",\n".join(values) + ";\n"
    return sql


def generate_user_recommendations_sql(users, user_ids, property_ids):
    """生成用户推荐SQL"""
    sql = "-- ============================================\n"
    sql += "-- 用户推荐表数据注入脚本（修复版）\n"
    sql += "-- ============================================\n\n"
    sql += "USE `bigdata`;\n\n"
    sql += "INSERT INTO `user_recommendations` (`user_id`, `property_id`, `recommendation_data`, `created_at`) VALUES\n"
    
    values = []
    for i, user_id in enumerate(user_ids[:10]):
        # 每个用户推荐2-3个房源
        num_recommendations = min(3, len(property_ids))
        for j in range(num_recommendations):
            prop_id = property_ids[j % len(property_ids)]
            score = 0.85 - (j * 0.05)
            recommendation_data = json.dumps({
                "type": "content_based",
                "score": round(score, 2),
                "reason": "基于您的浏览和偏好匹配",
                "is_viewed": False
            }, ensure_ascii=False)
            values.append(f"({user_id}, {prop_id}, '{recommendation_data}', '2024-12-01 10:00:00')")
    
    sql += ",\n".join(values) + ";\n"
    return sql


def generate_user_similarity_sql(users, user_ids):
    """生成用户相似度SQL"""
    sql = "-- ============================================\n"
    sql += "-- 用户相似度表数据注入脚本（修复版）\n"
    sql += "-- ============================================\n\n"
    sql += "USE `bigdata`;\n\n"
    sql += "INSERT INTO `user_similarity` (`user_id1`, `user_id2`, `similarity_data`, `created_at`, `updated_at`) VALUES\n"
    
    values = []
    # 生成用户对之间的相似度
    for i in range(min(10, len(user_ids))):
        for j in range(i + 1, min(10, len(user_ids))):
            user1 = user_ids[i]
            user2 = user_ids[j]
            similarity = 0.5 + (i + j) * 0.05
            similarity = min(0.95, similarity)
            similarity_data = json.dumps({
                "similarity_score": round(similarity, 2),
                "algorithm_version": "v1.0",
                "common_items": 3 + (i + j) % 5,
                "calculation_method": "cosine"
            }, ensure_ascii=False)
            values.append(f"({user1}, {user2}, '{similarity_data}', NOW(), NOW())")
    
    sql += ",\n".join(values) + ";\n"
    return sql


def main():
    """主函数"""
    print("=" * 60)
    print("SQL脚本修复工具")
    print("=" * 60)
    
    try:
        conn = pymysql.connect(**DB_CONFIG)
        print("✓ 数据库连接成功")
        
        # 获取实际ID
        print("\n查询实际存在的ID...")
        users, user_ids, property_ids = get_actual_ids(conn)
        print(f"  找到 {len(user_ids)} 个用户")
        print(f"  找到 {len(property_ids)} 个房源")
        
        if not user_ids or not property_ids:
            print("❌ 错误：数据库中没有足够的用户或房源数据")
            return
        
        # 生成修复后的SQL文件
        print("\n生成修复后的SQL文件...")
        
        files = {
            '04_browsing_history_fixed.sql': generate_browsing_history_sql(users, user_ids, property_ids),
            '05_favorites_fixed.sql': generate_favorites_sql(users, user_ids, property_ids),
            '06_search_history_fixed.sql': generate_search_history_sql(users, user_ids),
            '07_user_preferences_fixed.sql': generate_user_preferences_sql(users, user_ids),
            '08_user_recommendations_fixed.sql': generate_user_recommendations_sql(users, user_ids, property_ids),
            '09_user_similarity_fixed.sql': generate_user_similarity_sql(users, user_ids)
        }
        
        for filename, content in files.items():
            with open(filename, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"  ✓ 已生成: {filename}")
        
        print("\n" + "=" * 60)
        print("修复完成！")
        print("=" * 60)
        print("\n请使用生成的 *_fixed.sql 文件替换原来的SQL文件")
        
        conn.close()
        
    except Exception as e:
        print(f"\n❌ 错误: {str(e)}")
        import traceback
        traceback.print_exc()


if __name__ == '__main__':
    main()

