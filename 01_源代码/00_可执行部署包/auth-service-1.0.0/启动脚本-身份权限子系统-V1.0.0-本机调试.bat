@echo off
REM ============================================================
REM  auth-service-1.0.0 local debug startup (profile = local)
REM  Uses config\application-local.yml -> MySQL 127.0.0.1:3306/auth_db
REM  Contract 6.3: only the -local override file may contain 127.0.0.1
REM  NOTE: ASCII only, see [startup] script header for reason.
REM ============================================================
setlocal
cd /d "%~dp0"

if not "%JAVA_HOME%"=="" goto jdk_ready
if exist "E:\tools\jdk11\bin\java.exe" set "JAVA_HOME=E:\tools\jdk11"
:jdk_ready
if not "%JAVA_HOME%"=="" set "PATH=%JAVA_HOME%\bin;%PATH%"

echo ============================================================
echo  JDK     : %JAVA_HOME%
java -version
echo ------------------------------------------------------------
echo  Service : auth-service-1.0.0  ^|  port 8081  ^|  LOCAL DEBUG
echo  Config  : .\config\ + profile=local  (MySQL 127.0.0.1)
echo ============================================================

java -jar auth-service-1.0.0.jar --server.port=8081 --spring.config.additional-location=./config/ --spring.profiles.active=local

echo.
echo Service stopped. Press any key to close.
pause >nul
