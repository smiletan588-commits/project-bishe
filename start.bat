@echo off
setlocal
cd /d "%~dp0"

echo Starting SmartPM...
docker compose up -d --build
if errorlevel 1 (
  echo.
  echo SmartPM failed to start. Please make sure Docker Desktop is running.
  pause
  exit /b 1
)

echo.
echo SmartPM is running at http://localhost
start "" "http://localhost"
endlocal
