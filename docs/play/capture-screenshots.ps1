# Pull a Play-sized screenshot from the first adb device into docs/play/screenshots.
$ErrorActionPreference = "Stop"
$outDir = Join-Path $PSScriptRoot "screenshots"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$adb = Get-Command adb -ErrorAction SilentlyContinue
if (-not $adb) {
    throw "adb not on PATH. Install platform-tools or open a device from Android Studio first."
}
$devices = & adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "device$" }
if (-not $devices) { throw "No adb device/emulator. Start one, then retry." }
$n = 1
while (Test-Path (Join-Path $outDir ("{0:D2}-capture.png" -f $n))) { $n++ }
$dest = Join-Path $outDir ("{0:D2}-capture.png" -f $n)
$remote = "/sdcard/route-planner-shot.png"
& adb shell screencap -p $remote
if ($LASTEXITCODE -ne 0) { throw "screencap failed" }
& adb pull $remote $dest
& adb shell rm $remote
Write-Host "Wrote $dest — rename to 01-home.png / 02-driving.png / 03-about.png before upload."
