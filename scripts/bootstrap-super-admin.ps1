param(
    [Parameter(Mandatory = $true)]
    [string]$Email,

    [string]$FirstName = "UMS",
    [string]$LastName = "Administrator",
    [string]$GatewayBaseUrl = "http://localhost:8080",
    [string]$AuthorizationBaseUrl = "http://localhost:8082"
)

$ErrorActionPreference = "Stop"

$internalServiceSecret = $env:INTERNAL_SERVICE_SECRET
if ([string]::IsNullOrWhiteSpace($internalServiceSecret)) {
    throw "INTERNAL_SERVICE_SECRET must be set in the environment before bootstrapping an administrator."
}

$password = $env:BOOTSTRAP_ADMIN_PASSWORD
if ([string]::IsNullOrWhiteSpace($password)) {
    $securePassword = Read-Host "Password for $Email" -AsSecureString
    $password = [System.Net.NetworkCredential]::new("", $securePassword).Password
}

if ([string]::IsNullOrWhiteSpace($password)) {
    throw "A non-empty bootstrap administrator password is required."
}

$credentials = @{
    email = $Email
    password = $password
}

$userId = $null

try {
    $registerBody = @{
        email = $Email
        password = $password
        firstName = $FirstName
        lastName = $LastName
        provider = "LOCAL"
    } | ConvertTo-Json

    $registerResponse = Invoke-RestMethod `
        -Method Post `
        -Uri "$GatewayBaseUrl/api/v1/auth/register" `
        -ContentType "application/json" `
        -Body $registerBody

    $userId = $registerResponse.data.userId
    Write-Host "Bootstrap user registered: $userId"
}
catch {
    Write-Host "Registration did not complete (the user may already exist). Attempting login..."

    $loginResponse = Invoke-RestMethod `
        -Method Post `
        -Uri "$GatewayBaseUrl/api/v1/auth/login" `
        -ContentType "application/json" `
        -Body ($credentials | ConvertTo-Json)

    $userId = $loginResponse.data.userId
    Write-Host "Existing bootstrap user resolved: $userId"
}
finally {
    # Do not keep the plaintext password around longer than necessary.
    $password = $null
}

if ([string]::IsNullOrWhiteSpace($userId)) {
    throw "Unable to resolve the bootstrap administrator user id."
}

$assignBody = @{
    userId = $userId
    roleName = "SUPER_ADMIN"
    scopeType = "PLATFORM"
    scopeId = "*"
    assignedBy = $userId
} | ConvertTo-Json

try {
    Invoke-RestMethod `
        -Method Post `
        -Uri "$AuthorizationBaseUrl/api/v1/internal/roles/assign" `
        -Headers @{ "X-Internal-Service-Secret" = $internalServiceSecret } `
        -ContentType "application/json" `
        -Body $assignBody | Out-Null

    Write-Host "SUPER_ADMIN role assigned successfully. Log in again to receive a token containing the new role."
}
catch {
    $statusCode = $null
    try { $statusCode = [int]$_.Exception.Response.StatusCode } catch { }

    if ($statusCode -eq 409) {
        Write-Host "SUPER_ADMIN is already assigned to this user. Log in again to obtain current authorities."
    }
    else {
        throw
    }
}
