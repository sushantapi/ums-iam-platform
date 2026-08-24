[CmdletBinding()]
param(
    [switch]$SkipBuild,
    [switch]$NoMailpit,
    [int]$StartupTimeoutSeconds = 120
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$backendRoot = Join-Path $repoRoot "backend"
$runtimeRoot = Join-Path $repoRoot ".runtime\hrms-lite"
$logsRoot = Join-Path $repoRoot "logs\hrms-lite"
$organizationAssetsRoot = Join-Path $repoRoot "runtime\organization-assets"
$stateFile = Join-Path $runtimeRoot "processes.json"

$appContainers = @(
    "ums-discovery-service",
    "ums-config-service",
    "ums-authentication-service",
    "ums-authorization-service",
    "ums-user-service",
    "ums-organization-service",
    "ums-notification-service",
    "ums-audit-service",
    "ums-admin-service",
    "ums-api-gateway",
    "ums-employee-service",
    "ums-attendance-service",
    "ums-leave-service",
    "ums-payroll-service"
)

$services = @(
    [pscustomobject]@{ Name = "discovery-service";      Module = "discovery-service";      Port = 8761; HeapMb = 160 },
    [pscustomobject]@{ Name = "config-service";         Module = "config-service";         Port = 8888; HeapMb = 160 },
    [pscustomobject]@{ Name = "authorization-service";  Module = "authorization-service";  Port = 8082; HeapMb = 256 },
    [pscustomobject]@{ Name = "organization-service";   Module = "organization-service";   Port = 8087; HeapMb = 256 },
    [pscustomobject]@{ Name = "employee-service";       Module = "employee-service";       Port = 8095; HeapMb = 256 },
    [pscustomobject]@{ Name = "payroll-service";        Module = "payroll-service";        Port = 8098; HeapMb = 320 },
    [pscustomobject]@{ Name = "authentication-service"; Module = "authentication-service"; Port = 8086; HeapMb = 288 },
    [pscustomobject]@{ Name = "api-gateway";            Module = "api-gateway";            Port = 8080; HeapMb = 224 }
)

function Write-Step([string]$Message) {
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Import-DotEnv([string]$Path) {
    if (-not (Test-Path $Path)) {
        Write-Warning ".env was not found at $Path. Local defaults will be used where available."
        return
    }

    foreach ($line in Get-Content $Path) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith("#") -or -not $trimmed.Contains("=")) {
            continue
        }

        $parts = $trimmed.Split("=", 2)
        $name = $parts[0].Trim()
        $value = $parts[1].Trim()
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
            ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        [Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
}

function Test-PortListening([int]$Port) {
    return $null -ne (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1)
}

function Wait-PortFree([int]$Port, [int]$TimeoutSeconds = 30) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        if (-not (Test-PortListening $Port)) {
            return
        }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)

    throw "Port $Port is still in use. Stop the existing process/container before starting HRMS Lite."
}

function Wait-ContainerHealthy([string]$ContainerName, [int]$TimeoutSeconds = 120) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $status = (& docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' $ContainerName 2>$null)
        if ($LASTEXITCODE -eq 0 -and ($status -eq "healthy" -or $status -eq "running")) {
            return
        }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)

    throw "$ContainerName did not become healthy within $TimeoutSeconds seconds."
}

function Get-ServiceJar([string]$Module) {
    $target = Join-Path $backendRoot "$Module\target"
    if (-not (Test-Path $target)) {
        return $null
    }

    return Get-ChildItem $target -Filter "*.jar" -File |
        Where-Object { $_.Name -notmatch '(-sources|-javadoc)' } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

function Show-LogTail([string]$ServiceName, [int]$Lines = 35) {
    $stdout = Join-Path $logsRoot "$ServiceName.out.log"
    $stderr = Join-Path $logsRoot "$ServiceName.err.log"
    if (Test-Path $stdout) {
        Write-Host "--- $ServiceName stdout ---" -ForegroundColor Yellow
        Get-Content $stdout -Tail $Lines
    }
    if (Test-Path $stderr) {
        Write-Host "--- $ServiceName stderr ---" -ForegroundColor Yellow
        Get-Content $stderr -Tail $Lines
    }
}

function Wait-ServiceHealthy([pscustomobject]$Service, [int]$ProcessId, [int]$TimeoutSeconds) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $healthUrl = "http://127.0.0.1:$($Service.Port)/actuator/health"

    do {
        $process = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue
        if (-not $process) {
            Show-LogTail $Service.Name
            throw "$($Service.Name) exited before becoming healthy."
        }

        try {
            $health = Invoke-RestMethod -Uri $healthUrl -Method Get -TimeoutSec 3
            if ($health.status -eq "UP") {
                Write-Host "    $($Service.Name) UP on :$($Service.Port) (PID $ProcessId)" -ForegroundColor Green
                return
            }
        } catch {
            # Service is still starting.
        }

        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)

    Show-LogTail $Service.Name
    throw "$($Service.Name) did not become healthy within $TimeoutSeconds seconds."
}

function Export-ExistingOrganizationAssets {
    New-Item -ItemType Directory -Force -Path $organizationAssetsRoot | Out-Null
    $hasLocalAssets = $null -ne (Get-ChildItem $organizationAssetsRoot -File -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1)
    if ($hasLocalAssets) {
        return
    }

    $runningNames = @(& docker ps --format '{{.Names}}')
    if ($runningNames -contains "ums-organization-service") {
        Write-Host "    Preserving organization logo assets from the running organization container..."
        $previousErrorActionPreference = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        try {
            & docker cp "ums-organization-service:/app/data/organization-assets/." $organizationAssetsRoot 2>$null
            if ($LASTEXITCODE -eq 0) {
                return
            }
        } finally {
            $ErrorActionPreference = $previousErrorActionPreference
        }
    }

    $volume = @(& docker volume ls --format '{{.Name}}') |
        Where-Object { $_ -like '*organization_assets' } |
        Select-Object -First 1

    if ($volume) {
        Write-Host "    Preserving organization logo assets from Docker volume $volume..."
        $previousErrorActionPreference = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        try {
            & docker run --rm `
                --entrypoint sh `
                -v "${volume}:/from:ro" `
                -v "${organizationAssetsRoot}:/to" `
                mysql:8.0 `
                -c "cp -a /from/. /to/" 2>$null
            if ($LASTEXITCODE -eq 0) {
                return
            }
        } finally {
            $ErrorActionPreference = $previousErrorActionPreference
        }
    }

    Write-Warning "No existing organization logo assets could be copied. Database/volume data is untouched; re-upload the logo only if the local preview is missing."
}

function Stop-DockerApplicationContainers {
    $runningNames = @(& docker ps --format '{{.Names}}')
    foreach ($name in $appContainers) {
        if ($runningNames -contains $name) {
            Write-Host "    stopping $name"
            & docker stop $name | Out-Null
        }
    }
}

function Start-LocalService([pscustomobject]$Service) {
    if (Test-PortListening $Service.Port) {
        throw "$($Service.Name) cannot start because port $($Service.Port) is already in use."
    }

    $jar = Get-ServiceJar $Service.Module
    if (-not $jar) {
        throw "No runnable JAR found for $($Service.Name). Rerun without -SkipBuild."
    }

    $moduleDir = Join-Path $backendRoot $Service.Module
    $stdout = Join-Path $logsRoot "$($Service.Name).out.log"
    $stderr = Join-Path $logsRoot "$($Service.Name).err.log"
    Remove-Item $stdout, $stderr -ErrorAction SilentlyContinue

    $javaArgs = @(
        "-Xms64m",
        "-Xmx$($Service.HeapMb)m",
        "-XX:+UseSerialGC",
        "-jar",
        $jar.FullName
    )

    $process = Start-Process -FilePath "java.exe" `
        -ArgumentList $javaArgs `
        -WorkingDirectory $moduleDir `
        -RedirectStandardOutput $stdout `
        -RedirectStandardError $stderr `
        -WindowStyle Hidden `
        -PassThru

    Wait-ServiceHealthy -Service $Service -ProcessId $process.Id -TimeoutSeconds $StartupTimeoutSeconds

    return [pscustomobject]@{
        service = $Service.Name
        pid = $process.Id
        port = $Service.Port
        startedAt = (Get-Date).ToString("o")
    }
}

Write-Host "=========================================="
Write-Host " UMS IAM + HRMS Lite (Hybrid Local Runtime)"
Write-Host "=========================================="
Write-Host "Docker: MySQL + RabbitMQ + Redis + optional Mailpit"
Write-Host "Local JVM: Config + Eureka + Auth/AuthZ + Org + Employee + Payroll + Gateway"
Write-Host ""
Write-Host "This mode intentionally does NOT start the full IAM dashboard/support stack."

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker CLI was not found."
}
if (-not (Get-Command java.exe -ErrorAction SilentlyContinue)) {
    throw "java.exe was not found. Java 21 must be available on PATH."
}

Import-DotEnv (Join-Path $repoRoot ".env")

# Docker .env values use container DNS names. Override only runtime-location values;
# credentials/secrets remain inherited without being printed.
$mysqlHostPort = if ($env:MYSQL_HOST_PORT) { [int]$env:MYSQL_HOST_PORT } else { 3307 }
$env:MYSQL_HOST = "localhost"
$env:MYSQL_PORT = "$mysqlHostPort"
$env:RABBITMQ_HOST = "localhost"
$env:RABBITMQ_PORT = "5672"
$env:REDIS_HOST = "localhost"
$env:REDIS_PORT = "6379"
$env:CONFIG_SERVER_URL = "http://localhost:8888"
$env:EUREKA_DEFAULT_ZONE = "http://localhost:8761/eureka/"
$env:CONFIG_REPO_PATH = ("file:/" + ((Join-Path $backendRoot "config-repo").Replace('\', '/')))
$env:ORGANIZATION_LOGO_STORAGE_ROOT = $organizationAssetsRoot
$env:JAVA_TOOL_OPTIONS = ""
Remove-Item Env:SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue
Remove-Item Env:SPRING_CONFIG_IMPORT -ErrorAction SilentlyContinue
Remove-Item Env:OTEL_EXPORTER_OTLP_ENDPOINT -ErrorAction SilentlyContinue

$jwtPrivate = Join-Path $repoRoot "secrets\jwt\private_key.pem"
$jwtPublic = Join-Path $repoRoot "secrets\jwt\public_key.pem"
if (-not (Test-Path $jwtPrivate) -or -not (Test-Path $jwtPublic)) {
    throw "Canonical JWT key files are required under $repoRoot\secrets\jwt."
}
$env:JWT_PRIVATE_KEY_PATH = $jwtPrivate
$env:JWT_PUBLIC_KEY_PATH = $jwtPublic

Write-Step "Checking Docker Desktop engine"
$dockerServerVersion = (& docker version --format '{{.Server.Version}}' 2>$null)
if ($LASTEXITCODE -ne 0 -or -not $dockerServerVersion) {
    throw "Docker Desktop engine is not responding. Restart Docker Desktop first, then rerun this script."
}
Write-Host "    Docker Engine $dockerServerVersion" -ForegroundColor Green

New-Item -ItemType Directory -Force -Path $runtimeRoot, $logsRoot, $organizationAssetsRoot | Out-Null

Write-Step "Preserving organization branding assets, then stopping heavy Docker app containers"
Export-ExistingOrganizationAssets
Stop-DockerApplicationContainers
foreach ($service in $services) {
    Wait-PortFree -Port $service.Port
}

Write-Step "Starting only lightweight infrastructure containers"
Push-Location $repoRoot
try {
    $infraServices = @("mysql", "rabbitmq", "redis")
    if (-not $NoMailpit) {
        $infraServices += "mailpit"
    }
    & docker compose -f docker-compose.yml up -d $infraServices
    if ($LASTEXITCODE -ne 0) {
        throw "Docker infrastructure startup failed."
    }
} finally {
    Pop-Location
}

Wait-ContainerHealthy "ums-mysql" 150
Wait-ContainerHealthy "ums-rabbitmq" 150
Wait-ContainerHealthy "ums-redis" 90
Write-Host "    infrastructure ready (MySQL localhost:$mysqlHostPort, RabbitMQ :5672, Redis :6379)" -ForegroundColor Green

if (-not $SkipBuild) {
    Write-Step "Building the eight required services once (tests skipped)"
    $mvnw = Join-Path $backendRoot "mvnw.cmd"
    if (-not (Test-Path $mvnw)) {
        throw "Backend Maven wrapper not found: $mvnw"
    }
    $moduleList = ($services | ForEach-Object { $_.Module }) -join ","
    Push-Location $backendRoot
    try {
        & $mvnw -B -ntp -pl $moduleList -am package -DskipTests
        if ($LASTEXITCODE -ne 0) {
            throw "Maven package failed. Local services were not started."
        }
    } finally {
        Pop-Location
    }
} else {
    Write-Step "Skipping Maven build and reusing existing JARs"
}

Write-Step "Starting local services sequentially with health polling"
$started = @()
try {
    foreach ($service in $services) {
        Write-Host "    starting $($service.Name)..."
        $started += Start-LocalService $service
    }
    $started | ConvertTo-Json -Depth 4 | Set-Content -Path $stateFile -Encoding UTF8
} catch {
    Write-Host ""
    Write-Host "Startup failed. Stopping local JVMs started by this run..." -ForegroundColor Red
    foreach ($item in $started) {
        Stop-Process -Id $item.pid -Force -ErrorAction SilentlyContinue
    }
    throw
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Green
Write-Host " HRMS LITE READY" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host "Eureka:       http://localhost:8761"
Write-Host "Gateway:      http://localhost:8080"
Write-Host "Auth:         http://localhost:8086"
Write-Host "Organization: http://localhost:8087"
Write-Host "Employee:     http://localhost:8095"
Write-Host "Payroll:      http://localhost:8098"
if (-not $NoMailpit) {
    Write-Host "Mailpit:      http://localhost:8025"
}
Write-Host "Logs:         $logsRoot"
Write-Host ""
Write-Host "Stop local JVMs with: .\scripts\stop-hrms-lite.ps1"
Write-Host "On repeat starts, use -SkipBuild when source has not changed."