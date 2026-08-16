param(
    [int]$StartupTimeoutSeconds = 120,
    [switch]$SkipConfigServerSmoke
)

$ErrorActionPreference = "Stop"

$workspace = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$configRepo = Join-Path $workspace "backend\config-repo"
$configService = Join-Path $workspace "backend\config-service"
$logDirectory = Join-Path $workspace ".runlogs\profile-validation"
$profiles = @("dev", "docker", "uat", "prod")
$services = [ordered]@{
    "authentication-service" = @{ Port = 8086; Stateful = $true; Rabbit = $true; Redis = $true; JwtPrivate = $true }
    "user-service"           = @{ Port = 8081; Stateful = $true; Rabbit = $true }
    "authorization-service"  = @{ Port = 8082; Stateful = $true; Redis = $true }
    "organization-service"   = @{ Port = 8087; Stateful = $true; Rabbit = $true }
    "notification-service"   = @{ Port = 8085; Stateful = $true; Rabbit = $true; Mail = $true }
    "audit-service"          = @{ Port = 8089; Stateful = $true; Rabbit = $true }
    "admin-service"          = @{ Port = 8088; InternalOnly = $true }
    "api-gateway"            = @{ Port = 8080; Gateway = $true; Redis = $true }
}

function Assert-Contains {
    param([string]$Content, [string]$Pattern, [string]$Message)
    if ($Content -notmatch $Pattern) {
        throw $Message
    }
}

function Stop-ProcessTree {
    param([int]$ProcessId)
    $children = Get-CimInstance Win32_Process -Filter "ParentProcessId = $ProcessId" -ErrorAction SilentlyContinue
    foreach ($child in $children) {
        Stop-ProcessTree -ProcessId $child.ProcessId
    }
    Stop-Process -Id $ProcessId -Force -ErrorAction SilentlyContinue
}

foreach ($service in $services.GetEnumerator()) {
    foreach ($profile in $profiles) {
        $path = Join-Path $configRepo "$($service.Key)-$profile.yml"
        if (-not (Test-Path $path)) {
            throw "Missing profile: $path"
        }

        $content = Get-Content -Raw $path
        $sharedPath = Join-Path $configRepo "application-$profile.yml"
        $sharedContent = if (Test-Path $sharedPath) { Get-Content -Raw $sharedPath } else { "" }
        $mergedContent = "$sharedContent`n$content"
        Assert-Contains $content "(?m)^\s*port:\s*$($service.Value.Port)\s*$" "$($service.Key)/$profile has the wrong or missing port."

        if ($service.Value.Stateful) {
            Assert-Contains $content "(?m)^\s*datasource:\s*$" "$($service.Key)/$profile is missing its datasource."
            Assert-Contains $content "ddl-auto:\s*validate" "$($service.Key)/$profile must use Hibernate validate."
            Assert-Contains $content "(?m)^\s*flyway:\s*$" "$($service.Key)/$profile is missing Flyway configuration."
            Assert-Contains $content "(?m)^\s+enabled:\s*true\s*$" "$($service.Key)/$profile must enable Flyway."
            Assert-Contains $content "baseline-on-migrate:\s*false" "$($service.Key)/$profile must not auto-baseline."
        }
        if ($service.Value.Rabbit) {
            Assert-Contains $mergedContent "(?m)^\s*rabbitmq:\s*$" "$($service.Key)/$profile is missing RabbitMQ configuration."
        }
        if ($service.Value.Redis) {
            Assert-Contains $mergedContent "(?m)^\s*redis:\s*$" "$($service.Key)/$profile is missing Redis configuration."
        }
        if ($service.Value.Mail) {
            Assert-Contains $content "(?m)^\s*mail:\s*$" "$($service.Key)/$profile is missing mail configuration."
        }
        if ($service.Value.JwtPrivate) {
            Assert-Contains $content "private-key-path:" "$($service.Key)/$profile is missing the JWT signing-key path."
        }
        if ($service.Value.Gateway) {
            Assert-Contains $content "public-key-path:" "$($service.Key)/$profile is missing the JWT verification-key path."
            Assert-Contains $content "(?m)^\s*gateway:\s*$" "$($service.Key)/$profile is missing the gateway secret."
        }
        else {
            Assert-Contains $content "(?m)^\s*gateway:\s*$" "$($service.Key)/$profile is missing the gateway secret."
            Assert-Contains $content "(?m)^\s*service:\s*$" "$($service.Key)/$profile is missing the service secret."
        }

        Assert-Contains $mergedContent "defaultZone:" "$($service.Key)/$profile is missing Eureka configuration."

        if ($profile -in @("uat", "prod")) {
            Assert-Contains $content '\$\{INTERNAL_GATEWAY_SECRET\}' "$($service.Key)/$profile must require INTERNAL_GATEWAY_SECRET."
            if (-not $service.Value.Gateway) {
                Assert-Contains $content '\$\{INTERNAL_SERVICE_SECRET\}' "$($service.Key)/$profile must require INTERNAL_SERVICE_SECRET."
            }
        }
    }
}

Write-Host "[PASS] Static profile invariants passed for $($services.Count) services across $($profiles.Count) profiles."

if ($SkipConfigServerSmoke) {
    return
}

$maven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if (-not $maven) {
    $maven = Get-Command mvn -ErrorAction SilentlyContinue
}
$mavenExecutable = if ($maven) { $maven.Source } else { Join-Path $configService "mvnw.cmd" }
if (-not (Test-Path $mavenExecutable)) {
    throw "Maven or the Config Server Maven wrapper is required for smoke validation."
}

New-Item -ItemType Directory -Force -Path $logDirectory | Out-Null
$listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
$listener.Start()
$port = $listener.LocalEndpoint.Port
$listener.Stop()
$logPath = Join-Path $logDirectory "config-service.log"
$errorLogPath = Join-Path $logDirectory "config-service.error.log"
Remove-Item -LiteralPath $logPath, $errorLogPath -Force -ErrorAction SilentlyContinue

$arguments = @(
    "-q",
    "-DskipTests",
    "spring-boot:run",
    "`"-Dspring-boot.run.arguments=--server.port=$port --spring.profiles.active=native --spring.cloud.config.server.native.search-locations=file:$($configRepo.Replace('\','/')) --eureka.client.enabled=false`""
)

$process = Start-Process -FilePath $mavenExecutable -ArgumentList $arguments -WorkingDirectory $configService `
    -RedirectStandardOutput $logPath -RedirectStandardError $errorLogPath -WindowStyle Hidden -PassThru

try {
    $deadline = (Get-Date).AddSeconds($StartupTimeoutSeconds)
    do {
        if ($process.HasExited) {
            throw "Config Server exited before startup. See $logPath"
        }
        try {
            $health = Invoke-RestMethod "http://127.0.0.1:$port/actuator/health" -TimeoutSec 2
            if ($health.status -eq "UP") {
                break
            }
        }
        catch {
        }
        Start-Sleep -Seconds 2
        $process.Refresh()
    } while ((Get-Date) -lt $deadline)

    if ((Get-Date) -ge $deadline) {
        throw "Config Server did not become healthy within $StartupTimeoutSeconds seconds."
    }

    foreach ($service in $services.Keys) {
        foreach ($profile in $profiles) {
            $resolved = Invoke-RestMethod "http://127.0.0.1:$port/$service/$profile" -TimeoutSec 10
            if (-not $resolved.propertySources -or $resolved.propertySources.Count -lt 2) {
                throw "Config Server did not resolve shared and service configuration for $service/$profile."
            }
        }
    }

    Write-Host "[PASS] Config Server resolved all $($services.Count * $profiles.Count) service/profile combinations."
}
finally {
    if (-not $process.HasExited) {
        Stop-ProcessTree -ProcessId $process.Id
    }
}
