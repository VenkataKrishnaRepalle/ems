@echo off
setlocal

echo [%TIME%] Starting build process...

echo [%TIME%] Step 1: Building JAR with Maven...
call mvn.cmd clean package
if %ERRORLEVEL% NEQ 0 (
    echo [%TIME%] ERROR: Maven build failed with error code %ERRORLEVEL%
    pause
    exit /b %ERRORLEVEL%
)
echo [%TIME%] Maven build completed successfully

echo [%TIME%] Step 2: Building Docker image...
call docker-compose build --no-cache app
if %ERRORLEVEL% NEQ 0 (
    echo [%TIME%] ERROR: Docker build failed with error code %ERRORLEVEL%
    pause
    exit /b %ERRORLEVEL%
)
echo [%TIME%] Docker image built successfully

echo [%TIME%] Step 3: Starting containers...
call docker-compose up -d app
if %ERRORLEVEL% NEQ 0 (
    echo [%TIME%] ERROR: Failed to start containers with error code %ERRORLEVEL%
    pause
    exit /b %ERRORLEVEL%
)

echo [%TIME%] SUCCESS: Build and deployment completed successfully!
echo [%TIME%] Application should be running shortly...

timeout /t 10 >nul
