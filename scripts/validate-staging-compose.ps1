$ErrorActionPreference = 'Stop'

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
$productionCompose = Join-Path $repoRoot 'docker-compose.prod.yml'
$stagingCompose = Join-Path $repoRoot 'docker-compose.staging.yml'
$dockerfile = Join-Path $repoRoot 'backend/Dockerfile'
$publishWorkflow = Join-Path $repoRoot '.github/workflows/publish-images.yml'

$validationEnv = @{
    IMAGE_REGISTRY          = 'ghcr.io/sushantapi/ums-iam-platform'
    APP_VERSION             = 'v0.0.0-validation'
    API_DOMAIN              = 'api.example.invalid'
    ACME_EMAIL              = 'ops@example.invalid'
    MYSQL_ROOT_PASSWORD     = 'validation-root-password'
    MYSQL_USER              = 'ums_app'
    MYSQL_PASSWORD          = 'validation-app-password'
    RABBITMQ_DEFAULT_USER   = 'ums_app'
    RABBITMQ_DEFAULT_PASS   = 'validation-rabbit-password'
    REDIS_PASSWORD          = 'validation-redis-password'
    INTERNAL_GATEWAY_SECRET = 'validation-gateway-secret-32-bytes-minimum'
    INTERNAL_SERVICE_SECRET = 'validation-service-secret-32-bytes-minimum'
    JWT_KEY_ID              = 'validation-key-id'
    MFA_ENCRYPTION_KEY      = 'validation-mfa-encryption-key-material'
    PASSWORD_RESET_PAGE_URL = 'https://app.example.invalid/reset-password'
    MAIL_HOST               = 'smtp.example.invalid'
    MAIL_USERNAME           = 'validation@example.invalid'
    MAIL_PASSWORD           = 'validation-mail-password'
    MAIL_FROM               = 'noreply@example.invalid'
}

$previous = @{}
foreach ($entry in $validationEnv.GetEnumerator()) {
    $previous[$entry.Key] = [Environment]::GetEnvironmentVariable($entry.Key, 'Process')
    [Environment]::SetEnvironmentVariable($entry.Key, $entry.Value, 'Process')
}

function Convert-MemoryValueToBytes {
    param([object]$Value)

    if ($null -eq $Value) { return [int64]0 }

    $text = ([string]$Value).Trim().ToLowerInvariant()
    if ($text -match '^\d+$') {
        return [int64]$text
    }

    if ($text -match '^(\d+)([kmgt])b?$') {
        $amount = [int64]$Matches[1]
        switch ($Matches[2]) {
            'k' { return $amount * 1KB }
            'm' { return $amount * 1MB }
            'g' { return $amount * 1GB }
            't' { return $amount * 1TB }
        }
    }

    throw "Unsupported memory value '$Value'."
}

try {
    Push-Location $repoRoot

    $json = & docker compose -f $productionCompose -f $stagingCompose config --format json
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose staging config failed with exit code $LASTEXITCODE"
    }

    $config = $json | ConvertFrom-Json
    $violations = [System.Collections.Generic.List[string]]::new()

    if ([string]$config.name -ne 'ums-iam-staging') {
        $violations.Add("expected compose project name ums-iam-staging, got '$($config.name)'")
    }

    foreach ($serviceProperty in $config.services.PSObject.Properties) {
        $serviceName = $serviceProperty.Name
        $service = $serviceProperty.Value

        foreach ($port in @($service.ports)) {
            if ($null -eq $port) { continue }
            $published = [string]$port.published
            if ([string]::IsNullOrWhiteSpace($published)) { continue }

            if ($serviceName -ne 'caddy') {
                $violations.Add("$serviceName publishes host port $published")
                continue
            }

            $target = [string]$port.target
            if ($target -notin @('80', '443')) {
                $violations.Add("caddy publishes unexpected target port $target")
            }
        }
    }

    $smallJvmServices = @('discovery-service', 'config-service')
    $appServices = @(
        'authentication-service', 'user-service', 'authorization-service',
        'organization-service', 'notification-service', 'audit-service',
        'admin-service', 'employee-service', 'attendance-service',
        'leave-service', 'payroll-service', 'api-gateway'
    )

    $expectedLimits = @{
        'caddy'                  = 128MB
        'mysql'                  = 1536MB
        'rabbitmq'               = 768MB
        'redis'                  = 256MB
        'discovery-service'      = 384MB
        'config-service'         = 384MB
        'authentication-service' = 512MB
        'user-service'           = 512MB
        'authorization-service'  = 512MB
        'organization-service'   = 512MB
        'notification-service'   = 512MB
        'audit-service'          = 512MB
        'admin-service'          = 512MB
        'employee-service'       = 512MB
        'attendance-service'     = 512MB
        'leave-service'          = 512MB
        'payroll-service'        = 512MB
        'api-gateway'            = 512MB
    }

    [int64]$totalLimitBytes = 0
    foreach ($entry in $expectedLimits.GetEnumerator()) {
        $serviceProperty = $config.services.PSObject.Properties[$entry.Key]
        $service = if ($null -eq $serviceProperty) { $null } else { $serviceProperty.Value }
        if ($null -eq $service) {
            $violations.Add("missing service $($entry.Key)")
            continue
        }

        $actualLimit = Convert-MemoryValueToBytes $service.mem_limit
        $expectedLimit = [int64]$entry.Value
        if ($actualLimit -ne $expectedLimit) {
            $violations.Add("$($entry.Key) mem_limit expected $expectedLimit bytes, got $actualLimit")
        }
        $totalLimitBytes += $actualLimit
    }

    if ($totalLimitBytes -gt 10GB) {
        $violations.Add("staging hard memory limits total $totalLimitBytes bytes; expected <= 10 GiB")
    }

    foreach ($serviceName in $smallJvmServices) {
        $service = $config.services.$serviceName
        $javaOptions = [string]$service.environment.JAVA_TOOL_OPTIONS
        if ($javaOptions -notmatch '(?:^|\s)-Xmx192m(?:\s|$)') {
            $violations.Add("$serviceName does not use the staging -Xmx192m heap")
        }
        if (-not [string]::IsNullOrWhiteSpace([string]$service.platform)) {
            $violations.Add("$serviceName pins platform '$($service.platform)'; host-native manifest selection is required")
        }
    }

    foreach ($serviceName in $appServices) {
        $service = $config.services.$serviceName
        if ($null -eq $service) {
            $violations.Add("missing service $serviceName")
            continue
        }

        if ($null -ne $service.build) {
            $violations.Add("$serviceName contains a local build section; staging must use registry images")
        }
        if (-not ([string]$service.image).StartsWith('ghcr.io/')) {
            $violations.Add("$serviceName image is not sourced from GHCR")
        }
        if ([string]$service.environment.SPRING_PROFILES_ACTIVE -ne 'prod') {
            $violations.Add("$serviceName is not using the prod Spring profile")
        }

        $javaOptions = [string]$service.environment.JAVA_TOOL_OPTIONS
        if ($javaOptions -notmatch '(?:^|\s)-Xmx256m(?:\s|$)') {
            $violations.Add("$serviceName does not use the staging -Xmx256m heap")
        }
        if (-not [string]::IsNullOrWhiteSpace([string]$service.platform)) {
            $violations.Add("$serviceName pins platform '$($service.platform)'; host-native manifest selection is required")
        }
    }

    if ([string]$config.services.'config-service'.environment.SPRING_PROFILES_ACTIVE -ne 'native') {
        $violations.Add('config-service must use the native profile')
    }

    $mysqlCommand = (@($config.services.mysql.command) -join ' ')
    if ($mysqlCommand -notmatch '--innodb-buffer-pool-size=512M') {
        $violations.Add('mysql staging tuning must cap InnoDB buffer pool at 512M')
    }
    if ($mysqlCommand -notmatch '--max-connections=80') {
        $violations.Add('mysql staging tuning must cap max connections at 80')
    }

    if ($null -ne $config.services.mailpit) {
        $violations.Add('mailpit must not be part of the staging production topology')
    }

    $dockerfileText = Get-Content $dockerfile -Raw
    if (-not $dockerfileText.Contains('FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS build')) {
        $violations.Add('backend Dockerfile must compile on BUILDPLATFORM for efficient multi-arch builds')
    }

    $workflowText = Get-Content $publishWorkflow -Raw
    if (-not $workflowText.Contains('platforms: linux/amd64,linux/arm64')) {
        $violations.Add('publish-images workflow must publish linux/amd64 and linux/arm64')
    }
    if (-not $workflowText.Contains('docker/setup-qemu-action@v3')) {
        $violations.Add('publish-images workflow must set up ARM64 emulation support')
    }
    if (-not $workflowText.Contains('architecture == $arch')) {
        $violations.Add('publish-images workflow must verify both target architectures')
    }

    if ($violations.Count -gt 0) {
        $violations | ForEach-Object { Write-Host "VIOLATION: $_" }
        throw "Staging compose validation failed with $($violations.Count) violation(s)."
    }

    Write-Host "STAGING_MEMORY_LIMIT_TOTAL_BYTES=$totalLimitBytes"
    Write-Host 'STAGING_COMPOSE_VALIDATION=PASS'
}
finally {
    Pop-Location -ErrorAction SilentlyContinue

    foreach ($entry in $previous.GetEnumerator()) {
        [Environment]::SetEnvironmentVariable($entry.Key, $entry.Value, 'Process')
    }
}
