param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [Parameter(Mandatory = $true)][string]$AccessToken,
    [Parameter(Mandatory = $true)][Guid]$OrganizationId,
    [Parameter(Mandatory = $true)][Guid]$ProcessedRunId,
    [Parameter(Mandatory = $true)][Guid]$EntryId,
    [switch]$AllowLocalMutation
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
Add-Type -AssemblyName System.Net.Http

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
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        [object]$Body = $null
    )

    $httpMethod = [System.Net.Http.HttpMethod]::new($Method)
    $request = [System.Net.Http.HttpRequestMessage]::new($httpMethod, "$BaseUrl$Path")
    $request.Headers.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $AccessToken)
    $request.Headers.Accept.Add([System.Net.Http.Headers.MediaTypeWithQualityHeaderValue]::new("application/json"))
    $request.Headers.Accept.Add([System.Net.Http.Headers.MediaTypeWithQualityHeaderValue]::new("application/pdf"))

    if ($null -ne $Body) {
        $json = $Body | ConvertTo-Json -Depth 10 -Compress
        $request.Content = [System.Net.Http.StringContent]::new(
            $json,
            [Text.Encoding]::UTF8,
            "application/json"
        )
    }

    try {
        $response = $script:HttpClient.SendAsync($request).GetAwaiter().GetResult()
        try {
            $bytes = $response.Content.ReadAsByteArrayAsync().GetAwaiter().GetResult()
            $text = [Text.Encoding]::UTF8.GetString($bytes)
            return [pscustomobject]@{
                Status = [int]$response.StatusCode
                Bytes = $bytes
                Text = $text
                ContentType = if ($null -ne $response.Content.Headers.ContentType) {
                    [string]$response.Content.Headers.ContentType.MediaType
                } else { "" }
                ContentDisposition = if ($null -ne $response.Content.Headers.ContentDisposition) {
                    [string]$response.Content.Headers.ContentDisposition
                } else { "" }
            }
        }
        finally {
            $response.Dispose()
        }
    }
    finally {
        $request.Dispose()
    }
}

function Assert-Status {
    param($Response, [int]$Expected, [string]$Label)
    if ($Response.Status -ne $Expected) {
        $body = if ($Response.Text) { $Response.Text } else { "<empty>" }
        throw "$Label expected HTTP $Expected but got $($Response.Status): $body"
    }
    Write-Host "$Label=PASS"
}

Assert-LoopbackBaseUrl -Url $BaseUrl

$org = $OrganizationId.ToString()
$run = $ProcessedRunId.ToString()
$entry = $EntryId.ToString()
$script:HttpClient = [System.Net.Http.HttpClient]::new()
$script:HttpClient.Timeout = [TimeSpan]::FromSeconds(30)

try {
    $snapshotBeforeResponse = Invoke-Api -Method GET -Path "/api/v1/hrms/payroll/payslips/${entry}?organizationId=${org}"
    Assert-Status $snapshotBeforeResponse 200 "JSON_PAYSLIP_BEFORE_FINALIZE"
    $snapshotBefore = $snapshotBeforeResponse.Text | ConvertFrom-Json

    $blockedPdf = Invoke-Api -Method GET -Path "/api/v1/hrms/payroll/payslips/$entry/pdf?organizationId=$org"
    Assert-Status $blockedPdf 409 "PDF_BLOCKED_BEFORE_FINALIZE"

    $finalize = Invoke-Api -Method POST -Path "/api/v1/hrms/payroll/runs/$run/finalize" -Body @{ organizationId = $org }
    Assert-Status $finalize 200 "PAYROLL_FINALIZE"
    $finalizedRun = $finalize.Text | ConvertFrom-Json
    if ($finalizedRun.status -ne "FINALIZED" -or -not $finalizedRun.finalizedAt) {
        throw "Finalization response did not contain FINALIZED status/finalizedAt."
    }
    Write-Host "FINALIZED_RUN_STATE=PASS"

    $pdf = Invoke-Api -Method GET -Path "/api/v1/hrms/payroll/payslips/$entry/pdf?organizationId=$org"
    Assert-Status $pdf 200 "PDF_DOWNLOAD_AFTER_FINALIZE"

    if ($pdf.ContentType -ne "application/pdf") {
        throw "Expected application/pdf but got '$($pdf.ContentType)'."
    }
    Write-Host "PDF_CONTENT_TYPE=PASS"

    if ($pdf.ContentDisposition -notmatch "(?i)attachment" -or $pdf.ContentDisposition -notmatch "(?i)\.pdf") {
        throw "Expected attachment PDF Content-Disposition but got '$($pdf.ContentDisposition)'."
    }
    Write-Host "PDF_ATTACHMENT_FILENAME=PASS"

    if ($pdf.Bytes.Length -le 4) {
        throw "Downloaded PDF body is empty."
    }
    $prefix = [Text.Encoding]::ASCII.GetString($pdf.Bytes, 0, 4)
    if ($prefix -ne "%PDF") {
        throw "Downloaded body does not start with %PDF."
    }
    Write-Host "PDF_BODY_SIGNATURE=PASS"

    $snapshotAfterResponse = Invoke-Api -Method GET -Path "/api/v1/hrms/payroll/payslips/${entry}?organizationId=${org}"
    Assert-Status $snapshotAfterResponse 200 "JSON_PAYSLIP_AFTER_FINALIZE"
    $snapshotAfter = $snapshotAfterResponse.Text | ConvertFrom-Json

    foreach ($field in @("basicPay", "allowanceTotal", "grossPay", "deductionTotal", "netPay", "generatedAt")) {
        if ([string]$snapshotBefore.$field -ne [string]$snapshotAfter.$field) {
            throw "Persisted payslip snapshot field '$field' changed across finalization."
        }
    }
    Write-Host "JSON_SNAPSHOT_STABILITY=PASS"

    Write-Host "ISSUE_38_PAYSLIP_PDF_RUNTIME_GATE=PASS"
}
finally {
    if ($null -ne $script:HttpClient) {
        $script:HttpClient.Dispose()
    }
}
