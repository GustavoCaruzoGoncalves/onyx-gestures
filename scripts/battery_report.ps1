#Requires -Version 5.1
<#
.SYNOPSIS
  Captures OS-attributed battery usage for org.f7.motoactions via ADB.

.EXAMPLE
  .\battery_report.ps1
  .\battery_report.ps1 -ResetStats
  .\battery_report.ps1 -WatchMinutes 30
#>
param(
    [string]$Adb = "C:\Users\Administrator\Downloads\platform-tools-latest-windows\platform-tools\adb.exe",
    [string]$Package = "org.f7.motoactions",
    [switch]$ResetStats,
    [int]$WatchMinutes = 0,
    [string]$OutDir = ""
)

$ErrorActionPreference = "Stop"
if (-not (Test-Path $Adb)) { throw "adb not found: $Adb" }
if (-not $OutDir) {
    $OutDir = Join-Path (Split-Path $PSScriptRoot -Parent) "dist\battery"
}
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

function Invoke-Adb([string[]]$Args) {
    & $Adb @Args
}

function Save-Report([string]$tag) {
    $ts = Get-Date -Format "yyyyMMdd-HHmmss"
    $file = Join-Path $OutDir "batterystats_$tag_$ts.txt"
    $uid = $null
    $pkgDump = Invoke-Adb @("shell", "dumpsys", "package", $Package) 2>$null | Out-String
    $m = [regex]::Match($pkgDump, "userId=(\d+)")
    if ($m.Success) { $uid = $m.Groups[1].Value }

    $sb = New-Object System.Text.StringBuilder
    [void]$sb.AppendLine("=== Onyx Gestures battery report ===")
    [void]$sb.AppendLine("time=$(Get-Date -Format o) package=$Package uid=$uid")
    [void]$sb.AppendLine("")
    [void]$sb.AppendLine("--- dumpsys batterystats $Package ---")
    [void]$sb.AppendLine(((Invoke-Adb @("shell", "dumpsys", "batterystats", $Package) 2>$null | Out-String)))
    [void]$sb.AppendLine("")
    [void]$sb.AppendLine("--- wakelocks (grep) ---")
    [void]$sb.AppendLine(((Invoke-Adb @("shell", "dumpsys", "power") 2>$null | Select-String -Pattern "F7MotoActions|org.f7.motoactions" | Out-String)))
    [void]$sb.AppendLine("")
    [void]$sb.AppendLine("--- sensorservice listeners ---")
    [void]$sb.AppendLine(((Invoke-Adb @("shell", "dumpsys", "sensorservice") 2>$null | Select-String -Pattern "org.f7.motoactions" -Context 0,2 | Out-String)))
    [void]$sb.AppendLine("")
    [void]$sb.AppendLine("--- deviceidle whitelist ---")
    [void]$sb.AppendLine(((Invoke-Adb @("shell", "dumpsys", "deviceidle", "whitelist") 2>$null | Select-String $Package | Out-String)))

    Set-Content -Path $file -Value $sb.ToString() -Encoding UTF8
    Write-Host "Wrote $file"
    return $file
}

if ($ResetStats) {
    Write-Host "Resetting batterystats (requires root or dump permission)..."
    Invoke-Adb @("shell", "dumpsys", "batterystats", "--reset") | Out-Host
    Write-Host "Use the phone normally / leave screen-off gestures on, then re-run without -ResetStats."
}

$first = Save-Report "baseline"

if ($WatchMinutes -gt 0) {
    Write-Host "Watching $WatchMinutes minute(s)..."
    Start-Sleep -Seconds ($WatchMinutes * 60)
    Save-Report "after_${WatchMinutes}m" | Out-Null
    Write-Host "Compare baseline vs after_* files in $OutDir"
    Write-Host "Look for: Cpu, Wake lock, Sensor, Computed drain, Uid u0aXXX"
}

Write-Host "Tip: Settings > Battery > App usage also shows system attribution for $Package"
Write-Host "Baseline: $first"
