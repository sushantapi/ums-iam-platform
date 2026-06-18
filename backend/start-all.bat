@echo off
title UMS IAM Platform - Startup Script

echo ==========================================
echo      Starting UMS IAM Platform
echo ==========================================

REM =====================================================
REM STEP 0 - RABBITMQ
REM =====================================================

echo.
echo Starting RabbitMQ...

docker start rabbitmq > nul 2>&1

echo Waiting for RabbitMQ...
timeout /t 10 /nobreak > nul

REM =====================================================
REM STEP 1 - CONFIG SERVICE
REM =====================================================

echo.
echo Starting Config Service...
start "CONFIG-SERVICE" cmd /k "cd /d E:\ums-iam-platform\backend\config-service && mvnw spring-boot:run"

echo Waiting for Config Service...
timeout /t 15 /nobreak > nul

REM =====================================================
REM STEP 2 - EUREKA DISCOVERY
REM =====================================================

echo.
echo Starting Discovery Service...
start "DISCOVERY-SERVICE" cmd /k "cd /d E:\ums-iam-platform\backend\discovery-service && mvnw spring-boot:run"

echo Waiting for Eureka...
timeout /t 20 /nobreak > nul

REM =====================================================
REM STEP 3 - AUTHORIZATION SERVICE
REM =====================================================



REM =====================================================
REM STEP 4 - ORGANIZATION SERVICE
REM =====================================================


REM =====================================================
REM STEP 5 - ADMIN SERVICE
REM =====================================================



REM =====================================================
REM STEP 6 - API GATEWAY
REM =====================================================

echo.
echo Starting API Gateway...
start "API-GATEWAY" cmd /k "cd /d E:\ums-iam-platform\backend\api-gateway && mvnw spring-boot:run"

echo.
echo ==========================================
echo     All Services Startup Triggered
echo ==========================================

pause