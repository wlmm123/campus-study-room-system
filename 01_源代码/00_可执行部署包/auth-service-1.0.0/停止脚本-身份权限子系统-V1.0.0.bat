@echo off
REM ============================================================
REM  auth-service-1.0.0  stop the instance listening on port 8081
REM  Use when the port is occupied by a hidden javaw.exe instance
REM  (for example: someone double-clicked the jar directly)
REM ============================================================
setlocal
cd /d "%~dp0"

for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8081" ^| findstr "LISTENING"') do (
  echo Killing PID %%a on port 8081 ...
  taskkill /F /PID %%a >nul 2>&1
  goto :done
)

echo Port 8081 is free, nothing to stop.
goto :end

:done
echo Done. Port 8081 released.

:end
echo.
pause >nul
