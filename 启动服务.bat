@echo off
echo ========================================
echo 热力图项目启动脚本
echo ========================================

echo 正在启动后端服务...
cd backend
start "Backend Server" mvnw.cmd spring-boot:run

timeout /t 10 /nobreak > nul

echo 正在启动前端服务...
cd ../frontend
start "Frontend Dev Server" npm run dev

echo.
echo 服务启动完成！
echo 后端: http://localhost:8080
echo 前端: http://localhost:5173
echo.
echo 按任意键关闭服务...
pause > nul

echo 正在关闭服务...
taskkill /f /im java.exe > nul 2>&1
taskkill /f /im node.exe > nul 2>&1

echo 服务已关闭。