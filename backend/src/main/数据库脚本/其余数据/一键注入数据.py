#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
数据库数据一键注入脚本 (跨平台)
支持Windows、Linux、Mac
"""

import os
import sys
import subprocess
from pathlib import Path

# 数据库配置
DB_CONFIG = {
    'host': 'localhost',
    'port': '3306',
    'user': 'bigdata_user',
    'password': '123456',
    'database': 'bigdata'
}

# SQL文件列表（按执行顺序）
SQL_FILES = [
    '01_users.sql',
    '02_communities.sql',
    '03_properties.sql',
    '04_browsing_history.sql',
    '05_favorites.sql',
    '06_search_history.sql',
    '07_user_preferences.sql',
    '08_user_recommendations.sql',
    '09_user_similarity.sql',
    '10_property_similarity.sql',
    '11_property_features.sql'
]


def check_mysql_command():
    """检查MySQL命令是否可用"""
    try:
        subprocess.run(['mysql', '--version'], 
                      capture_output=True, 
                      check=True)
        return True
    except (subprocess.CalledProcessError, FileNotFoundError):
        return False


def execute_sql_file(sql_file_path, db_config):
    """执行SQL文件"""
    try:
        # 构建mysql命令
        cmd = [
            'mysql',
            f"-h{db_config['host']}",
            f"-P{db_config['port']}",
            f"-u{db_config['user']}",
            f"-p{db_config['password']}",
            db_config['database']
        ]
        
        # 读取SQL文件内容
        with open(sql_file_path, 'r', encoding='utf-8') as f:
            sql_content = f.read()
        
        # 执行SQL
        result = subprocess.run(
            cmd,
            input=sql_content,
            text=True,
            capture_output=True,
            encoding='utf-8'
        )
        
        if result.returncode == 0:
            return True, None
        else:
            return False, result.stderr
    except Exception as e:
        return False, str(e)


def main():
    """主函数"""
    print("=" * 60)
    print("数据库数据一键注入脚本 (跨平台)")
    print("=" * 60)
    print()
    
    # 获取脚本所在目录
    script_dir = Path(__file__).parent.absolute()
    os.chdir(script_dir)
    
    print(f"当前目录: {script_dir}")
    print()
    
    # 检查MySQL命令
    if not check_mysql_command():
        print("[错误] 未找到mysql命令，请确保MySQL已安装并添加到PATH环境变量")
        print("       或者使用 pymysql 方式连接数据库")
        sys.exit(1)
    
    print("开始执行SQL脚本...")
    print()
    
    success_count = 0
    fail_count = 0
    failed_files = []
    
    for sql_file in SQL_FILES:
        sql_path = script_dir / sql_file
        
        if sql_path.exists():
            print(f"[执行] {sql_file}")
            success, error = execute_sql_file(sql_path, DB_CONFIG)
            
            if success:
                print(f"[成功] {sql_file} 执行完成")
                success_count += 1
            else:
                print(f"[失败] {sql_file} 执行失败")
                if error:
                    print(f"        错误信息: {error[:100]}...")
                fail_count += 1
                failed_files.append(sql_file)
            print()
        else:
            print(f"[警告] 文件不存在: {sql_file}")
            print()
    
    print("=" * 60)
    print("执行完成！")
    print(f"成功: {success_count} 个文件")
    print(f"失败: {fail_count} 个文件")
    if failed_files:
        print(f"失败的文件: {', '.join(failed_files)}")
    print("=" * 60)
    print()


if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n[中断] 用户中断执行")
        sys.exit(1)
    except Exception as e:
        print(f"\n[错误] 执行过程中出现异常: {str(e)}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

