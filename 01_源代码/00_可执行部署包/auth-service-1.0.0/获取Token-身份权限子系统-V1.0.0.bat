@echo off
REM ============================================================
REM  auth-service-1.0.0  get JWT tokens (student + admin)
REM  Requires: service already running on 127.0.0.1:8081
REM  Output  : data.token for 20230001 (STUDENT) and admin (ADMIN)
REM  Note    : token TTL is 7200s per REST contract 4.1.2
REM            rerun this script any time you need a fresh token
REM ============================================================
setlocal
cd /d "%~dp0"
set "BASE=http://127.0.0.1:8081"
set "TMPD=%TEMP%\auth_token_job"
if not exist "%TMPD%" mkdir "%TMPD%"

echo [1/4] probing %BASE% ...
curl -s -o nul --max-time 5 "%BASE%/api/auth/me"
if errorlevel 1 (
  echo    ERROR: service is not reachable. Start the service first.
  pause >nul
  exit /b 1
)

echo [2/4] login student 20230001 ...
curl -s -X POST "%BASE%/api/auth/login" -H "Content-Type: application/json" -d "{\"username\":\"20230001\",\"password\":\"Password123!\"}" -o "%TMPD%\student.json"

echo [3/4] login admin ...
curl -s -X POST "%BASE%/api/auth/login" -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"Password123!\"}" -o "%TMPD%\admin.json"

echo [4/4] parsing ...
for /f "usebackq delims=" %%t in (`powershell -NoProfile -Command "$d=ConvertFrom-Json -InputObject (Get-Content -Raw '%TMPD%\student.json'); $d.data.token"`) do set "STU=%%t"
for /f "usebackq delims=" %%t in (`powershell -NoProfile -Command "$d=ConvertFrom-Json -InputObject (Get-Content -Raw '%TMPD%\admin.json'); $d.data.token"`) do set "ADM=%%t"

echo.
echo ============================================================
echo  STUDENT  (20230001 / Password123!)  TTL 7200s
echo ============================================================
echo %STU%
echo.
echo ============================================================
echo  ADMIN    (admin / Password123!)     TTL 7200s
echo ============================================================
echo %ADM%
echo.
echo Header for Postman:  Authorization: Bearer ^<token^>
echo Raw JSON saved in: %TMPD%
echo.
pause >nul
