@echo off
setlocal
title UMS IAM Platform - Startup Script

set "BACKEND_DIR=%~dp0"

echo ==========================================
echo      Starting UMS IAM Platform
echo ==========================================

REM =====================================================
REM STEP 0 - INFRASTRUCTURE
REM =====================================================

echo.
echo Starting infrastructure containers...
docker start ums-mysql > nul 2>&1
docker start ums-rabbitmq > nul 2>&1
docker start ums-redis > nul 2>&1

echo Waiting for MySQL, RabbitMQ, and Redis...
timeout /t 15 /nobreak > nul

REM Do not launch local Spring Boot services on top of an already running stack.
REM If any app port is occupied, stop here with a clear message instead of
REM opening windows that fail with "Port was already in use".
call :require_port_free 8761 "Eureka Discovery" || goto port_in_use
call :require_port_free 8888 "Config Service" || goto port_in_use
call :require_port_free 8086 "Authentication Service" || goto port_in_use
call :require_port_free 8082 "Authorization Service" || goto port_in_use
call :require_port_free 8081 "User Service" || goto port_in_use
call :require_port_free 8087 "Organization Service" || goto port_in_use
call :require_port_free 8089 "Audit Service" || goto port_in_use
call :require_port_free 8085 "Notification Service" || goto port_in_use
call :require_port_free 8088 "Admin Service" || goto port_in_use
call :require_port_free 8080 "API Gateway" || goto port_in_use

REM =====================================================
REM STEP 1 - CONFIG SERVICE
REM =====================================================

call :start_service "CONFIG-SERVICE" "config-service" 15 "Config Service" || goto startup_failed

REM =====================================================
REM STEP 2 - EUREKA DISCOVERY
REM =====================================================

call :start_service "DISCOVERY-SERVICE" "discovery-service" 20 "Eureka Discovery" || goto startup_failed

REM =====================================================
REM STEP 3 - CORE IAM SERVICES
REM =====================================================

call :start_service "AUTHENTICATION-SERVICE" "authentication-service" 20 "Authentication Service" || goto startup_failed
call :start_service "AUTHORIZATION-SERVICE" "authorization-service" 15 "Authorization Service" || goto startup_failed
call :start_service "USER-SERVICE" "user-service" 15 "User Service" || goto startup_failed
call :start_service "ORGANIZATION-SERVICE" "organization-service" 15 "Organization Service" || goto startup_failed

REM =====================================================
REM STEP 4 - SUPPORTING SERVICES
REM =====================================================

call :start_service "AUDIT-SERVICE" "audit-service" 15 "Audit Service" || goto startup_failed
call :start_service "NOTIFICATION-SERVICE" "notification-service" 15 "Notification Service" || goto startup_failed

REM =====================================================
REM STEP 5 - ADMIN AND GATEWAY
REM =====================================================

call :start_service "ADMIN-SERVICE" "admin-service" 15 "Admin Service" || goto startup_failed
call :start_service "API-GATEWAY" "api-gateway" 15 "API Gateway" || goto startup_failed

echo.
echo ==========================================
echo     All Services Startup Triggered
echo ==========================================
echo.
echo Check each opened service window for startup errors.
echo If infrastructure containers were missing, run:
echo docker compose up -d mysql rabbitmq redis
echo.

pause
exit /b 0

:port_in_use
echo.
echo ==========================================
echo     Startup Script Stopped
echo ==========================================
echo A required service port is already in use.
echo.
echo If Docker Compose is running the UMS stack, use Docker only,
echo or stop it first from the project root:
echo docker compose down
echo.
pause
exit /b 1

:startup_failed
echo.
echo ==========================================
echo     Startup Script Stopped
echo ==========================================
echo Check the error above, then rerun this script.
echo.
pause
exit /b 1

:start_service
echo.
echo Starting %~4...
if not exist "%BACKEND_DIR%\%~2\mvnw.cmd" (
    echo ERROR: Maven wrapper not found for %~4 at "%BACKEND_DIR%\%~2\mvnw.cmd"
    exit /b 1
)
start "%~1" /D "%BACKEND_DIR%\%~2" cmd /k "mvnw.cmd spring-boot:run"

echo Waiting for %~4...
timeout /t %~3 /nobreak > nul
exit /b 0

:require_port_free
powershell -NoProfile -ExecutionPolicy Bypass -Command "if (Get-NetTCPConnection -LocalPort %~1 -State Listen -ErrorAction SilentlyContinue) { exit 1 }"
if errorlevel 1 (
    echo ERROR: %~2 cannot start because port %~1 is already in use.
    exit /b 1
)
exit /b 0
