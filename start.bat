@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "FRONTEND_DIR=%~dp0kanban-frontend"
set "FRONTEND_URL=http://localhost:3000"
set "BACKEND_HEALTH_URL=http://localhost:8080/actuator/health"

title SmartPM Frontend Launcher
echo.
echo ======================================
echo   SmartPM Frontend Launcher
echo ======================================
echo.

if not exist "%FRONTEND_DIR%\package.json" (
  echo [ERROR] Frontend folder was not found:
  echo %FRONTEND_DIR%
  pause
  exit /b 1
)

where npm >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Node.js/npm was not found.
  echo Install Node.js LTS, then run this file again.
  pause
  exit /b 1
)

pushd "%FRONTEND_DIR%"
if not exist "node_modules" (
  echo [1/3] Installing frontend dependencies. This is only needed once...
  call npm install
  if errorlevel 1 (
    echo.
    echo [ERROR] Dependency installation failed. Check your network and try again.
    popd
    pause
    exit /b 1
  )
)

powershell -NoProfile -Command "try { $null = Invoke-WebRequest -UseBasicParsing -TimeoutSec 2 '%BACKEND_HEALTH_URL%'; exit 0 } catch { exit 1 }"
if errorlevel 1 (
  echo [WARNING] Backend is not responding on http://localhost:8080.
  echo           Start the Spring Boot backend in IntelliJ IDEA before using API features.
) else (
  echo [1/3] Backend is ready on port 8080.
)

powershell -NoProfile -Command "try { $null = Invoke-WebRequest -UseBasicParsing -TimeoutSec 1 '%FRONTEND_URL%'; exit 0 } catch { exit 1 }"
if not errorlevel 1 (
  echo [2/3] Frontend is already running.
  start "" "%FRONTEND_URL%"
  popd
  exit /b 0
)

echo [2/3] Starting Vue frontend...
start "SmartPM Frontend" cmd /k "cd /d ""%FRONTEND_DIR%"" && npm run dev"

set "READY="
for /l %%i in (1,1,30) do (
  powershell -NoProfile -Command "try { $null = Invoke-WebRequest -UseBasicParsing -TimeoutSec 1 '%FRONTEND_URL%'; exit 0 } catch { exit 1 }"
  if not errorlevel 1 set "READY=1"
  if defined READY goto frontend_ready
  timeout /t 1 /nobreak >nul
)

echo.
echo [ERROR] Frontend did not start within 30 seconds.
echo Check the "SmartPM Frontend" window for the detailed error.
popd
pause
exit /b 1

:frontend_ready
echo [3/3] Frontend is ready. Opening %FRONTEND_URL%
start "" "%FRONTEND_URL%"
popd
exit /b 0
