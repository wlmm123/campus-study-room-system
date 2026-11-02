@echo off
chcp 65001 >nul
REM ============================================================
REM  身份权限子系统（8081）· 本地调试启动脚本
REM  用法：双击本文件，或在命令行执行  启动-本地.bat
REM  说明：用外置 config/ 目录覆盖配置，profile=local 走本机 MySQL
REM ============================================================

REM JDK 11 路径（按本机实际安装位置改）
set "JAVA_HOME=E:\tools\jdk11"
set "PATH=%JAVA_HOME%\bin;%PATH%"

cd /d "%~dp0"

echo ============================================
echo   JDK: %JAVA_HOME%
%JAVA_HOME%\bin\java -version
echo ============================================
echo   启动 auth-service 1.0.0  ^|  端口 8081
echo ============================================

java -jar target\auth-service-1.0.0.jar ^
  --server.port=8081 ^
  --spring.config.additional-location=./config/ ^
  --spring.profiles.active=local

pause
