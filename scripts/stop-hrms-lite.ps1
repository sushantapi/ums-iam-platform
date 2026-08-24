[CmdletBinding()]
param(
    [switch]$StopInfrastructure
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$runtimeRoot = Join-Path $repoRoot ".runtime\hrms-lite"
$stateFile = Join-Path $runtimeRoot "processes.json"

Write-Host "=========================================="
Write-Host " Stopping UMS IAM + HRMS Lite"
Write-Host "=========================================="

if (Test-Path $stateFile) {
    $items = @(Get-Content $stateFile -Raw | ConvertFrom-Json)
    foreach ($item in $items) {
        $pidValue = [int]$item.pid
        $process = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
        if ($process) {
            Write-Host "Stopping $($item.service) (PID $pidValue)..."
            Stop-Process -Id $pidValue -Force -ErrorAction SilentlyContinue
        }
    }
    Remove-Item $stateFile -Force -ErrorAction SilentlyContinue
} else {
    Write-Host "No HRMS Lite process state file found."
}

if ($StopInfrastructure) {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw "Docker CLI was not found."
    }

    Write-Host "Stopping lightweight infrastructure containers..."
    foreach ($container in @("ums-mailpit", "ums-redis", "ums-rabbitmq", "ums-mysql")) {
        $running = (& docker inspect -f '{{.State.Running}}' $container 2>$null)
        if ($LASTEXITCODE -eq 0 -and $running -eq "true") {
            & docker stop $container | Out-Null
        }
    }
    Write-Host "Infrastructure stopped. Docker volumes were NOT deleted."
} else {
    Write-Host "Infrastructure left running for fast restart."
    Write-Host "Use -StopInfrastructure when you also want MySQL/RabbitMQ/Redis/Mailpit stopped."
}

Write-Host "HRMS Lite local JVMs stopped." -ForegroundColor Green
