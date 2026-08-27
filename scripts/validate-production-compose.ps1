$ErrorActionPreference = 'Stop'

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
$composeFile = Join-Path $repoRoot 'docker-compose.prod.yml'

$validationEnv = @{
    IMAGE_REGISTRY          = 'ghcr.io/sushantapi/ums-iam-platform'
    APP_VERSION             = 'v0.0.0-validation'
    API_DOMAIN              = 'api.example.invalid'
    ACME_EMAIL              = 'ops@example.invalid'
    GATEWAY_ALLOWED_ORIGINS = 'https://app.example.invalid'
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

try {
    Push-Location $repoRoot

    $json = & docker compose -f $composeFile config --format json
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose config failed with exit code $LASTEXITCODE"
    }

    $config = $json | ConvertFrom-Json
    $violations = [System.Collections.Generic.List[string]]::new()

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

    $applicationServices = @(
        'authentication-service', 'user-service', 'authorization-service',
        'organization-service', 'notification-service', 'audit-service',
        'admin-service', 'employee-service', 'attendance-service',
        'leave-service', 'payroll-service', 'api-gateway'
    )

    foreach ($serviceName in $applicationServices) {
        $service = $config.services.$serviceName
        if ($null -eq $service) {
            $violations.Add("missing service $serviceName")
            continue
        }

        if ($null -ne $service.build) {
            $violations.Add("$serviceName contains a local build section; production must use registry images")
        }

        if ([string]$service.environment.SPRING_PROFILES_ACTIVE -ne 'prod') {
            $violations.Add("$serviceName is not using the prod Spring profile")
        }

        if (-not ([string]$service.image).StartsWith('ghcr.io/')) {
            $violations.Add("$serviceName image is not sourced from GHCR")
        }
    }

    if ([string]$config.services.'config-service'.environment.SPRING_PROFILES_ACTIVE -ne 'native') {
        $violations.Add('config-service must use the native profile')
    }

    if ([string]$config.services.'discovery-service'.environment.SPRING_PROFILES_ACTIVE -ne 'prod') {
        $violations.Add('discovery-service must use the prod profile')
    }

    if ($null -ne $config.services.mailpit) {
        $violations.Add('mailpit must not be part of the production topology')
    }

    if ($violations.Count -gt 0) {
        $violations | ForEach-Object { Write-Host "VIOLATION: $_" }
        throw "Production compose validation failed with $($violations.Count) violation(s)."
    }

    Write-Host 'PRODUCTION_COMPOSE_VALIDATION=PASS'
}
finally {
    Pop-Location -ErrorAction SilentlyContinue

    foreach ($entry in $previous.GetEnumerator()) {
        [Environment]::SetEnvironmentVariable($entry.Key, $entry.Value, 'Process')
    }
}
