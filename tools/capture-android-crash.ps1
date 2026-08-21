$ErrorActionPreference = 'Stop'

$adbCommand = Get-Command adb -ErrorAction SilentlyContinue
if ($null -eq $adbCommand) {
    Write-Error 'ADB was not found. Install Android platform-tools and add adb to PATH.'
}

$workspaceRoot = Split-Path -Parent $PSScriptRoot
$packageName = 'com.companion.cc.debug'
$outputDir = Join-Path $workspaceRoot 'build'
$outputFile = Join-Path $outputDir 'device-crash.log'

$deviceLines = @(adb devices | Select-String '\t(device|unauthorized|offline)$')
$readyDevices = @($deviceLines | Where-Object { $_.Line -match '\tdevice$' })
if ($readyDevices.Count -eq 0) {
    if ($deviceLines.Count -gt 0) {
        Write-Error 'No authorized Android device is ready. Unlock the device and accept the USB debugging prompt.'
    }
    Write-Error 'No Android device is connected. Enable USB debugging and reconnect the device.'
}

New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
adb logcat -c
adb shell am force-stop $packageName
adb shell monkey -p $packageName 1 | Out-Null
Write-Host "Reproduce the crash now, then press Enter to capture the filtered log."
[void](Read-Host)

adb logcat -d -v threadtime |
    Select-String 'FATAL EXCEPTION|AndroidRuntime|CCNavigation|NavController' |
    Set-Content -Encoding UTF8 $outputFile

Write-Host "Saved filtered crash log to $outputFile"
