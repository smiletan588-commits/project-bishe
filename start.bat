@echo off
setlocal EnableExtensions

call :main
set "SMARTPM_EXIT_CODE=%ERRORLEVEL%"
endlocal & exit /b %SMARTPM_EXIT_CODE%

:main
cd /d "%~dp0"

title SmartPM One-Click Launcher

set "APP_URL=http://localhost:3000"
set "FRONTEND_HEALTH_URL=http://127.0.0.1:3000/"

echo.
echo ==============================================
echo   SmartPM - Docker Compose One-Click Launcher
echo ==============================================
echo.
echo This launcher starts:
echo   - MySQL 8 database
echo   - Redis 7
echo   - Spring Boot backend
echo   - Vue frontend with Nginx
echo.

if not exist "docker-compose.yml" (
  echo [ERROR] docker-compose.yml was not found in:
  echo         %CD%
  call :show_failure
  exit /b 1
)

where docker >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Docker was not found.
  echo.
  echo Install Docker Desktop, restart Windows if requested, and run this file again:
  echo https://www.docker.com/products/docker-desktop/
  call :show_failure
  exit /b 1
)

docker compose version >nul 2>&1
if errorlevel 1 (
  echo [ERROR] The Docker Compose plugin is unavailable.
  echo Update Docker Desktop and run this file again.
  call :show_failure
  exit /b 1
)

call :ensure_port_3000
if errorlevel 1 (
  call :show_failure
  exit /b 1
)

docker info >nul 2>&1
if errorlevel 1 (
  powershell -NoProfile -Command "$cpu = Get-CimInstance Win32_Processor -ErrorAction SilentlyContinue; if (-not $cpu) { exit 2 }; if ($cpu.VirtualizationFirmwareEnabled -contains $true) { exit 0 }; exit 1" >nul 2>&1
  if errorlevel 2 (
    echo [INFO] Windows could not determine the firmware virtualization status.
  ) else if errorlevel 1 (
    echo [ERROR] CPU virtualization is disabled in BIOS/UEFI.
    echo.
    echo On an ASUS AMD computer, restart into BIOS and enable:
    echo   Advanced Mode ^(F7^) ^> Advanced ^> CPU Configuration ^> SVM Mode
    echo Then press F10 to save, reboot Windows, and run this file again.
    call :show_failure
    exit /b 1
  )

  echo [1/4] Docker Desktop is not running. Starting it now...

  call :start_docker_desktop
  if errorlevel 1 (
    echo [ERROR] Docker Desktop could not be located or started automatically.
    echo Start Docker Desktop manually, wait until it is ready, then run this file again.
    call :show_failure
    exit /b 1
  )

  echo       Waiting for the Docker engine to become ready...

  for /l %%i in (1,1,120) do (
    docker info >nul 2>&1 && goto :docker_ready
    timeout /t 1 /nobreak >nul
  )

  echo [ERROR] Docker Desktop did not become ready within 120 seconds.
  echo Check Docker Desktop for startup errors and try again.
  call :show_failure
  exit /b 1
)

:docker_ready
echo [1/4] Docker is ready.

rem Docker Desktop stores images, build cache and named volumes inside this VHDX.
rem Stop before downloading anything when it is still using the system drive.
if exist "%LOCALAPPDATA%\Docker\wsl\disk\docker_data.vhdx" (
  powershell -NoProfile -Command "$p = Get-Item -LiteralPath '%LOCALAPPDATA%\Docker\wsl'; if ($p.Attributes -band [IO.FileAttributes]::ReparsePoint) { exit 0 }; exit 1" >nul 2>&1
  if errorlevel 1 (
    echo.
    echo [ERROR] Docker data is currently stored on the C drive:
    echo         %LOCALAPPDATA%\Docker\wsl\disk\docker_data.vhdx
    echo.
    echo To avoid filling the system drive, open Docker Desktop and go to:
    echo Settings ^> Resources ^> Advanced ^> Disk image location
    echo Set it to a folder on D: or E:, then Apply and restart.
    call :show_failure
    exit /b 1
  )
)

if not exist ".env" (
  echo [INFO] No .env file was found. Default database credentials will be used.
  echo        AI features remain disabled until AI_API_KEY is configured in .env.
)

echo [2/4] Building images and starting all services...
echo       The first run can take several minutes while images are downloaded.
docker compose up -d --build
if errorlevel 1 (
  echo.
  echo [ERROR] Docker Compose failed to build or start the services.
  echo.
  docker compose ps
  echo.
  echo Run "docker compose logs" in this directory for detailed diagnostics.
  call :show_failure
  exit /b 1
)

echo [3/4] Waiting for the backend and frontend to become healthy...

for /l %%i in (1,1,120) do (
  docker compose exec -T backend curl -fsS http://localhost:8080/actuator/health >nul 2>&1
  if not errorlevel 1 goto :backend_ready
  timeout /t 2 /nobreak >nul
)

echo.
echo [ERROR] The backend did not become healthy within 4 minutes.
docker compose ps
echo.
echo Recent backend logs:
docker compose logs --tail 40 backend
call :show_failure
exit /b 1

:ensure_port_3000
rem Hyper-V/WinNAT can reserve port 3000 when Windows has an unusually low
rem dynamic TCP range. Repair it once and keep the public URL predictable.
powershell -NoProfile -Command "$blocked = netsh interface ipv4 show excludedportrange protocol=tcp | Select-String '^\s*(\d+)\s+(\d+)' | ForEach-Object { $m = [regex]::Match($_.Line, '^\s*(\d+)\s+(\d+)'); [pscustomobject]@{ Start = [int]$m.Groups[1].Value; End = [int]$m.Groups[2].Value } } | Where-Object { $_.Start -le 3000 -and $_.End -ge 3000 }; if ($blocked) { exit 1 }; exit 0" >nul 2>&1
if not errorlevel 1 exit /b 0

echo.
echo [INFO] Windows has reserved TCP port 3000 for Hyper-V/WinNAT.
echo        Administrator approval is required once to release it.
echo        Docker Desktop will restart automatically afterwards.
echo.

docker desktop stop >nul 2>&1

set "PORT_FIX_SCRIPT=%~dp0tools\fix-windows-port-3000.ps1"
set "PORT_FIX_RESULT=%TEMP%\smartpm-port-3000-fix.log"

if not exist "%PORT_FIX_SCRIPT%" (
  echo [ERROR] Port repair script was not found:
  echo         %PORT_FIX_SCRIPT%
  exit /b 1
)

del /q "%PORT_FIX_RESULT%" >nul 2>&1
powershell -NoProfile -ExecutionPolicy Bypass -File "%PORT_FIX_SCRIPT%" -ResultFile "%PORT_FIX_RESULT%"
if errorlevel 1 (
  echo.
  echo [ERROR] Port 3000 repair was cancelled or failed.
  if exist "%PORT_FIX_RESULT%" type "%PORT_FIX_RESULT%"
  exit /b 1
)

powershell -NoProfile -Command "$blocked = netsh interface ipv4 show excludedportrange protocol=tcp | Select-String '^\s*(\d+)\s+(\d+)' | ForEach-Object { $m = [regex]::Match($_.Line, '^\s*(\d+)\s+(\d+)'); [pscustomobject]@{ Start = [int]$m.Groups[1].Value; End = [int]$m.Groups[2].Value } } | Where-Object { $_.Start -le 3000 -and $_.End -ge 3000 }; if ($blocked) { exit 1 }; exit 0" >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Windows still reserves TCP port 3000.
  if exist "%PORT_FIX_RESULT%" type "%PORT_FIX_RESULT%"
  exit /b 1
)

echo [INFO] TCP port 3000 is available now.
exit /b 0

:backend_ready
for /l %%i in (1,1,30) do (
  curl.exe -fsS --max-time 2 "%FRONTEND_HEALTH_URL%" >nul 2>&1
  if not errorlevel 1 goto :app_ready
  timeout /t 2 /nobreak >nul
)

echo.
echo [ERROR] The frontend did not become available within 60 seconds.
docker compose ps
echo.
echo Recent Nginx logs:
docker compose logs --tail 40 nginx
call :show_failure
exit /b 1

:app_ready
echo [4/4] SmartPM is ready.
echo.
docker compose ps
echo.
echo Opening %APP_URL%
start "" "%APP_URL%"
echo.
echo You may close this window. The Docker containers will keep running.
echo To stop SmartPM later, run: docker compose down
echo.
pause
exit /b 0

:show_failure
echo.
echo SmartPM was not started successfully.
echo Press any key to close this window.
pause >nul
exit /b 0

:start_docker_desktop
rem Recent Docker Desktop versions expose a location-independent CLI command.
docker desktop start >nul 2>&1
if not errorlevel 1 exit /b 0

rem Try the standard machine-wide installation first.
if exist "%ProgramFiles%\Docker\Docker\Docker Desktop.exe" (
  start "" "%ProgramFiles%\Docker\Docker\Docker Desktop.exe"
  exit /b 0
)

rem Also support per-user and 32-bit/custom installations.
if exist "%ProgramFiles(x86)%\Docker\Docker\Docker Desktop.exe" (
  start "" "%ProgramFiles(x86)%\Docker\Docker\Docker Desktop.exe"
  exit /b 0
)
if exist "%LOCALAPPDATA%\Docker\Docker Desktop.exe" (
  start "" "%LOCALAPPDATA%\Docker\Docker Desktop.exe"
  exit /b 0
)
if exist "%LOCALAPPDATA%\Programs\Docker\Docker\Docker Desktop.exe" (
  start "" "%LOCALAPPDATA%\Programs\Docker\Docker\Docker Desktop.exe"
  exit /b 0
)

rem docker.exe normally lives under Docker\Docker\resources\bin. Derive the
rem desktop executable from its actual location to support custom install drives.
for /f "delims=" %%D in ('where docker 2^>nul') do (
  if exist "%%~dpD..\..\Docker Desktop.exe" (
    start "" "%%~dpD..\..\Docker Desktop.exe"
    exit /b 0
  )
)

rem Fall back to Start Menu shortcuts.
if exist "%ProgramData%\Microsoft\Windows\Start Menu\Programs\Docker Desktop.lnk" (
  start "" "%ProgramData%\Microsoft\Windows\Start Menu\Programs\Docker Desktop.lnk"
  exit /b 0
)
if exist "%APPDATA%\Microsoft\Windows\Start Menu\Programs\Docker Desktop.lnk" (
  start "" "%APPDATA%\Microsoft\Windows\Start Menu\Programs\Docker Desktop.lnk"
  exit /b 0
)

rem Last resort: ask Windows for the registered Start Menu application ID.
powershell -NoProfile -Command "$app = Get-StartApps ^| Where-Object { $_.Name -eq 'Docker Desktop' } ^| Select-Object -First 1; if (-not $app) { exit 1 }; Start-Process ('shell:AppsFolder\' + $app.AppID)" >nul 2>&1
if not errorlevel 1 exit /b 0
exit /b 1
