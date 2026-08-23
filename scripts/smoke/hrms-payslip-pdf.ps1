param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [Parameter(Mandatory = $true)][string]$AccessToken,
    [Parameter(Mandatory = $true)][Guid]$OrganizationId,
    [Parameter(Mandatory = $true)][Guid]$ProcessedRunId,
    [Parameter(Mandatory = $true)][Guid]$EntryId,
    [switch]$AllowLocalMutation
)

$ErrorActionPreference = "Stop"

function Assert-LoopbackBaseUrl {
    param([string]$Url)
    $uri = [Uri]$Url
    if ($uri.Scheme -notin @("http", "https")) {
        throw "BaseUrl must use http or https."
    }
    if ($uri.Host -notin @("127.0.0.1", "localhost", "::1")) {
        throw "Refusing mutation against non-loopback BaseUrl: $Url"
    }
    if (-not $AllowLocalMutation) {
        throw "Pass -AllowLocalMutation to confirm the local payroll run may be finalized."
    }
}

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body,
        [string]$OutFile
    )

    $headers = @{ Authorization = "Bearer $AccessToken" }
    $params = @{
        Method = $Method
        Uri = "$BaseUrl$Path"
        Headers = $headers
        SkipHttpErrorCheck = $true
    }
    if ($null -ne $Body) {
        $params.ContentType = "application/json"
        $params.Body = ($Body | ConvertTo-Json -Compress)
    }
    if ($OutFile) {
        $params.OutFile = $OutFile
        $params.PassThru = $true
    }
    return Invoke-WebRequest @params
}

function Assert-Status {
    param($Response, [int]$Expected, [string]$Label)
    if ([int]$Response.StatusCode -ne $Expected) {
        $body = if ($Response.Content) { $Response.Content } else { "<empty>" }
        throw "$Label expected HTTP $Expected but got $($Response.StatusCode): $body"
    }
    Write-Host "$Label=PASS"
}

Assert-LoopbackBaseUrl -Url $BaseUrl

$org = $OrganizationId.ToString()
$run = $ProcessedRunId.ToString()
$entry = $EntryId.ToString()
$pdfPath = Join-Path ([IO.Path]::GetTempPath()) "ums-payslip-$entry.pdf"
if (Test-Path $pdfPath) { Remove-Item $pdfPath -Force }

try {
    $snapshotBeforeResponse = Invoke-Api -Method GET -Path "/api/v1/hrms/payroll/payslips/$entry?organizationId=$org"
    Assert-Status $snapshotBeforeResponse 200 "JSON_PAYSLIP_BEFORE_FINALIZE"
    $snapshotBefore = $snapshotBeforeResponse.Content | ConvertFrom-Json

    $blockedPdf = Invoke-Api -Method GET -Path "/api/v1/hrms/payroll/payslips/$entry/pdf?organizationId=$org"
    Assert-Status $blockedPdf 409 "PDF_BLOCKED_BEFORE_FINALIZE"

    $finalize = Invoke-Api -Method POST -Path "/api/v1/hrms/payroll/runs/$run/finalize" -Body @{ organizationId = $org }
    Assert-Status $finalize 200 "PAYROLL_FINALIZE"
    $finalizedRun = $finalize.Content | ConvertFrom-Json
    if ($finalizedRun.status -ne "FINALIZED" -or -not $finalizedRun.finalizedAt) {
        throw "Finalization response did not contain FINALIZED status/finalizedAt."
    }
    Write-Host "FINALIZED_RUN_STATE=PASS"

    $pdf = Invoke-Api -Method GET -Path "/api/v1/hrms/payroll/payslips/$entry/pdf?organizationId=$org" -OutFile $pdfPath
    Assert-Status $pdf 200 "PDF_DOWNLOAD_AFTER_FINALIZE"

    $contentType = [string]$pdf.Headers["Content-Type"]
    if ($contentType -notmatch "application/pdf") {
        throw "Expected application/pdf but got '$contentType'."
    }
    Write-Host "PDF_CONTENT_TYPE=PASS"

    $disposition = [string]$pdf.Headers["Content-Disposition"]
    if ($disposition -notmatch "(?i)attachment" -or $disposition -notmatch "(?i)\.pdf") {
        throw "Expected attachment PDF Content-Disposition but got '$disposition'."
    }
    Write-Host "PDF_ATTACHMENT_FILENAME=PASS"

    if (-not (Test-Path $pdfPath) -or (Get-Item $pdfPath).Length -le 4) {
        throw "Downloaded PDF body is empty."
    }
    $prefix = [Text.Encoding]::ASCII.GetString([IO.File]::ReadAllBytes($pdfPath), 0, 4)
    if ($prefix -ne "%PDF") {
        throw "Downloaded body does not start with %PDF."
    }
    Write-Host "PDF_BODY_SIGNATURE=PASS"

    $snapshotAfterResponse = Invoke-Api -Method GET -Path "/api/v1/hrms/payroll/payslips/$entry?organizationId=$org"
    Assert-Status $snapshotAfterResponse 200 "JSON_PAYSLIP_AFTER_FINALIZE"
    $snapshotAfter = $snapshotAfterResponse.Content | ConvertFrom-Json

    foreach ($field in @("basicPay", "allowanceTotal", "grossPay", "deductionTotal", "netPay", "generatedAt")) {
        if ([string]$snapshotBefore.$field -ne [string]$snapshotAfter.$field) {
            throw "Persisted payslip snapshot field '$field' changed across finalization."
        }
    }
    Write-Host "JSON_SNAPSHOT_STABILITY=PASS"

    Write-Host "ISSUE_38_PAYSLIP_PDF_RUNTIME_GATE=PASS"
}
finally {
    if (Test-Path $pdfPath) {
        Remove-Item $pdfPath -Force
    }
}
