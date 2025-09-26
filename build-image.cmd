@echo off
REM Step 1: Build the latest JAR
mvn.cmd package

REM Step 2: Build the Docker image for the app, always overriding the previous one
docker-compose build --no-cache app

REM Step 3: (Optional) Start the app container (and db if not running)
docker-compose up -d app

echo Build and run complete.
