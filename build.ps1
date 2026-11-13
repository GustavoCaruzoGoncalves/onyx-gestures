#Requires -Version 5.1
param(
    [switch]$SkipTests,
    [switch]$DebugApk
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Sdk = $env:ANDROID_HOME
if (-not $Sdk) { $Sdk = Join-Path $env:LOCALAPPDATA "Android\Sdk" }
$env:ANDROID_HOME = $Sdk
$env:ANDROID_SDK_ROOT = $Sdk

$prop = Get-Content (Join-Path $Root "module\module.prop")
$verLine = $prop | Where-Object { $_ -match '^version=' } | Select-Object -First 1
$Version = if ($verLine) { ($verLine -replace '^version=', '').Trim() } else { "v0.0.0" }
if ($Version -notmatch '^v') { $Version = "v$Version" }
$codeLine = $prop | Where-Object { $_ -match '^versionCode=' } | Select-Object -First 1
$VersionCode = if ($codeLine) { ($codeLine -replace '^versionCode=', '').Trim() } else { "0" }
# Placeholder update.json (sha256 filled after ZIP is packed).
$updatePath = Join-Path $Root "module\update.json"
$zipUrl = "https://github.com/GustavoCaruzoGoncalves/onyx-gestures/releases/download/$Version/f7_moto_actions-$Version.zip"
$changelogUrl = "https://raw.githubusercontent.com/GustavoCaruzoGoncalves/onyx-gestures/main/CHANGELOG.md"
function Write-UpdateJson([string]$Sha) {
    $json = @"
{
  "version": "$Version",
  "versionCode": $VersionCode,
  "zipUrl": "$zipUrl",
  "changelog": "$changelogUrl",
  "sha256": "$Sha"
}
"@
    [System.IO.File]::WriteAllText($updatePath, $json.Trim() + "`n")
}
Write-UpdateJson ""
Write-Host "Wrote module/update.json for $Version ($VersionCode) (sha256 pending)"

Push-Location $Root
try {
    $tasks = @(":app:assembleRelease")
    if (-not $SkipTests) { $tasks = @(":app:testDebugUnitTest") + $tasks }
    if ($DebugApk) { $tasks += ":app:assembleDebug" }

    & .\gradlew.bat @tasks --stacktrace
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed: $LASTEXITCODE" }

    $candidate = @(
        (Join-Path $Root "app\build\outputs\apk\release\app-release.apk"),
        (Join-Path $Root "app\build\outputs\apk\release\app-release-unsigned.apk")
    ) | Where-Object { Test-Path $_ } | Select-Object -First 1

    if (-not $candidate) { throw "Release APK not found" }

    $outApk = Join-Path $Root "module\system\priv-app\F7MotoActions\F7MotoActions.apk"
    New-Item -ItemType Directory -Force -Path (Split-Path $outApk) | Out-Null

    if ($candidate -like "*unsigned*") {
        $ks = Join-Path $Root "tools\module.keystore"
        if (-not (Test-Path $ks)) {
            New-Item -ItemType Directory -Force -Path (Join-Path $Root "tools") | Out-Null
            keytool -genkeypair -v -keystore $ks -alias f7moto -keyalg RSA -keysize 2048 `
                -validity 10000 -storepass f7motoactions -keypass f7motoactions `
                -dname "CN=Onyx Gestures,O=GustavoCaruzo,C=BR"
        }
        $bt = Get-ChildItem (Join-Path $Sdk "build-tools") | Sort-Object Name -Descending | Select-Object -First 1
        $apksigner = Join-Path $bt.FullName "apksigner.bat"
        $zipalign = Join-Path $bt.FullName "zipalign.exe"
        $aligned = Join-Path $Root "app\build\outputs\apk\release\app-release-aligned.apk"
        & $zipalign -f -p 4 $candidate $aligned
        & $apksigner sign --ks $ks --ks-pass pass:f7motoactions --key-pass pass:f7motoactions `
            --out $outApk $aligned
    } else {
        Copy-Item $candidate $outApk -Force
    }

    $dist = Join-Path $Root "dist"
    New-Item -ItemType Directory -Force -Path $dist | Out-Null
    $stage = Join-Path $env:TEMP "f7_moto_actions_stage"
    if (Test-Path $stage) { Remove-Item $stage -Recurse -Force }
    New-Item -ItemType Directory -Force -Path $stage | Out-Null
    Copy-Item (Join-Path $Root "module\*") $stage -Recurse -Force

    $upd = Join-Path $stage "META-INF\com\google\android\update-binary"
    $updDir = Split-Path $upd
    New-Item -ItemType Directory -Force -Path $updDir | Out-Null
    @'
#!/sbin/sh
# Magisk/KernelSU compatible installer (busybox ash)
OUTFD=$2
ZIPFILE=$3
ui_print() { echo "ui_print $1" > /proc/self/fd/$OUTFD; echo "ui_print" > /proc/self/fd/$OUTFD; }
ui_print "- Onyx Gestures"
if [ -f /data/adb/magisk/util_functions.sh ]; then
  . /data/adb/magisk/util_functions.sh
  install_module
  exit 0
fi
MODDIR=/data/adb/modules_update/f7_moto_actions
mkdir -p "$MODDIR"
unzip -o "$ZIPFILE" -x 'META-INF/*' -d "$MODDIR"
ui_print "- Installed to $MODDIR (reboot)"
exit 0
'@ | Set-Content -Path $upd -Encoding ascii
    "*************************" | Set-Content (Join-Path $stage "META-INF\com\google\android\updater-script") -Encoding ascii

    $zipPath = Join-Path $dist "f7_moto_actions-$Version.zip"
    if (Test-Path $zipPath) { Remove-Item $zipPath -Force }
    $packPy = @"
import zipfile
from pathlib import Path
mod = Path(r'$stage')
out = Path(r'$zipPath')
with zipfile.ZipFile(out, 'w', compression=zipfile.ZIP_DEFLATED) as zf:
    for path in sorted(mod.rglob('*')):
        if not path.is_file():
            continue
        rel = path.relative_to(mod).as_posix()
        if rel.startswith('META-INF'):
            continue
        zf.write(path, rel)
print(out)
"@
    $packPyPath = Join-Path $env:TEMP "pack_f7_moto_zip.py"
    Set-Content -Path $packPyPath -Value $packPy -Encoding UTF8
    python $packPyPath
    $hash = (Get-FileHash -Algorithm SHA256 -Path $zipPath).Hash.ToLowerInvariant()
    Write-UpdateJson $hash
    # Rewrite update.json inside the ZIP so the packaged copy matches the hosted file.
    $rewritePy = @"
import zipfile
from pathlib import Path
zp = Path(r'$zipPath')
uj = Path(r'$updatePath').read_bytes()
tmp = zp.with_suffix('.zip.tmp')
with zipfile.ZipFile(zp, 'r') as zin, zipfile.ZipFile(tmp, 'w', compression=zipfile.ZIP_DEFLATED) as zout:
    for info in zin.infolist():
        data = uj if info.filename == 'update.json' else zin.read(info.filename)
        zout.writestr(info, data)
tmp.replace(zp)
print('rewrote update.json in', zp)
"@
    $rewritePyPath = Join-Path $env:TEMP "rewrite_onyx_update_json.py"
    Set-Content -Path $rewritePyPath -Value $rewritePy -Encoding UTF8
    python $rewritePyPath
    # Re-hash after rewrite (content changed).
    $hash = (Get-FileHash -Algorithm SHA256 -Path $zipPath).Hash.ToLowerInvariant()
    Write-UpdateJson $hash
    # Second rewrite so ZIP sha256 field matches final hash (fixed-point: update.json sha field changes hash).
    # Hosted update.json is authoritative for KernelSU / in-app; keep ZIP copy best-effort without looping.
    python $rewritePyPath
    $hashFile = "$zipPath.sha256"
    Set-Content -Path $hashFile -Value "$hash  f7_moto_actions-$Version.zip" -Encoding ascii
    Write-Host "APK -> $outApk"
    Write-Host "ZIP -> $zipPath"
    Write-Host "SHA256 -> $hash (hosted update.json; ZIP copy may differ by one hash iteration)"
}
finally {
    Pop-Location
}
