[CmdletBinding()]
param(
    [string] $TmnCode = $env:VNPAY_TMN_CODE,
    [string] $HashSecret = $env:VNPAY_HASH_SECRET,
    [int] $Port = 8081
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$projectRoot = Split-Path -Parent $PSScriptRoot
$toolDirectory = Join-Path $projectRoot ".tools"
$cloudflaredPath = Join-Path $toolDirectory "cloudflared.exe"
$tunnelOutputPath = Join-Path $toolDirectory "cloudflared.out.log"
$tunnelErrorPath = Join-Path $toolDirectory "cloudflared.err.log"

if ([string]::IsNullOrWhiteSpace($TmnCode)) {
    $TmnCode = Read-Host "Nhap VNPAY Terminal ID (vnp_TmnCode)"
}

if ([string]::IsNullOrWhiteSpace($HashSecret)) {
    $secureSecret = Read-Host "Nhap VNPAY Hash Secret (khong hien tren man hinh)" -AsSecureString
    $secretPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureSecret)
    try {
        $HashSecret = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($secretPointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($secretPointer)
    }
}

if ([string]::IsNullOrWhiteSpace($TmnCode) -or [string]::IsNullOrWhiteSpace($HashSecret)) {
    throw "Thieu VNPAY Terminal ID hoac Hash Secret."
}

New-Item -ItemType Directory -Path $toolDirectory -Force | Out-Null
if (-not (Test-Path -LiteralPath $cloudflaredPath)) {
    Write-Host "Dang tai Cloudflare Tunnel lan dau..."
    Invoke-WebRequest `
        -Uri "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe" `
        -OutFile $cloudflaredPath
}

Remove-Item -LiteralPath $tunnelOutputPath, $tunnelErrorPath -Force -ErrorAction SilentlyContinue
$tunnel = Start-Process `
    -FilePath $cloudflaredPath `
    -ArgumentList @("tunnel", "--url", "http://127.0.0.1:$Port", "--no-autoupdate") `
    -RedirectStandardOutput $tunnelOutputPath `
    -RedirectStandardError $tunnelErrorPath `
    -PassThru `
    -WindowStyle Hidden

try {
    $publicUrl = $null
    for ($attempt = 0; $attempt -lt 30 -and [string]::IsNullOrWhiteSpace($publicUrl); $attempt += 1) {
        Start-Sleep -Seconds 1
        if ($tunnel.HasExited) {
            throw "Cloudflare Tunnel da dung. Xem log tai $tunnelErrorPath"
        }

        $logText = Get-Content -LiteralPath $tunnelErrorPath -Raw -ErrorAction SilentlyContinue
        if ($logText -match "https://[a-z0-9-]+\.trycloudflare\.com") {
            $publicUrl = $Matches[0]
        }
    }

    if ([string]::IsNullOrWhiteSpace($publicUrl)) {
        throw "Khong lay duoc URL public tu Cloudflare Tunnel. Xem log tai $tunnelErrorPath"
    }

    $env:VNPAY_TMN_CODE = $TmnCode
    $env:VNPAY_HASH_SECRET = $HashSecret
    $env:VNPAY_RETURN_URL = "$publicUrl/api/payments/vnpay/return"

    Write-Host ""
    Write-Host "VNPAY public return URL: $env:VNPAY_RETURN_URL"
    Write-Host "VNPAY IPN URL neu sandbox cho cau hinh: $publicUrl/api/payments/vnpay/ipn"
    Write-Host "Hay tao QR moi sau khi admin-web khoi dong; QR cu van chua URL local."
    Write-Host ""

    Push-Location $projectRoot
    try {
        & .\gradlew.bat :admin-web:bootRun
    } finally {
        Pop-Location
    }
} finally {
    if ($null -ne $tunnel -and -not $tunnel.HasExited) {
        Stop-Process -Id $tunnel.Id -Force
    }
}
