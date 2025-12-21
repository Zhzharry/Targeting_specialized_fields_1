#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
房源相似度测试数据生成脚本

功能：
1. 插入极端相似的房源数据（用于测试高相似度计算）
2. 插入极端不相似的房源数据（用于测试低相似度计算）
3. 为这些房源创建对应的小区（communities）数据

注意：本脚本只插入数据，不做任何相似度计算

使用方法：
    python generate_extreme_property_data.py

数据库配置：
    - 主机: localhost
    - 端口: 3306
    - 数据库: bigdata
    - 用户名: root
    - 密码: 123456
"""

import pymysql
import json
import random
from datetime import datetime, timedelta
from typing import List, Dict, Tuple

# 数据库配置
DB_CONFIG = {
    'host': '127.0.0.1',
    'port': 3306,
    'user': 'root',
    'password': '123456',
    'database': 'bigdata',
    'charset': 'utf8mb4',
    'autocommit': False,
    'connect_timeout': 10
}

# 房源ID范围
SIMILAR_PROPERTIES_START_ID = 10000
DISSIMILAR_PROPERTIES_START_ID = 20000
COMMUNITIES_START_ID = 1000

# 深圳地区
SHENZHEN_DISTRICTS = [
    "南山区", "福田区", "罗湖区", "宝安区", 
    "龙岗区", "龙华区", "盐田区", "坪山区", "光明区", "大鹏新区"
]

# 朝向
ORIENTATIONS = ["south", "southeast", "southwest", "east", "west", "north", "northeast", "northwest"]

# 装修类型
DECORATIONS = ["精装", "简装", "毛坯", "豪华装修"]

# 价格范围（万元）
PRICE_RANGES = [
    (200, 400),   # 低价格
    (300, 500),   # 中低价格
    (400, 600),   # 中价格
    (500, 700),   # 中高价格
    (600, 800),   # 高价格
    (800, 1000),  # 超高价格
]

# 面积范围（平方米）
AREA_RANGES = [
    (60, 90),     # 小户型
    (90, 120),    # 中户型
    (120, 150),   # 大户型
    (150, 200),   # 超大户型
]


def get_connection():
    """获取数据库连接"""
    try:
        connection = pymysql.connect(**DB_CONFIG)
        return connection
    except pymysql.Error as e:
        print(f"\n✗ Database connection failed!")
        print(f"  Error: {e}")
        print(f"\nPlease check:")
        print(f"  1. MySQL service is running")
        print(f"  2. Database credentials are correct")
        print(f"  3. User '{DB_CONFIG['user']}' has access to '{DB_CONFIG['database']}' database")
        raise


def create_similar_communities(count: int = 3) -> List[Dict]:
    """
    创建极端相似的小区（用于相似房源）
    这些小区具有：
    - 相同的位置（同一区域）
    - 相同的设施配置
    - 相同的地理坐标范围
    """
    print(f"\n{'='*60}")
    print(f"Creating {count} EXTREMELY SIMILAR communities")
    print(f"{'='*60}")
    
    communities = []
    base_district = "南山区"
    base_longitude = 113.9344  # 深圳南山中心坐标
    base_latitude = 22.5329
    
    for i in range(count):
        community_id = COMMUNITIES_START_ID + i
        community_name = f"相似小区_{community_id}"
        
        # 所有小区使用相同的配置（极端相似）
        location_info = {
            "district": base_district,
            "city": "深圳市",
            "province": "广东省",
            "longitude": base_longitude + random.uniform(-0.01, 0.01),  # 非常接近
            "latitude": base_latitude + random.uniform(-0.01, 0.01),
            "address": f"{base_district}科技园{community_id}号"
        }
        
        facility_info = {
            "management_fee": 3.5,  # 相同的物业费
            "green_ratio": 0.35,    # 相同的绿化率
            "plot_ratio": 2.5,      # 相同的容积率
            "parking_spaces": 500,   # 相同的停车位
            "has_gym": True,
            "has_swimming_pool": True,
            "has_kindergarten": True,
            "has_supermarket": True
        }
        
        communities.append({
            'community_id': community_id,
            'name': community_name,
            'location_info': json.dumps(location_info, ensure_ascii=False),
            'facility_info': json.dumps(facility_info, ensure_ascii=False),
            'created_at': datetime.now() - timedelta(days=random.randint(1, 365))
        })
        
        print(f"  Community {community_id}: {community_name}")
        print(f"    District: {base_district}")
        print(f"    Location: {location_info['address']}")
    
    return communities


def create_dissimilar_communities(count: int = 5) -> List[Dict]:
    """
    创建极端不相似的小区（用于不相似房源）
    这些小区具有：
    - 完全不同的位置（不同区域）
    - 不同的设施配置
    - 不同的地理坐标
    """
    print(f"\n{'='*60}")
    print(f"Creating {count} EXTREMELY DISSIMILAR communities")
    print(f"{'='*60}")
    
    communities = []
    
    # 不同区域的坐标
    district_coords = {
        "南山区": (113.9344, 22.5329),
        "龙岗区": (114.2514, 22.7208),
        "宝安区": (113.8831, 22.5533),
        "盐田区": (114.2354, 22.5556),
        "光明区": (113.9359, 22.7489),
    }
    
    dissimilar_configs = [
        {
            'district': '南山区',
            'coords': district_coords['南山区'],
            'facility': {
                "management_fee": 5.0,
                "green_ratio": 0.40,
                "plot_ratio": 2.0,
                "parking_spaces": 800,
                "has_gym": True,
                "has_swimming_pool": True,
                "has_kindergarten": True,
                "has_supermarket": True
            }
        },
        {
            'district': '龙岗区',
            'coords': district_coords['龙岗区'],
            'facility': {
                "management_fee": 2.0,
                "green_ratio": 0.25,
                "plot_ratio": 3.5,
                "parking_spaces": 200,
                "has_gym": False,
                "has_swimming_pool": False,
                "has_kindergarten": False,
                "has_supermarket": True
            }
        },
        {
            'district': '宝安区',
            'coords': district_coords['宝安区'],
            'facility': {
                "management_fee": 2.5,
                "green_ratio": 0.30,
                "plot_ratio": 3.0,
                "parking_spaces": 300,
                "has_gym": False,
                "has_swimming_pool": False,
                "has_kindergarten": True,
                "has_supermarket": True
            }
        },
        {
            'district': '盐田区',
            'coords': district_coords['盐田区'],
            'facility': {
                "management_fee": 1.8,
                "green_ratio": 0.20,
                "plot_ratio": 4.0,
                "parking_spaces": 150,
                "has_gym": False,
                "has_swimming_pool": False,
                "has_kindergarten": False,
                "has_supermarket": False
            }
        },
        {
            'district': '光明区',
            'coords': district_coords['光明区'],
            'facility': {
                "management_fee": 1.5,
                "green_ratio": 0.15,
                "plot_ratio": 4.5,
                "parking_spaces": 100,
                "has_gym": False,
                "has_swimming_pool": False,
                "has_kindergarten": False,
                "has_supermarket": False
            }
        }
    ]
    
    for i, config in enumerate(dissimilar_configs[:count]):
        community_id = COMMUNITIES_START_ID + 10 + i
        community_name = f"不相似小区_{community_id}"
        
        location_info = {
            "district": config['district'],
            "city": "深圳市",
            "province": "广东省",
            "longitude": config['coords'][0] + random.uniform(-0.05, 0.05),
            "latitude": config['coords'][1] + random.uniform(-0.05, 0.05),
            "address": f"{config['district']}某街道{community_id}号"
        }
        
        communities.append({
            'community_id': community_id,
            'name': community_name,
            'location_info': json.dumps(location_info, ensure_ascii=False),
            'facility_info': json.dumps(config['facility'], ensure_ascii=False),
            'created_at': datetime.now() - timedelta(days=random.randint(1, 365))
        })
        
        print(f"  Community {community_id}: {community_name}")
        print(f"    District: {config['district']}")
        print(f"    Location: {location_info['address']}")
    
    return communities


def create_similar_properties(community_ids: List[int], count_per_community: int = 5) -> List[Dict]:
    """
    为相似小区创建极端相似的房源
    这些房源具有：
    - 相同的价格范围
    - 相同的面积范围
    - 相同的户型
    - 相同的朝向
    - 相同的装修
    """
    print(f"\n{'='*60}")
    print(f"Creating EXTREMELY SIMILAR properties")
    print(f"{'='*60}")
    
    properties = []
    property_id = SIMILAR_PROPERTIES_START_ID
    
    # 固定的配置模板（所有相似房源使用相同配置）
    base_price = PRICE_RANGES[2]  # 400-600万
    base_area = AREA_RANGES[1]    # 90-120平米
    base_bedroom = 3
    base_living = 2
    base_bathroom = 2
    base_orientation = "south"
    base_decoration = "精装"
    base_build_year = 2018
    base_floor = 15
    base_total_floors = 28
    
    for community_id in community_ids:
        for i in range(count_per_community):
            # 所有房源使用相同的配置（极端相似）
            price_info = {
                "total_price": random.uniform(base_price[0], base_price[1]),
                "unit_price": random.uniform(40000, 50000)
            }
            
            layout_info = {
                "area": random.uniform(base_area[0], base_area[1]),
                "bedroom_count": base_bedroom,
                "living_room_count": base_living,
                "bathroom_count": base_bathroom,
                "floor": base_floor + i,  # 楼层略有不同
                "total_floors": base_total_floors,
                "orientation": base_orientation
            }
            
            basic_info = {
                "build_year": base_build_year,
                "decoration": base_decoration,
                "property_type": "住宅"
            }
            
            properties.append({
                'property_id': property_id,
                'community_id': community_id,
                'title': f'相似房源_{property_id}',
                'basic_info': json.dumps(basic_info, ensure_ascii=False),
                'price_info': json.dumps(price_info, ensure_ascii=False),
                'layout_info': json.dumps(layout_info, ensure_ascii=False),
                'status': 'for_sale',
                'view_count': 0,
                'favorite_count': 0,
                'created_at': datetime.now() - timedelta(days=random.randint(1, 180)),
                'updated_at': datetime.now()
            })
            
            print(f"  Property {property_id}: {price_info['total_price']:.0f}万, "
                  f"{layout_info['area']:.0f}㎡, {base_bedroom}室{base_living}厅{base_bathroom}卫")
            property_id += 1
    
    return properties


def create_dissimilar_properties(community_ids: List[int], count_per_community: int = 3) -> List[Dict]:
    """
    为不相似小区创建极端不相似的房源
    这些房源具有：
    - 完全不同的价格范围
    - 完全不同的面积范围
    - 不同的户型
    - 不同的朝向
    - 不同的装修
    """
    print(f"\n{'='*60}")
    print(f"Creating EXTREMELY DISSIMILAR properties")
    print(f"{'='*60}")
    
    properties = []
    property_id = DISSIMILAR_PROPERTIES_START_ID
    
    # 完全不同的配置
    dissimilar_configs = [
        {
            'price': PRICE_RANGES[5],  # 800-1000万（超高）
            'area': AREA_RANGES[3],    # 150-200平米（超大）
            'bedroom': 5,
            'living': 3,
            'bathroom': 3,
            'orientation': 'south',
            'decoration': '豪华装修',
            'build_year': 2020,
            'floor': 25,
            'total_floors': 30
        },
        {
            'price': PRICE_RANGES[0],  # 200-400万（低）
            'area': AREA_RANGES[0],    # 60-90平米（小）
            'bedroom': 1,
            'living': 1,
            'bathroom': 1,
            'orientation': 'north',
            'decoration': '简装',
            'build_year': 2010,
            'floor': 5,
            'total_floors': 10
        },
        {
            'price': PRICE_RANGES[3],  # 500-700万
            'area': AREA_RANGES[2],    # 120-150平米
            'bedroom': 4,
            'living': 2,
            'bathroom': 2,
            'orientation': 'east',
            'decoration': '精装',
            'build_year': 2015,
            'floor': 10,
            'total_floors': 20
        },
        {
            'price': PRICE_RANGES[1],  # 300-500万
            'area': AREA_RANGES[1],    # 90-120平米
            'bedroom': 2,
            'living': 1,
            'bathroom': 1,
            'orientation': 'west',
            'decoration': '毛坯',
            'build_year': 2005,
            'floor': 3,
            'total_floors': 6
        },
        {
            'price': PRICE_RANGES[4],  # 600-800万
            'area': AREA_RANGES[2],    # 120-150平米
            'bedroom': 3,
            'living': 2,
            'bathroom': 2,
            'orientation': 'southeast',
            'decoration': '精装',
            'build_year': 2012,
            'floor': 8,
            'total_floors': 15
        }
    ]
    
    for idx, community_id in enumerate(community_ids):
        config = dissimilar_configs[idx % len(dissimilar_configs)]
        
        for i in range(count_per_community):
            price_info = {
                "total_price": random.uniform(config['price'][0], config['price'][1]),
                "unit_price": random.uniform(30000, 60000)
            }
            
            layout_info = {
                "area": random.uniform(config['area'][0], config['area'][1]),
                "bedroom_count": config['bedroom'],
                "living_room_count": config['living'],
                "bathroom_count": config['bathroom'],
                "floor": config['floor'] + i,
                "total_floors": config['total_floors'],
                "orientation": config['orientation']
            }
            
            basic_info = {
                "build_year": config['build_year'],
                "decoration": config['decoration'],
                "property_type": "住宅"
            }
            
            properties.append({
                'property_id': property_id,
                'community_id': community_id,
                'title': f'不相似房源_{property_id}',
                'basic_info': json.dumps(basic_info, ensure_ascii=False),
                'price_info': json.dumps(price_info, ensure_ascii=False),
                'layout_info': json.dumps(layout_info, ensure_ascii=False),
                'status': 'for_sale',
                'view_count': 0,
                'favorite_count': 0,
                'created_at': datetime.now() - timedelta(days=random.randint(1, 180)),
                'updated_at': datetime.now()
            })
            
            print(f"  Property {property_id}: {price_info['total_price']:.0f}万, "
                  f"{layout_info['area']:.0f}㎡, {config['bedroom']}室{config['living']}厅{config['bathroom']}卫")
            property_id += 1
    
    return properties


def insert_communities(connection, communities: List[Dict]):
    """批量插入小区数据"""
    print(f"\nInserting {len(communities)} communities into database...")
    
    sql = """
        INSERT INTO communities (community_id, name, location_info, facility_info, created_at)
        VALUES (%s, %s, %s, %s, %s)
        ON DUPLICATE KEY UPDATE
            name = VALUES(name),
            location_info = VALUES(location_info),
            facility_info = VALUES(facility_info)
    """
    
    with connection.cursor() as cursor:
        for community in communities:
            try:
                cursor.execute(sql, (
                    community['community_id'],
                    community['name'],
                    community['location_info'],
                    community['facility_info'],
                    community['created_at']
                ))
                print(f"  ✓ Inserted/Updated community {community['community_id']}: {community['name']}")
            except Exception as e:
                print(f"  ✗ Failed to insert community {community['community_id']}: {e}")
    
    connection.commit()
    print(f"✓ Successfully inserted/updated {len(communities)} communities\n")


def delete_all_properties(connection):
    """删除properties表中的所有数据（包括相关依赖数据）"""
    print(f"\nDeleting all existing data from properties table...")
    try:
        with connection.cursor() as cursor:
            # 先删除可能存在的依赖数据（避免外键约束错误）
            # 删除收藏数据
            cursor.execute("DELETE FROM favorites")
            favorites_deleted = cursor.rowcount
            print(f"  ✓ Deleted {favorites_deleted} favorites records")
            
            # 删除浏览历史
            cursor.execute("DELETE FROM browsing_history")
            history_deleted = cursor.rowcount
            print(f"  ✓ Deleted {history_deleted} browsing_history records")
            
            # 删除房源相似度数据
            cursor.execute("DELETE FROM property_similarity")
            similarity_deleted = cursor.rowcount
            print(f"  ✓ Deleted {similarity_deleted} property_similarity records")
            
            # 删除房源数据
            cursor.execute("DELETE FROM properties")
            properties_deleted = cursor.rowcount
            print(f"  ✓ Deleted {properties_deleted} properties records")
        
        connection.commit()
        print(f"✓ Successfully deleted all existing properties and related data\n")
        return True
    except Exception as e:
        print(f"  ✗ Error: Failed to delete existing data: {e}")
        print(f"  Continuing with insert anyway...\n")
        connection.rollback()
        return False


def insert_properties(connection, properties: List[Dict]):
    """批量插入房源数据"""
    
    print(f"\nInserting {len(properties)} properties into database...")
    
    sql = """
        INSERT INTO properties (
            property_id, community_id, title, basic_info, price_info, 
            layout_info, status, view_count, favorite_count, created_at, updated_at
        )
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    """
    
    inserted_count = 0
    with connection.cursor() as cursor:
        for property_data in properties:
            try:
                cursor.execute(sql, (
                    property_data['property_id'],
                    property_data['community_id'],
                    property_data['title'],
                    property_data['basic_info'],
                    property_data['price_info'],
                    property_data['layout_info'],
                    property_data['status'],
                    property_data['view_count'],
                    property_data['favorite_count'],
                    property_data['created_at'],
                    property_data['updated_at']
                ))
                inserted_count += 1
                print(f"  ✓ Inserted property {property_data['property_id']}")
            except Exception as e:
                print(f"  ✗ Failed to insert property {property_data['property_id']}: {e}")
    
    connection.commit()
    print(f"✓ Successfully inserted {inserted_count} properties\n")


def verify_data(connection):
    """验证插入的数据"""
    print(f"\n{'='*60}")
    print("Verifying inserted data")
    print(f"{'='*60}")
    
    with connection.cursor() as cursor:
        # 检查相似小区
        cursor.execute("""
            SELECT community_id, name 
            FROM communities 
            WHERE community_id >= %s AND community_id < %s
            ORDER BY community_id
        """, (COMMUNITIES_START_ID, COMMUNITIES_START_ID + 10))
        similar_communities = cursor.fetchall()
        print(f"\nSimilar communities (IDs {COMMUNITIES_START_ID}-{COMMUNITIES_START_ID+2}): {len(similar_communities)}")
        
        # 检查不相似小区
        cursor.execute("""
            SELECT community_id, name 
            FROM communities 
            WHERE community_id >= %s AND community_id < %s
            ORDER BY community_id
        """, (COMMUNITIES_START_ID + 10, COMMUNITIES_START_ID + 20))
        dissimilar_communities = cursor.fetchall()
        print(f"Dissimilar communities (IDs {COMMUNITIES_START_ID+10}+): {len(dissimilar_communities)}")
        
        # 检查相似房源
        cursor.execute("""
            SELECT COUNT(*) as count
            FROM properties 
            WHERE property_id >= %s AND property_id < %s
        """, (SIMILAR_PROPERTIES_START_ID, DISSIMILAR_PROPERTIES_START_ID))
        similar_properties = cursor.fetchone()
        print(f"\nSimilar properties (IDs {SIMILAR_PROPERTIES_START_ID}-{DISSIMILAR_PROPERTIES_START_ID-1}): {similar_properties[0] if similar_properties else 0}")
        
        # 检查不相似房源
        cursor.execute("""
            SELECT COUNT(*) as count
            FROM properties 
            WHERE property_id >= %s AND property_id < %s
        """, (DISSIMILAR_PROPERTIES_START_ID, DISSIMILAR_PROPERTIES_START_ID + 100))
        dissimilar_properties = cursor.fetchone()
        print(f"Dissimilar properties (IDs {DISSIMILAR_PROPERTIES_START_ID}+): {dissimilar_properties[0] if dissimilar_properties else 0}")


def main():
    """主函数 - 仅为测试插入极端的properties和communities数据"""
    print("="*60)
    print("Property Similarity Test Data Insertion Script")
    print("="*60)
    print("\nThis script will:")
    print("  1. Insert EXTREMELY SIMILAR communities and properties")
    print("  2. Insert EXTREMELY DISSIMILAR communities and properties")
    print("\nDatabase:", DB_CONFIG['database'])
    print("="*60)
    
    try:
        connection = get_connection()
        print("✓ Database connection established\n")
        
        # 1. 创建相似小区
        similar_communities = create_similar_communities(count=3)
        insert_communities(connection, similar_communities)
        similar_community_ids = [c['community_id'] for c in similar_communities]
        
        # 2. 创建不相似小区
        dissimilar_communities = create_dissimilar_communities(count=5)
        insert_communities(connection, dissimilar_communities)
        dissimilar_community_ids = [c['community_id'] for c in dissimilar_communities]
        
        # 3. 删除所有现有的properties数据（包括相关依赖数据）
        delete_all_properties(connection)
        
        # 4. 为相似小区创建相似房源
        similar_properties = create_similar_properties(similar_community_ids, count_per_community=5)
        insert_properties(connection, similar_properties)
        
        # 5. 为不相似小区创建不相似房源
        dissimilar_properties = create_dissimilar_properties(dissimilar_community_ids, count_per_community=3)
        insert_properties(connection, dissimilar_properties)
        
        # 5. 验证数据
        verify_data(connection)
        
        print("\n" + "="*60)
        print("✓ Data insertion completed successfully!")
        print("="*60)
        print("\nSummary:")
        print(f"  - Inserted {len(similar_communities)} similar communities")
        print(f"  - Inserted {len(dissimilar_communities)} dissimilar communities")
        print(f"  - Inserted {len(similar_properties)} similar properties")
        print(f"  - Inserted {len(dissimilar_properties)} dissimilar properties")
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

