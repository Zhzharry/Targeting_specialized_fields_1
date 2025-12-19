#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
房价信息注入脚本
从CSV文件读取房价数据并注入到MySQL数据库
"""

import pandas as pd
import pymysql
import json
import os
from datetime import datetime
from typing import Dict, Optional, Tuple
import sys

# 数据库配置
DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'bigdata_user',
    'password': '123456',
    'database': 'bigdata',
    'charset': 'utf8mb4'
}

# CSV文件列表
CSV_FILES = [
    '上海合并数据_清洗.csv',
    '北京合并数据_清洗.csv',
    '天津合并数据_清洗.csv'
]


def get_connection():
    """获取数据库连接"""
    return pymysql.connect(**DB_CONFIG)


def safe_float(value, default=0.0):
    """安全转换为浮点数"""
    if pd.isna(value) or value == '' or value is None:
        return default
    try:
        return float(value)
    except (ValueError, TypeError):
        return default


def safe_int(value, default=0):
    """安全转换为整数"""
    if pd.isna(value) or value == '' or value is None:
        return default
    try:
        return int(float(value))
    except (ValueError, TypeError):
        return default


def safe_str(value, default=''):
    """安全转换为字符串"""
    if pd.isna(value) or value is None:
        return default
    return str(value).strip()


def get_or_create_community(cursor, name: str, city: str, area: str, address: str,
                            longitude: float, latitude: float,
                            build_year: int, total_size: float, total_households: float,
                            plot_ratio: float, green_ratio: float, management_fee: float) -> int:
    """
    获取或创建小区，返回community_id
    """
    # 先查询是否存在同名小区（在同一城市）
    select_sql = """
        SELECT community_id FROM communities 
        WHERE name = %s 
        AND JSON_UNQUOTE(JSON_EXTRACT(location_info, '$.city')) = %s
        LIMIT 1
    """
    cursor.execute(select_sql, (name, city))
    result = cursor.fetchone()
    
    if result:
        return result[0]
    
    # 创建新小区
    basic_info = {
        "build_year": build_year,
        "total_size": total_size,
        "total_households": total_households,
        "management_company": ""
    }
    
    location_info = {
        "city": city,
        "district": area,
        "address": address,
        "province": get_province_by_city(city),
        "latitude": latitude,
        "longitude": longitude
    }
    
    facility_info = {
        "plot_ratio": plot_ratio,
        "green_ratio": green_ratio / 100.0 if green_ratio > 1 else green_ratio,  # 如果是百分比形式，转换为小数
        "management_fee": management_fee,
        "parking_spaces": 0,
        "schools": []
    }
    
    insert_sql = """
        INSERT INTO communities (name, basic_info, location_info, facility_info)
        VALUES (%s, %s, %s, %s)
    """
    cursor.execute(insert_sql, (
        name,
        json.dumps(basic_info, ensure_ascii=False),
        json.dumps(location_info, ensure_ascii=False),
        json.dumps(facility_info, ensure_ascii=False)
    ))
    
    return cursor.lastrowid


def get_province_by_city(city: str) -> str:
    """根据城市返回省份"""
    city_province_map = {
        '上海': '上海市',
        '北京': '北京市',
        '天津': '天津市',
        '深圳': '广东省',
        '广州': '广东省',
        '杭州': '浙江省',
        '南京': '江苏省',
        '苏州': '江苏省',
        '成都': '四川省',
        '武汉': '湖北省'
    }
    return city_province_map.get(city, city + '省')


def create_property(cursor, community_id: int, title: str, area: float,
                   bedroom_count: int, living_room_count: int, bathroom_count: int,
                   floor: int, total_floors: int, orientation: str,
                   build_year: int, decoration: str, property_type: str,
                   unit_price: float, total_price: float) -> int:
    """
    创建房源，返回property_id
    """
    basic_info = {
        "build_year": build_year,
        "decoration": decoration,
        "property_type": property_type
    }
    
    price_info = {
        "unit_price": unit_price,
        "total_price": total_price,
        "price_history": []
    }
    
    layout_info = {
        "area": area,
        "floor": floor,
        "orientation": orientation,
        "total_floors": total_floors,
        "bedroom_count": bedroom_count,
        "bathroom_count": bathroom_count,
        "living_room_count": living_room_count
    }
    
    insert_sql = """
        INSERT INTO properties (community_id, title, basic_info, price_info, layout_info, status)
        VALUES (%s, %s, %s, %s, %s, 'for_sale')
    """
    cursor.execute(insert_sql, (
        community_id,
        title,
        json.dumps(basic_info, ensure_ascii=False),
        json.dumps(price_info, ensure_ascii=False),
        json.dumps(layout_info, ensure_ascii=False)
    ))
    
    return cursor.lastrowid


def create_transaction_record(cursor, property_id: int, community_id: int,
                             transaction_date: str, total_price: float, unit_price: float,
                             area: float, layout: str, floor_info: str, orientation: str,
                             city: str, district: str, community_name: str):
    """
    创建成交记录
    """
    transaction_data = {
        "transaction_date": transaction_date,
        "total_price": total_price,
        "unit_price": unit_price,
        "area": area,
        "layout": layout,
        "floor_info": floor_info,
        "orientation": orientation,
        "city": city,
        "district": district,
        "community_name": community_name,
        "source_url": ""
    }
    
    insert_sql = """
        INSERT INTO transaction_records (property_id, community_id, transaction_data)
        VALUES (%s, %s, %s)
    """
    cursor.execute(insert_sql, (
        property_id,
        community_id,
        json.dumps(transaction_data, ensure_ascii=False)
    ))


def map_orientation(orientation_score: float) -> str:
    """将朝向评分映射为朝向字符串"""
    if orientation_score >= 4:
        return "south"
    elif orientation_score >= 3:
        return "southeast"
    elif orientation_score >= 2:
        return "east"
    else:
        return "north"


def map_decoration(build_year: int, current_year: int = 2025) -> str:
    """根据建筑年份推断装修情况"""
    age = current_year - build_year
    if age <= 5:
        return "luxury"
    elif age <= 10:
        return "hard"
    else:
        return "simple"


def process_csv_file(csv_file_path: str, connection):
    """
    处理单个CSV文件
    """
    print(f"\n开始处理文件: {csv_file_path}")
    
    if not os.path.exists(csv_file_path):
        print(f"警告: 文件不存在 {csv_file_path}")
        return
    
    # 读取CSV文件
    try:
        df = pd.read_csv(csv_file_path, encoding='utf-8')
    except UnicodeDecodeError:
        try:
            df = pd.read_csv(csv_file_path, encoding='gbk')
        except:
            df = pd.read_csv(csv_file_path, encoding='gb18030')
    
    print(f"读取到 {len(df)} 条记录")
    
    cursor = connection.cursor()
    
    success_count = 0
    error_count = 0
    
    # 用于缓存小区ID，避免重复查询
    community_cache = {}
    
    for idx, row in df.iterrows():
        try:
            # 读取CSV字段（尝试多种可能的列名）
            name = safe_str(row.get('name', row.get('小区名称', '')))
            city = safe_str(row.get('城市', row.get('city', '')))
            area = safe_str(row.get('area', row.get('区域', row.get('district', ''))))
            address = safe_str(row.get('address', row.get('地址', row.get('address', ''))))
            
            if not name or not city:
                error_count += 1
                if error_count <= 5:  # 只打印前5个错误
                    print(f"跳过第 {idx + 1} 行: 缺少必要字段 (name={name}, city={city})")
                continue
            
            # 位置信息
            longitude = safe_float(row.get('经度', 0))
            latitude = safe_float(row.get('纬度', 0))
            
            # 小区信息
            build_year = safe_int(row.get('建筑年份_数值', 0))
            total_size = safe_float(row.get('小区面积_数值', 0))
            total_households = safe_int(row.get('小区户数_数值', 0))
            plot_ratio = safe_float(row.get('容积率_数值', 0))
            green_ratio = safe_float(row.get('绿化率_数值', 0))
            management_fee = safe_float(row.get('物业费_数值', 0))
            
            # 房源信息
            area_size = safe_float(row.get('面积（m²）', 0))
            bedroom_count = safe_int(row.get('室数', 0))
            living_room_count = safe_int(row.get('厅数', 0))
            bathroom_count = safe_int(row.get('卫数', 0))
            total_floors = safe_int(row.get('总楼层数', 0))
            current_floor = safe_int(row.get('当前楼层估算', 0))
            if current_floor == 0:
                current_floor = safe_int(row.get('总楼层数', 0)) // 2  # 如果没有楼层信息，默认中层
            
            orientation_score = safe_float(row.get('朝向评分', 3))
            orientation = map_orientation(orientation_score)
            
            # 价格信息
            unit_price = safe_float(row.get('成交单价（元）', 0))
            total_price = safe_float(row.get('成交金额（万元）', 0))  # 单位是万元，需要转换为元
            
            # 成交信息
            transaction_year = safe_int(row.get('成交年份', 2024))
            transaction_month = safe_int(row.get('成交月份', 1))
            transaction_date = f"{transaction_year}-{transaction_month:02d}-01"
            
            # 生成房源标题
            title = f"{name} {bedroom_count}室{living_room_count}厅{bathroom_count}卫 {area_size}㎡"
            
            # 获取或创建小区
            cache_key = f"{name}_{city}"
            if cache_key not in community_cache:
                community_id = get_or_create_community(
                    cursor, name, city, area, address,
                    longitude, latitude,
                    build_year, total_size, total_households,
                    plot_ratio, green_ratio, management_fee
                )
                community_cache[cache_key] = community_id
            else:
                community_id = community_cache[cache_key]
            
            # 创建房源
            decoration = map_decoration(build_year)
            property_id = create_property(
                cursor, community_id, title, area_size,
                bedroom_count, living_room_count, bathroom_count,
                current_floor, total_floors, orientation,
                build_year, decoration, "apartment",
                unit_price, total_price * 10000  # 转换为元
            )
            
            # 创建成交记录
            layout = f"{bedroom_count}室{living_room_count}厅{bathroom_count}卫"
            floor_info = f"{current_floor}/{total_floors}层" if total_floors > 0 else "未知"
            orientation_cn = {"south": "南", "southeast": "东南", "east": "东", "north": "北"}.get(orientation, "未知")
            
            create_transaction_record(
                cursor, property_id, community_id,
                transaction_date, total_price * 10000, unit_price,
                area_size, layout, floor_info, orientation_cn,
                city, area, name
            )
            
            success_count += 1
            
            # 每100条提交一次
            if success_count % 100 == 0:
                connection.commit()
                print(f"已处理 {success_count} 条记录...")
        
        except Exception as e:
            error_count += 1
            print(f"处理第 {idx + 1} 行时出错: {str(e)}")
            if error_count > 10:  # 如果错误太多，停止处理
                print("错误过多，停止处理")
                break
    
    # 最终提交
    connection.commit()
    cursor.close()
    
    print(f"文件 {csv_file_path} 处理完成:")
    print(f"  成功: {success_count} 条")
    print(f"  失败: {error_count} 条")


def main():
    """主函数"""
    print("=" * 60)
    print("房价信息注入脚本")
    print("=" * 60)
    
    # 获取脚本所在目录
    script_dir = os.path.dirname(os.path.abspath(__file__))
    
    # 连接数据库
    try:
        connection = get_connection()
        print("数据库连接成功")
    except Exception as e:
        print(f"数据库连接失败: {str(e)}")
        sys.exit(1)
    
    try:
        # 检查文件是否存在
        missing_files = []
        for csv_file in CSV_FILES:
            csv_path = os.path.join(script_dir, csv_file)
            if not os.path.exists(csv_path):
                missing_files.append(csv_file)
        
        if missing_files:
            print(f"警告: 以下文件不存在:")
            for f in missing_files:
                print(f"  - {f}")
            print("\n继续处理存在的文件...\n")
        
        # 处理每个CSV文件
        for csv_file in CSV_FILES:
            csv_path = os.path.join(script_dir, csv_file)
            if os.path.exists(csv_path):
                process_csv_file(csv_path, connection)
        
        print("\n" + "=" * 60)
        print("所有文件处理完成！")
        print("=" * 60)
    
    except Exception as e:
        print(f"处理过程中出错: {str(e)}")
        import traceback
        traceback.print_exc()
    
    finally:
        connection.close()
        print("数据库连接已关闭")


if __name__ == '__main__':
    main()

