@echo off
echo ====================================
echo 房价热力图功能测试脚本
echo ====================================
echo.

echo [步骤 1] 更新数据库...
echo 请手动在MySQL中执行: backend\热力图数据库更新.sql
echo.
pause

echo [步骤 2] 启动后端服务...
cd backend
start cmd /k "echo 正在启动Spring Boot... && mvn spring-boot:run"
cd ..
echo 后端服务启动中，请等待约30秒...
timeout /t 30 /nobreak
echo.

echo [步骤 3] 启动前端服务...
cd frontend
start cmd /k "echo 正在启动Vue开发服务器... && npm run dev"
cd ..
echo 前端服务启动中，请等待约10秒...
timeout /t 10 /nobreak
echo.

echo ====================================
echo 服务启动完成！
echo ====================================
echo.
echo 访问地址:
echo - 前端: http://localhost:5173
echo - 热力图: http://localhost:5173/heatmap
echo.
echo ⚠️ 重要提示:
echo 1. 请先在 frontend\src\views\HeatmapPage.vue 中配置高德地图Key
echo 2. 确保已执行数据库更新脚本
echo 3. 如果遇到问题，请查看 房价热力图使用说明.md
echo.
pause
