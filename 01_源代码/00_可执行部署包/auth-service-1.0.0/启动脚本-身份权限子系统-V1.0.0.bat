@echo off
REM ============================================================
REM  auth-service-1.0.0 startup script (Node A / deployment)
REM  Contract 7.2.1: external config via --spring.config.additional-location
REM  Usage: double click, or run in cmd
REM  NOTE: keep this file ASCII-only. cmd.exe reads .bat in ANSI(GBK)
REM        on Chinese Windows; UTF-8 Chinese text will break parsing.
REM ============================================================
setlocal
cd /d "%~dp0"

REM ---- pick JDK: JAVA_HOME first, then local JDK 11, then PATH ----
if not "%JAVA_HOME%"=="" goto jdk_ready
if exist "E:\tools\jdk11\bin\java.exe" set "JAVA_HOME=E:\tools\jdk11"
:jdk_ready
if not "%JAVA_HOME%"=="" set "PATH=%JAVA_HOME%\bin;%PATH%"

echo ============================================================
echo  JDK : %JAVA_HOME%
java -version
echo ------------------------------------------------------------
echo  Service : auth-service-1.0.0  ^|  port 8081  ^|  node A
echo  Config  : .\config\  (external, additional-location)
echo ============================================================

java -jar auth-service-1.0.0.jar --server.port=8081 --spring.config.additional-location=./config/

echo.
echo Service stopped. Press any key to close.
pause >nul
