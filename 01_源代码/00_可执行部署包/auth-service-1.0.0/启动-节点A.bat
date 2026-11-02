@echo off
chcp 65001 >nul
rem ==========================================================
rem 身份权限子系统 8081 - 节点A 启动脚本
rem 必须使用 additional-location（追加），不要用 location（覆盖）
rem ==========================================================
cd /d "%~dp0"
java -jar auth-service-1.0.0.jar --spring.config.additional-location=./config/
pause
