[CmdletBinding()]
param(
    [switch]$StopInfrastructure
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$runtimeRoot = Join-Path $repoRoot ".runtime\hrms-lite"
$stateFile = Join-Path $runtimeRoot "processes.json"
$servicePorts = @(8761, 8888, 8082, 8087, 8095, 8098, 8086, 8080)
$stoppedProcessIds = @{}

function Stop-HrmsLiteProcess([int]$ProcessId, [string]$Label) {
    if ($ProcessId -le 0 -or $stoppedProcessIds.ContainsKey($ProcessId)) {
        return
    }

    $process = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue
    if ($process) {
        Write-Host "Stopping $Label (PID $ProcessId)..."
        Stop-Process -Id $ProcessId -Force -ErrorAction SilentlyContinue
        $stoppedProcessIds[$ProcessId] = $true
    }
}

Write-Host "=========================================="
Write-Host " Stopping UMS IAM + HRMS Lite"
Write-Host "=========================================="

if (Test-Path $stateFile) {
    $state = Get-Content $stateFile -Raw | ConvertFrom-Json
    $items = @($state)

    foreach ($item in $items) {
        if ($null -eq $item -or -not ($item.PSObject.Properties.Name -contains "pid")) {
            continue
        }

        $serviceName = if ($item.PSObject.Properties.Name -contains "service") {
            [string]$item.service
        } else {
            "HRMS Lite service"
        }

        # Be defensive about older/corrupt state files where pid may have been
        # serialized as an array instead of one scalar value.
        foreach ($candidate in @($item.pid)) {
            $processId = 0
            if ([int]::TryParse([string]$candidate, [ref]$processId)) {
                Stop-HrmsLiteProcess -ProcessId $processId -Label $serviceName
            } else {
                Write-Warning "Ignoring invalid PID value in $stateFile for $serviceName."
            }
        }
    }

    Remove-Item $stateFile -Force -ErrorAction SilentlyContinue
} else {
    Write-Host "No HRMS Lite process state file found."
}

# State files are only a convenience. If a previous run was interrupted or an
# older state file was malformed, locate stale local Java services by their
# dedicated ports and stop only processes launched from this repository.
foreach ($port in $servicePorts) {
    $connections = @(Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
    foreach ($connection in $connections) {
        $ownerProcessId = [int]$connection.OwningProcess
        if ($ownerProcessId -le 0 -or $stoppedProcessIds.ContainsKey($ownerProcessId)) {
            continue
        }

        $winProcess = Get-CimInstance Win32_Process -Filter "ProcessId=$ownerProcessId" -ErrorAction SilentlyContinue
        if (-not $winProcess) {
            continue
        }

        $commandLine = [string]$winProcess.CommandLine
        if ($winProcess.Name -match '^java(w)?\.exe$' -and $commandLine -like "*$repoRoot*") {
            Stop-HrmsLiteProcess -ProcessId $ownerProcessId -Label "stale HRMS Lite service on :$port"
        }
    }
}

if ($StopInfrastructure) {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw "Docker CLI was not found."
    }

    Write-Host "Stopping lightweight infrastructure containers..."
    foreach ($container in @("ums-mailpit", "ums-redis", "ums-rabbitmq", "ums-mysql")) {
        $previousErrorActionPreference = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        try {
            $running = (& docker inspect -f '{{.State.Running}}' $container 2>$null)
            if ($LASTEXITCODE -eq 0 -and $running -eq "true") {
                & docker stop $container | Out-Null
            }
        } finally {
            $ErrorActionPreference = $previousErrorActionPreference
        }
    }
    Write-Host "Infrastructure stopped. Docker volumes were NOT deleted."
} else {
    Write-Host "Infrastructure left running for fast restart."
    Write-Host "Use -StopInfrastructure when you also want MySQL/RabbitMQ/Redis/Mailpit stopped."
}

Write-Host "HRMS Lite local JVMs stopped." -ForegroundColor Green
