#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
用户偏好数据插入脚本

功能：
1. 使用数据库中已存在的用户
2. 为这些用户插入极端相似的偏好数据（用于测试高相似度）
3. 为这些用户插入极端不相似的偏好数据（用于测试低相似度）
4. 为这些用户插入收藏数据

注意：本脚本只插入数据，不做任何相似度计算

使用方法：
    python generate_extreme_user_preferences.py

数据库配置：
    - 主机: localhost
    - 端口: 3306
    - 数据库: bigdata
    - 用户名: bigdata_user
    - 密码: 123456
"""

import pymysql
import json
import random
from datetime import datetime, timedelta
from typing import List, Dict, Tuple

# 数据库配置
# ============================================
# 如果遇到 "Access denied for user 'xxx'@'gateway'" 错误：
# ============================================
# 方法1: 修改MySQL用户权限（推荐）
#   在MySQL中执行：
#   GRANT ALL PRIVILEGES ON bigdata.* TO 'bigdata_user'@'%' IDENTIFIED BY '123456';
#   FLUSH PRIVILEGES;
#
# 方法2: 使用root用户（临时测试）
#   将下面的 'user' 改为 'root'，'password' 改为你的root密码
#
# 方法3: 如果使用Docker，确保端口映射正确
# ============================================
DB_CONFIG = {
    'host': '127.0.0.1',  # 或 'localhost'，如果使用Docker请改为容器IP
    'port': 3306,
    'user': 'root',  # 如果权限问题，可以临时改为 'root'
    'password': '123456',    # 请确保密码正确
    'database': 'bigdata',
    'charset': 'utf8mb4',
    'autocommit': False,
    'connect_timeout': 10
}

# 注意：本脚本使用数据库中已存在的用户，不创建新用户

# 深圳地区列表
SHENZHEN_DISTRICTS = [
    "南山区", "福田区", "罗湖区", "宝安区", 
    "龙岗区", "龙华区", "盐田区", "坪山区", "光明区", "大鹏新区"
]

# 家庭结构类型
FAMILY_STRUCTURES = ["single", "couple", "family", "extended_family"]

# 预算范围（万元）
BUDGET_RANGES = [
    (200, 400),   # 低预算
    (300, 500),   # 中低预算
    (400, 600),   # 中预算
    (500, 700),   # 中高预算
    (600, 800),   # 高预算
    (800, 1000),  # 超高预算
]


def get_connection():
    """获取数据库连接，尝试多种连接方式"""
    # 尝试连接的主机列表（按优先级）
    hosts_to_try = [
        DB_CONFIG['host'],  # 当前配置的主机
        '127.0.0.1',        # 如果当前是localhost，尝试127.0.0.1
        'localhost'         # 如果当前是127.0.0.1，尝试localhost
    ]
    
    # 去重
    hosts_to_try = list(dict.fromkeys(hosts_to_try))
    
    last_error = None
    for host in hosts_to_try:
        try:
            config = DB_CONFIG.copy()
            config['host'] = host
            print(f"  Trying to connect to {host}:{config['port']}...")
            connection = pymysql.connect(**config)
            print(f"  ✓ Connected successfully to {host}")
            return connection
        except pymysql.Error as e:
            last_error = e
            print(f"  ✗ Failed to connect to {host}: {e}")
            continue
    
    # 所有连接方式都失败
    print(f"\n✗ Database connection failed after trying all hosts!")
    print(f"  Last error: {last_error}")
    print(f"\nPlease check:")
    print(f"  1. MySQL service is running")
    print(f"  2. Database credentials are correct")
    print(f"  3. User '{DB_CONFIG['user']}' has access to '{DB_CONFIG['database']}' database")
    print(f"  4. MySQL user is allowed to connect from your host")
    print(f"\nTried hosts: {', '.join(hosts_to_try)}")
    print(f"Current config:")
    print(f"  Port: {DB_CONFIG['port']}")
    print(f"  User: {DB_CONFIG['user']}")
    print(f"  Database: {DB_CONFIG['database']}")
    print(f"\n" + "="*60)
    print("SOLUTION: Fix MySQL user permissions")
    print("="*60)
    print("If you see 'Access denied for user 'xxx'@'gateway'':")
    print("\nOption 1: Grant permissions to user (in MySQL):")
    print(f"  GRANT ALL PRIVILEGES ON {DB_CONFIG['database']}.* TO '{DB_CONFIG['user']}'@'%' IDENTIFIED BY '{DB_CONFIG['password']}';")
    print("  FLUSH PRIVILEGES;")
    print("\nOption 2: Use root user (temporary, for testing):")
    print("  Edit DB_CONFIG in the script:")
    print("    'user': 'root',")
    print(f"    'password': 'your_root_password',")
    print("="*60)
    raise last_error


def get_existing_users(connection, limit: int = 20) -> List[int]:
    """获取数据库中已存在的用户ID列表"""
    with connection.cursor() as cursor:
        cursor.execute("SELECT user_id FROM users ORDER BY user_id LIMIT %s", (limit,))
        results = cursor.fetchall()
        user_ids = [row[0] for row in results] if results else []
        print(f"Found {len(user_ids)} existing users: {user_ids}")
        return user_ids


def create_similar_user_profiles(user_ids: List[int]) -> List[Dict]:
    """
    为指定用户创建极端相似的偏好配置（用于测试高相似度）
    这些用户具有：
    - 相同的预算范围
    - 相同的家庭结构
    - 相同或高度重叠的偏好地区
    """
    print(f"\n{'='*60}")
    print(f"Creating EXTREMELY SIMILAR preferences for {len(user_ids)} users (for high similarity testing)")
    print(f"{'='*60}")
    
    # 选择一个固定的配置作为模板
    base_budget = BUDGET_RANGES[2]  # 400-600万
    base_family = "couple"
    base_locations = ["南山区", "福田区"]
    
    user_profiles = []
    for user_id in user_ids:
        # 所有用户使用相同的偏好配置（极端相似）
        user_profile = {
            "budget": {
                "min": base_budget[0],
                "max": base_budget[1]
            },
            "family_structure": base_family,
            "preferred_locations": base_locations.copy()  # 完全相同的地区偏好
        }
        
        user_profiles.append({
            'user_id': user_id,
            'user_profile': json.dumps(user_profile, ensure_ascii=False)
        })
        
        print(f"  User {user_id}")
        print(f"    Budget: {base_budget[0]}-{base_budget[1]}万")
        print(f"    Family: {base_family}")
        print(f"    Locations: {', '.join(base_locations)}")
    
    return user_profiles


def create_dissimilar_user_profiles(user_ids: List[int]) -> List[Dict]:
    """
    为指定用户创建极端不相似的偏好配置（用于测试低相似度）
    这些用户具有：
    - 完全不同的预算范围（一个很高，一个很低）
    - 不同的家庭结构
    - 完全不重叠的偏好地区
    """
    print(f"\n{'='*60}")
    print(f"Creating EXTREMELY DISSIMILAR preferences for {len(user_ids)} users (for low similarity testing)")
    print(f"{'='*60}")
    
    # 创建完全不同的配置
    dissimilar_configs = [
        # 高预算 vs 低预算
        {
            'budget': BUDGET_RANGES[5],  # 800-1000万
            'family': 'extended_family',
            'locations': ['南山区', '福田区']
        },
        {
            'budget': BUDGET_RANGES[0],  # 200-400万
            'family': 'single',
            'locations': ['龙岗区', '宝安区']
        },
        # 不同家庭结构
        {
            'budget': BUDGET_RANGES[3],  # 500-700万
            'family': 'family',
            'locations': ['罗湖区']
        },
        {
            'budget': BUDGET_RANGES[2],  # 400-600万
            'family': 'single',
            'locations': ['盐田区', '坪山区']
        },
        # 完全不重叠的地区
        {
            'budget': BUDGET_RANGES[4],  # 600-800万
            'family': 'couple',
            'locations': ['光明区', '大鹏新区']
        }
    ]
    
    user_profiles = []
    for i, user_id in enumerate(user_ids):
        # 循环使用配置，如果用户数多于配置数
        config = dissimilar_configs[i % len(dissimilar_configs)]
        
        user_profile = {
            "budget": {
                "min": config['budget'][0],
                "max": config['budget'][1]
            },
            "family_structure": config['family'],
            "preferred_locations": config['locations']
        }
        
        user_profiles.append({
            'user_id': user_id,
            'user_profile': json.dumps(user_profile, ensure_ascii=False)
        })
        
        print(f"  User {user_id}")
        print(f"    Budget: {config['budget'][0]}-{config['budget'][1]}万")
        print(f"    Family: {config['family']}")
        print(f"    Locations: {', '.join(config['locations'])}")
    
    return user_profiles


def get_available_property_ids(connection, limit: int = 100) -> List[int]:
    """获取可用的房源ID列表"""
    with connection.cursor() as cursor:
        cursor.execute("SELECT property_id FROM properties ORDER BY property_id LIMIT %s", (limit,))
        results = cursor.fetchall()
        return [row[0] for row in results] if results else []


def create_favorites_for_similar_users(connection, user_ids: List[int], property_ids: List[int]):
    """
    为相似用户创建收藏数据
    这些用户会收藏大量相同的房源（高相似度）
    """
    print(f"\n{'='*60}")
    print(f"Creating favorites for SIMILAR users (high overlap)")
    print(f"{'='*60}")
    
    # 选择一组共同的房源（所有相似用户都收藏这些）
    common_properties = property_ids[:15]  # 前15个房源作为共同收藏
    
    with connection.cursor() as cursor:
        for user_id in user_ids:
            # 每个用户收藏共同房源 + 少量个人偏好
            favorites = common_properties.copy()
            # 添加1-3个个人偏好的房源（增加一些变化）
            personal_favorites = random.sample(
                property_ids[15:], 
                min(random.randint(1, 3), len(property_ids) - 15)
            )
            favorites.extend(personal_favorites)
            
            # 插入收藏记录
            for property_id in favorites:
                try:
                    sql = """
                        INSERT INTO favorites (user_id, property_id, favorite_data, created_at)
                        VALUES (%s, %s, %s, %s)
                        ON DUPLICATE KEY UPDATE created_at = VALUES(created_at)
                    """
                    cursor.execute(sql, (
                        user_id,
                        property_id,
                        '{}',
                        datetime.now() - timedelta(days=random.randint(1, 30))
                    ))
                except Exception as e:
                    print(f"    Warning: Failed to insert favorite for user {user_id}, property {property_id}: {e}")
            
            print(f"  User {user_id}: {len(favorites)} favorites ({len(common_properties)} common + {len(favorites) - len(common_properties)} personal)")
    
    connection.commit()


def create_favorites_for_dissimilar_users(connection, user_ids: List[int], property_ids: List[int]):
    """
    为不相似用户创建收藏数据
    这些用户会收藏完全不同的房源（低相似度，甚至无重叠）
    """
    print(f"\n{'='*60}")
    print(f"Creating favorites for DISSIMILAR users (low/no overlap)")
    print(f"{'='*60}")
    
    # 将房源分成不重叠的组
    properties_per_user = len(property_ids) // len(user_ids)
    
    with connection.cursor() as cursor:
        for idx, user_id in enumerate(user_ids):
            # 每个用户收藏不同范围的房源，确保不重叠
            start_idx = idx * properties_per_user
            end_idx = start_idx + min(properties_per_user, 10)  # 每个用户收藏10个房源
            user_properties = property_ids[start_idx:end_idx]
            
            # 插入收藏记录
            for property_id in user_properties:
                try:
                    sql = """
                        INSERT INTO favorites (user_id, property_id, favorite_data, created_at)
                        VALUES (%s, %s, %s, %s)
                        ON DUPLICATE KEY UPDATE created_at = VALUES(created_at)
                    """
                    cursor.execute(sql, (
                        user_id,
                        property_id,
                        '{}',
                        datetime.now() - timedelta(days=random.randint(1, 30))
                    ))
                except Exception as e:
                    print(f"    Warning: Failed to insert favorite for user {user_id}, property {property_id}: {e}")
            
            print(f"  User {user_id}: {len(user_properties)} favorites (property_ids: {user_properties[:5]}...)")
    
    connection.commit()


def update_user_profiles(connection, user_profiles: List[Dict]):
    """批量更新用户偏好数据（只更新user_profile字段，不创建新用户）"""
    print(f"\nUpdating user profiles for {len(user_profiles)} users...")
    
    sql = """
        UPDATE users 
        SET user_profile = %s
        WHERE user_id = %s
    """
    
    updated_count = 0
    with connection.cursor() as cursor:
        for profile in user_profiles:
            try:
                cursor.execute(sql, (
                    profile['user_profile'],
                    profile['user_id']
                ))
                if cursor.rowcount > 0:
                    updated_count += 1
                    print(f"  ✓ Updated user {profile['user_id']} profile")
                else:
                    print(f"  ⚠ User {profile['user_id']} not found in database")
            except Exception as e:
                print(f"  ✗ Failed to update user {profile['user_id']}: {e}")
    
    connection.commit()
    print(f"✓ Successfully updated {updated_count} user profiles\n")


def verify_data(connection, similar_user_ids: List[int], dissimilar_user_ids: List[int]):
    """验证插入的数据"""
    print(f"\n{'='*60}")
    print("Verifying inserted data")
    print(f"{'='*60}")
    
    all_user_ids = similar_user_ids + dissimilar_user_ids
    
    with connection.cursor() as cursor:
        # 检查用户偏好
        if similar_user_ids:
            placeholders = ','.join(['%s'] * len(similar_user_ids))
            cursor.execute(f"""
                SELECT user_id, user_profile 
                FROM users 
                WHERE user_id IN ({placeholders})
                ORDER BY user_id
            """, similar_user_ids)
            similar_profiles = cursor.fetchall()
            print(f"\nSimilar users (IDs {similar_user_ids}): {len(similar_profiles)} profiles updated")
        
        if dissimilar_user_ids:
            placeholders = ','.join(['%s'] * len(dissimilar_user_ids))
            cursor.execute(f"""
                SELECT user_id, user_profile 
                FROM users 
                WHERE user_id IN ({placeholders})
                ORDER BY user_id
            """, dissimilar_user_ids)
            dissimilar_profiles = cursor.fetchall()
            print(f"Dissimilar users (IDs {dissimilar_user_ids}): {len(dissimilar_profiles)} profiles updated")
        
        # 检查收藏数据
        if all_user_ids:
            placeholders = ','.join(['%s'] * len(all_user_ids))
            cursor.execute(f"""
                SELECT user_id, COUNT(*) as favorite_count
                FROM favorites
                WHERE user_id IN ({placeholders})
                GROUP BY user_id
                ORDER BY user_id
            """, all_user_ids)
            favorites = cursor.fetchall()
            print(f"\nFavorites created: {len(favorites)} users have favorites")
            for fav in favorites:
                print(f"  User {fav[0]}: {fav[1]} favorites")


def main():
    """主函数 - 仅为现有用户插入偏好和收藏数据，不做任何计算"""
    print("="*60)
    print("User Preference Data Insertion Script")
    print("="*60)
    print("\nThis script will:")
    print("  1. Use EXISTING users from database")
    print("  2. Insert EXTREMELY SIMILAR preferences (for high similarity testing)")
    print("  3. Insert EXTREMELY DISSIMILAR preferences (for low similarity testing)")
    print("  4. Insert favorite data for these users")
    print("\nDatabase:", DB_CONFIG['database'])
    print("="*60)
    
    try:
        connection = get_connection()
        print("✓ Database connection established\n")
        
        # 1. 获取现有用户
        existing_users = get_existing_users(connection, limit=20)
        if len(existing_users) < 10:
            print(f"⚠ Warning: Only {len(existing_users)} users found. Need at least 10 users.")
            print("Please ensure there are enough users in the database.")
            return
        
        # 2. 将用户分成两组：相似用户和不相似用户
        mid_point = len(existing_users) // 2
        similar_user_ids = existing_users[:mid_point]  # 前一半作为相似用户
        dissimilar_user_ids = existing_users[mid_point:mid_point+5]  # 后5个作为不相似用户
        
        print(f"\nSelected users:")
        print(f"  Similar users (IDs): {similar_user_ids}")
        print(f"  Dissimilar users (IDs): {dissimilar_user_ids}")
        
        # 3. 为相似用户创建偏好配置
        similar_profiles = create_similar_user_profiles(similar_user_ids)
        update_user_profiles(connection, similar_profiles)
        
        # 4. 为不相似用户创建偏好配置
        dissimilar_profiles = create_dissimilar_user_profiles(dissimilar_user_ids)
        update_user_profiles(connection, dissimilar_profiles)
        
        # 5. 获取可用房源ID
        property_ids = get_available_property_ids(connection, limit=100)
        if not property_ids:
            print("⚠ Warning: No properties found in database. Skipping favorite creation.")
        else:
            print(f"\n✓ Found {len(property_ids)} available properties")
            
            # 6. 为相似用户创建收藏（高重叠）
            create_favorites_for_similar_users(connection, similar_user_ids, property_ids)
            
            # 7. 为不相似用户创建收藏（低重叠）
            create_favorites_for_dissimilar_users(connection, dissimilar_user_ids, property_ids)
        
        # 8. 验证插入的数据
        verify_data(connection, similar_user_ids, dissimilar_user_ids)
        
        print("\n" + "="*60)
        print("✓ Data insertion completed successfully!")
        print("="*60)
        print("\nSummary:")
        print(f"  - Updated preferences for {len(similar_user_ids)} similar users")
        print(f"  - Updated preferences for {len(dissimilar_user_ids)} dissimilar users")
        print(f"  - Inserted favorite data for all users")
        print("\nNote: This script only inserts data. Run similarity calculation separately.")
        print("="*60)
        
    except Exception as e:
        print(f"\n✗ Error: {e}")
        import traceback
        traceback.print_exc()
    finally:
        if 'connection' in locals():
            connection.close()
            print("\n✓ Database connection closed")


if __name__ == "__main__":
    main()

