@echo off
chcp 65001 >nul
echo ============================================
echo 数据库数据一键注入脚本 (Windows)
echo ============================================
echo.

REM 设置数据库连接参数
set DB_HOST=localhost
set DB_PORT=3306
set DB_USER=bigdata_user
set DB_PASSWORD=123456
set DB_NAME=bigdata

REM 获取脚本所在目录
set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%"

echo 当前目录: %SCRIPT_DIR%
echo.

REM 检查MySQL命令是否存在
where mysql >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到mysql命令，请确保MySQL已安装并添加到PATH环境变量
    pause
    exit /b 1
)

echo 开始执行SQL脚本...
echo.

REM SQL文件列表（按执行顺序）
set SQL_FILES=01_users.sql 02_communities.sql 03_properties.sql 04_browsing_history.sql 05_favorites.sql 06_search_history.sql 07_user_preferences.sql 08_user_recommendations.sql 09_user_similarity.sql 10_property_similarity.sql 11_property_features.sql

set SUCCESS_COUNT=0
set FAIL_COUNT=0

for %%f in (%SQL_FILES%) do (
    if exist "%%f" (
        echo [执行] %%f
        mysql -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASSWORD% %DB_NAME% < "%%f" 2>nul
        if !errorlevel! equ 0 (
            echo [成功] %%f 执行完成
            set /a SUCCESS_COUNT+=1
        ) else (
            echo [失败] %%f 执行失败
            set /a FAIL_COUNT+=1
        )
        echo.
    ) else (
        echo [警告] 文件不存在: %%f
        echo.
    )
)

echo ============================================
echo 执行完成！
echo 成功: %SUCCESS_COUNT% 个文件
echo 失败: %FAIL_COUNT% 个文件
echo ============================================
echo.

pause

