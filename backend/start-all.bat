@echo off
setlocal EnableExtensions
title UMS IAM + HRMS - Full Local Startup

set "BACKEND_DIR=%~dp0"
for %%I in ("%BACKEND_DIR%..") do set "REPO_ROOT=%%~fI"

echo ==========================================
echo   Starting UMS IAM + HRMS Full Local Stack
echo ==========================================
echo.
echo Docker : MySQL + RabbitMQ + Redis + Mailpit
echo Local  : IAM + HRMS Spring Boot services
echo.

REM =====================================================
REM STEP 0 - PRECHECKS / LOCAL ENVIRONMENT
REM =====================================================

where docker >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker CLI was not found on PATH.
    goto startup_failed
)

where java >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java was not found on PATH. Java 21 is required.
    goto startup_failed
)

docker info >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker Desktop engine is not responding.
    goto startup_failed
)

REM Load simple KEY=VALUE entries from repository .env.
REM Comment/blank lines are ignored. Local runtime host values are overridden below.
if exist "%REPO_ROOT%\.env" (
    for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%REPO_ROOT%\.env") do (
        if not "%%A"=="" set "%%A=%%~B"
    )
) else (
    echo WARNING: "%REPO_ROOT%\.env" was not found. Service defaults will be used where possible.
)

REM Local JVMs must not inherit Docker-only Spring/JVM process overrides.
REM Config Service will therefore use its own native profile and other
REM applications will fall back to their normal local defaults.
set "SPRING_PROFILES_ACTIVE="
set "SPRING_CONFIG_IMPORT="
set "JAVA_TOOL_OPTIONS="
REM Docker .env commonly contains container DNS names. Local JVMs must use localhost.
if not defined MYSQL_HOST_PORT set "MYSQL_HOST_PORT=3307"
set "MYSQL_HOST=localhost"
set "MYSQL_PORT=%MYSQL_HOST_PORT%"
set "RABBITMQ_HOST=localhost"
set "RABBITMQ_PORT=5672"
set "REDIS_HOST=localhost"
set "REDIS_PORT=6379"
set "CONFIG_SERVER_URL=http://localhost:8888"
set "EUREKA_DEFAULT_ZONE=http://localhost:8761/eureka/"

REM Config Server native repository.
set "CONFIG_REPO_PATH=file:/%BACKEND_DIR:\=/%config-repo"

REM Canonical local JWT key files.
set "JWT_PRIVATE_KEY_PATH=%REPO_ROOT%\secrets\jwt\private_key.pem"
set "JWT_PUBLIC_KEY_PATH=%REPO_ROOT%\secrets\jwt\public_key.pem"

if not exist "%JWT_PRIVATE_KEY_PATH%" (
    echo ERROR: JWT private key not found:
    echo        %JWT_PRIVATE_KEY_PATH%
    goto startup_failed
)
if not exist "%JWT_PUBLIC_KEY_PATH%" (
    echo ERROR: JWT public key not found:
    echo        %JWT_PUBLIC_KEY_PATH%
    goto startup_failed
)

REM =====================================================
REM STEP 1 - REQUIRED PORTS MUST BE FREE
REM =====================================================

echo Checking application ports...

call :require_port_free 8888 "Config Service" || goto port_in_use
call :require_port_free 8761 "Eureka Discovery" || goto port_in_use
call :require_port_free 8086 "Authentication Service" || goto port_in_use
call :require_port_free 8082 "Authorization Service" || goto port_in_use
call :require_port_free 8081 "User Service" || goto port_in_use
call :require_port_free 8087 "Organization Service" || goto port_in_use
call :require_port_free 8095 "Employee Service" || goto port_in_use
call :require_port_free 8096 "Attendance Service" || goto port_in_use
call :require_port_free 8097 "Leave Service" || goto port_in_use
call :require_port_free 8098 "Payroll Service" || goto port_in_use
call :require_port_free 8089 "Audit Service" || goto port_in_use
call :require_port_free 8085 "Notification Service" || goto port_in_use
call :require_port_free 8088 "Admin Service" || goto port_in_use
call :require_port_free 8080 "API Gateway" || goto port_in_use

REM =====================================================
REM STEP 2 - INFRASTRUCTURE
REM =====================================================

echo.
echo Starting/reusing infrastructure containers...

REM The batch loader may inherit a quoted/stale project name from .env.
REM Force a stable valid value before any Compose fallback.
set "COMPOSE_PROJECT_NAME=ums-iam-platform"

REM Reuse the named local infrastructure containers when they already exist.
for %%C in (ums-mysql ums-rabbitmq ums-redis ums-mailpit) do (
    docker inspect %%C >nul 2>&1
    if not errorlevel 1 docker start %%C >nul 2>&1
)

REM Only use Compose if one of the required containers does not exist.
docker inspect ums-mysql >nul 2>&1 || goto compose_infrastructure
docker inspect ums-rabbitmq >nul 2>&1 || goto compose_infrastructure
docker inspect ums-redis >nul 2>&1 || goto compose_infrastructure
docker inspect ums-mailpit >nul 2>&1 || goto compose_infrastructure

goto infrastructure_started

:compose_infrastructure
pushd "%REPO_ROOT%"
docker compose -p ums-iam-platform -f docker-compose.yml up -d mysql rabbitmq redis mailpit
if errorlevel 1 (
    popd
    echo ERROR: Infrastructure startup failed.
    goto startup_failed
)
popd

:infrastructure_started
echo Waiting for infrastructure...
call :wait_container ums-mysql 150 || goto startup_failed
call :wait_container ums-rabbitmq 150 || goto startup_failed
call :wait_container ums-redis 90 || goto startup_failed
call :wait_container ums-mailpit 90 || goto startup_failed

echo Infrastructure READY.
echo.
REM =====================================================
REM STEP 3 - PLATFORM FOUNDATION
REM =====================================================

call :start_service "CONFIG-SERVICE" "config-service" 8888 120 "Config Service" || goto startup_failed
call :start_service "DISCOVERY-SERVICE" "discovery-service" 8761 120 "Eureka Discovery" || goto startup_failed

REM =====================================================
REM STEP 4 - CORE IAM
REM =====================================================

call :start_service "AUTHORIZATION-SERVICE" "authorization-service" 8082 150 "Authorization Service" || goto startup_failed
call :start_service "ORGANIZATION-SERVICE" "organization-service" 8087 150 "Organization Service" || goto startup_failed
call :start_service "USER-SERVICE" "user-service" 8081 150 "User Service" || goto startup_failed
call :start_service "AUTHENTICATION-SERVICE" "authentication-service" 8086 150 "Authentication Service" || goto startup_failed

REM =====================================================
REM STEP 5 - HRMS SERVICES
REM =====================================================

call :start_service "EMPLOYEE-SERVICE" "employee-service" 8095 150 "Employee Service" || goto startup_failed
call :start_service "ATTENDANCE-SERVICE" "attendance-service" 8096 150 "Attendance Service" || goto startup_failed
call :start_service "LEAVE-SERVICE" "leave-service" 8097 150 "Leave Service" || goto startup_failed
call :start_service "PAYROLL-SERVICE" "payroll-service" 8098 180 "Payroll Service" || goto startup_failed

REM =====================================================
REM STEP 6 - SUPPORT / ADMIN
REM =====================================================

call :start_service "AUDIT-SERVICE" "audit-service" 8089 150 "Audit Service" || goto startup_failed
call :start_service "NOTIFICATION-SERVICE" "notification-service" 8085 150 "Notification Service" || goto startup_failed
call :start_service "ADMIN-SERVICE" "admin-service" 8088 150 "Admin Service" || goto startup_failed

REM Gateway last, after downstream services are healthy.
call :start_service "API-GATEWAY" "api-gateway" 8080 150 "API Gateway" || goto startup_failed

echo.
echo ==========================================
echo   UMS IAM + HRMS FULL STACK READY
echo ==========================================
echo Eureka : http://localhost:8761
echo Gateway: http://localhost:8080
echo Mailpit: http://localhost:8025
echo.
echo Started local JVM services:
echo   Config          8888
echo   Discovery       8761
echo   Authentication  8086
echo   Authorization   8082
echo   User            8081
echo   Organization    8087
echo   Employee        8095
echo   Attendance      8096
echo   Leave           8097
echo   Payroll         8098
echo   Audit           8089
echo   Notification    8085
echo   Admin           8088
echo   API Gateway     8080
echo.
echo Keep this launcher window open for status reference.
pause
exit /b 0

REM =====================================================
REM HELPERS
REM =====================================================

:start_service
echo.
echo Starting %~5...

set "SERVICE_DIR=%BACKEND_DIR%%~2"
set "SERVICE_PROFILE=dev"

if /I "%~2"=="config-service" set "SERVICE_PROFILE=native"

if not exist "%SERVICE_DIR%\pom.xml" (
    echo ERROR: pom.xml not found for %~5:
    echo        "%SERVICE_DIR%\pom.xml"
    exit /b 1
)

echo     Profile: %SERVICE_PROFILE%

if exist "%SERVICE_DIR%\mvnw.cmd" (
    echo     Using service Maven wrapper.
    start "%~1" /D "%SERVICE_DIR%" cmd /k "mvnw.cmd -Dspring-boot.run.profiles=%SERVICE_PROFILE% spring-boot:run"
) else (
    if not exist "%BACKEND_DIR%mvnw.cmd" (
        echo ERROR: Backend Maven wrapper not found.
        exit /b 1
    )

    echo     Using backend Maven wrapper.
    start "%~1" /D "%SERVICE_DIR%" cmd /k "..\mvnw.cmd -f pom.xml -Dspring-boot.run.profiles=%SERVICE_PROFILE% spring-boot:run"
)

echo Waiting for %~5 health on port %~3...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$deadline=(Get-Date).AddSeconds(%~4); do { try { $r=Invoke-RestMethod -Uri 'http://127.0.0.1:%~3/actuator/health' -Method Get -TimeoutSec 3; if($r.status -eq 'UP'){ exit 0 } } catch {}; Start-Sleep -Seconds 2 } while((Get-Date)-lt $deadline); exit 1"

if errorlevel 1 (
    echo ERROR: %~5 did not become healthy on port %~3 within %~4 seconds.
    exit /b 1
)

echo %~5 UP on :%~3
exit /b 0
:require_port_free
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "if (Get-NetTCPConnection -LocalPort %~1 -State Listen -ErrorAction SilentlyContinue) { exit 1 } else { exit 0 }"
if errorlevel 1 (
    echo ERROR: %~2 cannot start because port %~1 is already in use.
    exit /b 1
)
exit /b 0

:wait_container
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$deadline=(Get-Date).AddSeconds(%~2); do { $s=docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' '%~1' 2>$null; if($LASTEXITCODE -eq 0 -and ($s -eq 'healthy' -or $s -eq 'running')){ exit 0 }; Start-Sleep -Seconds 2 } while((Get-Date)-lt $deadline); exit 1"
if errorlevel 1 (
    echo ERROR: Container %~1 did not become ready.
    exit /b 1
)
exit /b 0

:port_in_use
echo.
echo ==========================================
echo   STARTUP STOPPED - PORT ALREADY IN USE
echo ==========================================
echo.
echo Another local/Docker application stack is already running.
echo.
echo If HRMS Lite is running, stop it first from repository root:
echo   powershell -ExecutionPolicy Bypass -File .\scripts\stop-hrms-lite.ps1
echo.
echo Then run this batch file again.
echo.
pause
exit /b 1

:startup_failed
echo.
echo ==========================================
echo   STARTUP FAILED
echo ==========================================
echo Review the service window that failed, fix the exact error, then rerun.
echo.
pause
exit /b 1
