#!/bin/bash

echo "============================================"
echo "数据库数据一键注入脚本 (Linux/Mac)"
echo "============================================"
echo ""

# 设置数据库连接参数
DB_HOST="localhost"
DB_PORT="3306"
DB_USER="bigdata_user"
DB_PASSWORD="123456"
DB_NAME="bigdata"

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "当前目录: $SCRIPT_DIR"
echo ""

# 检查MySQL命令是否存在
if ! command -v mysql &> /dev/null; then
    echo "[错误] 未找到mysql命令，请确保MySQL已安装并添加到PATH环境变量"
    exit 1
fi

echo "开始执行SQL脚本..."
echo ""

# SQL文件列表（按执行顺序）
SQL_FILES=(
    "01_users.sql"
    "02_communities.sql"
    "03_properties.sql"
    "04_browsing_history.sql"
    "05_favorites.sql"
    "06_search_history.sql"
    "07_user_preferences.sql"
    "08_user_recommendations.sql"
    "09_user_similarity.sql"
    "10_property_similarity.sql"
    "11_property_features.sql"
)

SUCCESS_COUNT=0
FAIL_COUNT=0

for sql_file in "${SQL_FILES[@]}"; do
    if [ -f "$sql_file" ]; then
        echo "[执行] $sql_file"
        mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < "$sql_file" 2>/dev/null
        if [ $? -eq 0 ]; then
            echo "[成功] $sql_file 执行完成"
            ((SUCCESS_COUNT++))
        else
            echo "[失败] $sql_file 执行失败"
            ((FAIL_COUNT++))
        fi
        echo ""
    else
        echo "[警告] 文件不存在: $sql_file"
        echo ""
    fi
done

echo "============================================"
echo "执行完成！"
echo "成功: $SUCCESS_COUNT 个文件"
echo "失败: $FAIL_COUNT 个文件"
echo "============================================"
echo ""

