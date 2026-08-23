param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [string]$Password = "SmokePass@123",
    [switch]$AllowLocalMutation
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Fail([string]$Message) {
    throw "FAIL: $Message"
}

function Assert-True([bool]$Condition, [string]$Message) {
    if (-not $Condition) {
        Fail $Message
    }
}

function Get-HttpStatusFromException($Exception) {
    if ($null -eq $Exception.Response) {
        return 0
    }

    try {
        return [int]$Exception.Response.StatusCode
    }
    catch {
        try {
            return [int]$Exception.Response.StatusCode.value__
        }
        catch {
            return 0
        }
    }
}

function Invoke-UmsApi {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        $Body = $null,
        [string]$Token = $null,
        [hashtable]$ExtraHeaders = $null
    )

    $headers = @{
        Accept = "application/json"
    }

    if (-not [string]::IsNullOrWhiteSpace($Token)) {
        $headers.Authorization = "Bearer $Token"
    }

    if ($null -ne $ExtraHeaders) {
        foreach ($key in $ExtraHeaders.Keys) {
            $headers[$key] = $ExtraHeaders[$key]
        }
    }

    $params = @{
        Uri         = "$BaseUrl$Path"
        Method      = $Method
        Headers     = $headers
        ErrorAction = "Stop"
    }

    if ($null -ne $Body) {
        $params.ContentType = "application/json"
        $params.Body = ($Body | ConvertTo-Json -Depth 12 -Compress)
    }

    try {
        $response = Invoke-RestMethod @params
        return [pscustomobject]@{
            Status = 200
            Body   = $response
            Error  = $null
        }
    }
    catch {
        return [pscustomobject]@{
            Status = (Get-HttpStatusFromException $_.Exception)
            Body   = $null
            Error  = $_.Exception.Message
        }
    }
}

function Assert-Success($Response, [string]$Step) {
    if ($Response.Status -lt 200 -or $Response.Status -ge 300) {
        Fail "$Step returned HTTP $($Response.Status): $($Response.Error)"
    }
}

function Wait-ForUserProfileReadiness {
    param(
        [Parameter(Mandatory = $true)][string]$Token,
        [int]$MaxAttempts = 40,
        [int]$DelayMilliseconds = 250
    )

    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        $profile = Invoke-UmsApi -Method GET -Path "/api/v1/users/me" -Token $Token
        if ($profile.Status -ge 200 -and $profile.Status -lt 300) {
            return
        }

        if ($profile.Status -eq 401 -or $profile.Status -eq 403) {
            Fail "User profile readiness returned terminal HTTP $($profile.Status): $($profile.Error)"
        }

        if ($attempt -lt $MaxAttempts) {
            Start-Sleep -Milliseconds $DelayMilliseconds
        }
    }

    Fail "User profile was not ready after $MaxAttempts attempts"
}

function ConvertFrom-Base32([string]$Value) {
    $alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    $clean = $Value.Trim().ToUpperInvariant().Replace("=", "").Replace(" ", "")
    $bits = New-Object System.Text.StringBuilder

    foreach ($character in $clean.ToCharArray()) {
        $index = $alphabet.IndexOf($character)
        if ($index -lt 0) {
            Fail "TOTP secret contains an invalid Base32 character"
        }
        [void]$bits.Append([Convert]::ToString($index, 2).PadLeft(5, "0"))
    }

    $bitString = $bits.ToString()
    $bytes = New-Object 'System.Collections.Generic.List[byte]'
    for ($offset = 0; $offset + 8 -le $bitString.Length; $offset += 8) {
        $bytes.Add([Convert]::ToByte($bitString.Substring($offset, 8), 2))
    }

    return $bytes.ToArray()
}

function Get-TotpCode {
    param(
        [Parameter(Mandatory = $true)][string]$Secret,
        [int]$Digits = 6,
        [int]$PeriodSeconds = 30
    )

    $key = ConvertFrom-Base32 $Secret
    $epochSeconds = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $counter = [int64][Math]::Floor($epochSeconds / $PeriodSeconds)
    $counterBytes = [BitConverter]::GetBytes($counter)
    if ([BitConverter]::IsLittleEndian) {
        [Array]::Reverse($counterBytes)
    }

    $hmac = New-Object System.Security.Cryptography.HMACSHA1
    try {
        $hmac.Key = $key
        $hash = $hmac.ComputeHash($counterBytes)
    }
    finally {
        $hmac.Dispose()
    }

    $offset = $hash[$hash.Length - 1] -band 0x0F
    $binary = (($hash[$offset] -band 0x7F) -shl 24) -bor
              (($hash[$offset + 1] -band 0xFF) -shl 16) -bor
              (($hash[$offset + 2] -band 0xFF) -shl 8) -bor
              ($hash[$offset + 3] -band 0xFF)

    $modulus = [int][Math]::Pow(10, $Digits)
    $otp = $binary % $modulus
    return $otp.ToString("D$Digits")
}

function Get-ProvisioningNumber {
    param(
        [string]$ProvisioningUri,
        [string]$Name,
        [int]$DefaultValue
    )

    $match = [regex]::Match($ProvisioningUri, "(?:[?&])$Name=(\d+)")
    if ($match.Success) {
        return [int]$match.Groups[1].Value
    }
    return $DefaultValue
}

if (-not $AllowLocalMutation) {
    Fail "Pass -AllowLocalMutation to acknowledge that the script creates disposable local smoke data"
}

try {
    $baseUri = [Uri]$BaseUrl
}
catch {
    Fail "BaseUrl is not a valid URI: $BaseUrl"
}

if (-not $baseUri.IsLoopback) {
    Fail "This destructive smoke is loopback-only. BaseUrl must resolve to localhost/127.0.0.1"
}

$nonce = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$smokeEmail = "issue34-smoke-$nonce@example.com"
$organizationName = "Issue 34 MFA Smoke $nonce"

Write-Host "=== ISSUE #34 ORGANIZATION MFA RUNTIME GATE ==="
Write-Host "BASE_URL=$BaseUrl"
Write-Host "SMOKE_EMAIL=$smokeEmail"
Write-Host "This creates disposable user/organization rows in the local dev databases."

$health = Invoke-UmsApi -Method GET -Path "/actuator/health"
Assert-Success $health "Gateway health"
Write-Host "GATEWAY_HEALTH=PASS"

$registration = Invoke-UmsApi -Method POST -Path "/api/v1/auth/register" -Body @{
    firstName = "Issue34"
    lastName  = "Smoke"
    email     = $smokeEmail
    password  = $Password
    provider  = "LOCAL"
}
Assert-Success $registration "Register smoke user"
$platformToken = [string]$registration.Body.data.accessToken
Assert-True (-not [string]::IsNullOrWhiteSpace($platformToken)) "Registration did not return a platform access token"
Write-Host "REGISTER=PASS"

Wait-ForUserProfileReadiness -Token $platformToken
Write-Host "USER_PROFILE_READY=PASS"

$organization = Invoke-UmsApi -Method POST -Path "/api/v1/organizations" -Token $platformToken -Body @{
    name        = $organizationName
    description = "Disposable Issue #34 organization MFA runtime smoke"
}
Assert-Success $organization "Create organization"
$organizationId = [string]$organization.Body.id
Assert-True (-not [string]::IsNullOrWhiteSpace($organizationId)) "Organization create response did not contain an id"
Write-Host "ORGANIZATION_CREATE=PASS"
Write-Host "ORGANIZATION_ID=$organizationId"

$initialPolicy = Invoke-UmsApi -Method GET -Path "/api/v1/organizations/$organizationId/security-policy" -Token $platformToken
Assert-Success $initialPolicy "Read default organization security policy"
Assert-True (-not [bool]$initialPolicy.Body.requireMfa) "New organization policy should default to requireMfa=false"
Write-Host "POLICY_DEFAULT_OFF=PASS"

$legacyLogin = Invoke-UmsApi -Method POST -Path "/api/v1/auth/login" -Body @{
    email          = $smokeEmail
    password       = $Password
    organizationId = $organizationId
    client         = "ISSUE_34_SMOKE"
    deviceInfo     = "PowerShell runtime smoke"
}
Assert-Success $legacyLogin "Policy-off organization login"
$legacyOrgToken = [string]$legacyLogin.Body.data.accessToken
Assert-True (-not [string]::IsNullOrWhiteSpace($legacyOrgToken)) "Policy-off login did not return an organization access token"
Assert-True (-not [bool]$legacyLogin.Body.data.mfaRequired) "Policy-off login unexpectedly required MFA"
Assert-True (-not [bool]$legacyLogin.Body.data.mfaEnrollmentRequired) "Policy-off login unexpectedly required MFA enrollment"

$legacyOrgRead = Invoke-UmsApi -Method GET -Path "/api/v1/organizations/$organizationId" -Token $legacyOrgToken
Assert-Success $legacyOrgRead "Policy-off organization access"
Write-Host "POLICY_OFF_LEGACY_ACCESS=PASS"

$enablePolicy = Invoke-UmsApi -Method PUT -Path "/api/v1/organizations/$organizationId/security-policy" -Token $platformToken -Body @{
    requireMfa = $true
}
Assert-Success $enablePolicy "Enable organization MFA policy"
Assert-True ([bool]$enablePolicy.Body.requireMfa) "Policy update response did not enable requireMfa"
Write-Host "POLICY_ENABLE=PASS"

$immediateOldToken = Invoke-UmsApi -Method GET -Path "/api/v1/organizations/$organizationId" -Token $legacyOrgToken
Assert-True ($immediateOldToken.Status -eq 401 -or $immediateOldToken.Status -eq 403) "Old non-MFA organization token remained usable immediately after policy enable (HTTP $($immediateOldToken.Status))"
Write-Host "IMMEDIATE_NON_MFA_ACCESS_DENIED=PASS HTTP=$($immediateOldToken.Status)"

$revoked = $false
for ($attempt = 1; $attempt -le 30; $attempt++) {
    $revocationProbe = Invoke-UmsApi -Method GET -Path "/api/v1/organizations/$organizationId" -Token $legacyOrgToken
    if ($revocationProbe.Status -eq 401) {
        $revoked = $true
        break
    }
    Start-Sleep -Seconds 1
}
Assert-True $revoked "Durable policy-change propagation did not revoke the old organization session within 30 seconds"
Write-Host "EXISTING_ORG_SESSION_REVOKED=PASS"

$enrollmentLogin = Invoke-UmsApi -Method POST -Path "/api/v1/auth/login" -Body @{
    email          = $smokeEmail
    password       = $Password
    organizationId = $organizationId
    client         = "ISSUE_34_SMOKE"
    deviceInfo     = "PowerShell runtime smoke"
}
Assert-Success $enrollmentLogin "Policy-on login for MFA-disabled user"
Assert-True ([bool]$enrollmentLogin.Body.data.mfaEnrollmentRequired) "Policy-on MFA-disabled login did not set mfaEnrollmentRequired=true"
Assert-True ([string]$enrollmentLogin.Body.data.requiredOrganizationId -eq $organizationId) "requiredOrganizationId did not match the requested organization"
Assert-True (-not [bool]$enrollmentLogin.Body.data.mfaRequired) "MFA-disabled user should be sent to enrollment, not challenge verification"
$enrollmentToken = [string]$enrollmentLogin.Body.data.accessToken
Assert-True (-not [string]::IsNullOrWhiteSpace($enrollmentToken)) "Enrollment flow did not return a platform-only access token"
Write-Host "PLATFORM_ONLY_ENROLLMENT_SESSION=PASS"

$platformOnlyOrgRead = Invoke-UmsApi -Method GET -Path "/api/v1/organizations/$organizationId" -Token $enrollmentToken
Assert-True ($platformOnlyOrgRead.Status -eq 403) "Platform-only enrollment session should receive 403 for MFA-protected organization access, got HTTP $($platformOnlyOrgRead.Status)"
Write-Host "PLATFORM_ONLY_ORG_ACCESS_DENIED=PASS"

$spoofedOrgRead = Invoke-UmsApi -Method GET -Path "/api/v1/organizations/$organizationId" -Token $enrollmentToken -ExtraHeaders @{
    "X-Authenticated-Organization" = $organizationId
    "X-MFA-Verified"               = "true"
}
Assert-True ($spoofedOrgRead.Status -eq 403) "Spoofed organization/MFA headers bypassed the Gateway boundary (HTTP $($spoofedOrgRead.Status))"
Write-Host "SPOOFED_ASSURANCE_HEADERS_REJECTED=PASS"

$setup = Invoke-UmsApi -Method POST -Path "/api/v1/auth/mfa/totp/setup" -Token $enrollmentToken
Assert-Success $setup "Start TOTP setup"
$secret = [string]$setup.Body.data.secret
$provisioningUri = [string]$setup.Body.data.provisioningUri
Assert-True (-not [string]::IsNullOrWhiteSpace($secret)) "TOTP setup did not return a secret"
Assert-True (-not [string]::IsNullOrWhiteSpace($provisioningUri)) "TOTP setup did not return a provisioning URI"
$digits = Get-ProvisioningNumber -ProvisioningUri $provisioningUri -Name "digits" -DefaultValue 6
$periodSeconds = Get-ProvisioningNumber -ProvisioningUri $provisioningUri -Name "period" -DefaultValue 30

$confirmCode = Get-TotpCode -Secret $secret -Digits $digits -PeriodSeconds $periodSeconds
$confirm = Invoke-UmsApi -Method POST -Path "/api/v1/auth/mfa/totp/confirm" -Token $enrollmentToken -Body @{
    code = $confirmCode
}
Assert-Success $confirm "Confirm TOTP enrollment"
Assert-True ($confirm.Body.data.recoveryCodes.Count -gt 0) "MFA confirmation did not return recovery codes"
Write-Host "MFA_ENROLLMENT=PASS"

$challengeLogin = Invoke-UmsApi -Method POST -Path "/api/v1/auth/login" -Body @{
    email          = $smokeEmail
    password       = $Password
    organizationId = $organizationId
    client         = "ISSUE_34_SMOKE"
    deviceInfo     = "PowerShell runtime smoke"
}
Assert-Success $challengeLogin "Policy-on login after MFA enrollment"
Assert-True ([bool]$challengeLogin.Body.data.mfaRequired) "MFA-enabled organization login did not return an MFA challenge"
Assert-True ([string]::IsNullOrWhiteSpace([string]$challengeLogin.Body.data.accessToken)) "MFA challenge response must not contain an access token before factor verification"
$challengeToken = [string]$challengeLogin.Body.data.mfaChallengeToken
Assert-True (-not [string]::IsNullOrWhiteSpace($challengeToken)) "MFA challenge response did not contain a challenge token"
Write-Host "PRE_FACTOR_SESSION_BLOCK=PASS"

$verifyCode = Get-TotpCode -Secret $secret -Digits $digits -PeriodSeconds $periodSeconds
$verified = Invoke-UmsApi -Method POST -Path "/api/v1/auth/mfa/challenge/verify" -Body @{
    challengeToken = $challengeToken
    totpCode       = $verifyCode
}
Assert-Success $verified "Verify MFA login challenge"
$verifiedOrgToken = [string]$verified.Body.data.accessToken
Assert-True (-not [string]::IsNullOrWhiteSpace($verifiedOrgToken)) "Verified MFA challenge did not return an organization access token"

$verifiedOrgRead = Invoke-UmsApi -Method GET -Path "/api/v1/organizations/$organizationId" -Token $verifiedOrgToken
Assert-Success $verifiedOrgRead "MFA-verified organization access"
Write-Host "MFA_VERIFIED_ORG_ACCESS=PASS"

$disablePolicy = Invoke-UmsApi -Method PUT -Path "/api/v1/organizations/$organizationId/security-policy" -Token $verifiedOrgToken -Body @{
    requireMfa = $false
}
Assert-Success $disablePolicy "Restore organization policy"
Assert-True (-not [bool]$disablePolicy.Body.requireMfa) "Cleanup did not restore requireMfa=false"
Write-Host "POLICY_CLEANUP=PASS"

$disableCode = Get-TotpCode -Secret $secret -Digits $digits -PeriodSeconds $periodSeconds
$disableMfa = Invoke-UmsApi -Method POST -Path "/api/v1/auth/mfa/disable" -Token $verifiedOrgToken -Body @{
    password = $Password
    totpCode = $disableCode
}
Assert-Success $disableMfa "Disable disposable user MFA"
Write-Host "MFA_CLEANUP=PASS"

$revokedAfterDisable = $false
for ($attempt = 1; $attempt -le 10; $attempt++) {
    $probe = Invoke-UmsApi -Method GET -Path "/api/v1/organizations/$organizationId" -Token $verifiedOrgToken
    if ($probe.Status -eq 401) {
        $revokedAfterDisable = $true
        break
    }
    Start-Sleep -Milliseconds 500
}
Assert-True $revokedAfterDisable "MFA disable did not revoke the verified session"
Write-Host "MFA_DISABLE_SESSION_REVOCATION=PASS"

Write-Host ""
Write-Host "ISSUE_34_ORGANIZATION_MFA_RUNTIME_GATE=PASS"
Write-Host "Disposable rows retained for audit evidence:"
Write-Host "  user=$smokeEmail"
Write-Host "  organizationId=$organizationId"
