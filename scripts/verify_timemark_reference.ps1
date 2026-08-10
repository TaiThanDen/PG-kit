[CmdletBinding()]
param(
    [string]$PackageName = 'com.oceangalaxy.camera.new',
    [string]$ApkPath = 'reference_app/base.apk',
    [string]$OutputDir = 'docs/timemark_reference/evidence/latest',
    [switch]$IncludeDex
)

$ErrorActionPreference = 'Stop'
$workspace = Split-Path -Parent $PSScriptRoot
$apk = Join-Path $workspace $ApkPath
$out = Join-Path $workspace $OutputDir

if (!(Test-Path -LiteralPath $apk)) { throw "Không tìm thấy APK: $apk" }
if (!(Get-Command adb -ErrorAction SilentlyContinue)) { throw 'Không tìm thấy adb trong PATH.' }

& adb start-server | Out-Null
& adb wait-for-device
if ((& adb get-state).Trim() -ne 'device') {
    throw 'ADB không có thiết bị ở trạng thái device. Mở khóa điện thoại và chấp nhận USB debugging rồi chạy lại.'
}

New-Item -ItemType Directory -Force -Path $out | Out-Null

function Save-Text([string]$Name, [scriptblock]$Action) {
    $path = Join-Path $out $Name
    try { & $Action | Out-File -FilePath $path -Encoding utf8 }
    catch { "ERROR: $($_.Exception.Message)" | Out-File -FilePath $path -Encoding utf8; throw }
}

Save-Text '01-adb-devices.txt' { adb devices }
Save-Text '02-package.txt' {
    adb shell dumpsys package $PackageName |
        Select-String -Pattern 'versionName=|versionCode=|minSdk=|targetSdk=|codePath=|uses-permission:'
}
Save-Text '03-apk-paths.txt' { adb shell pm path $PackageName }

$sdkRoot = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { 'E:\Android\Sdk' }
$aapt2 = Get-ChildItem (Join-Path $sdkRoot 'build-tools') -Directory -ErrorAction Stop |
    Sort-Object Name -Descending |
    ForEach-Object { Join-Path $_.FullName 'aapt2.exe' } |
    Where-Object { Test-Path $_ } |
    Select-Object -First 1
if (!$aapt2) { throw 'Không tìm thấy aapt2.exe. Đặt ANDROID_HOME hoặc cài Android SDK Build Tools.' }

Save-Text '04-aapt-badging.txt' { & $aapt2 dump badging $apk }
Save-Text '05-apk-font-and-watermark-entries.txt' {
    jar tf $apk | Select-String -Pattern '^assets/fonts/global/|^res/font/|^assets/watermark/|watermark_preview|camera_preview|video_action|location'
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($apk)
try {
    $entry = $zip.Entries | Where-Object FullName -eq 'assets/watermark/default_watermarks_global.json'
    if (!$entry) { throw 'Không có default_watermarks_global.json trong APK.' }
    $reader = [System.IO.StreamReader]::new($entry.Open())
    try {
        $json = $reader.ReadToEnd() | ConvertFrom-Json
        $template = $json | Where-Object name -eq 'Time&Location' | Select-Object -First 1
        if (!$template) { throw 'Không tìm thấy template Time&Location.' }
        $template | ConvertTo-Json -Depth 20 | Out-File (Join-Path $out '06-time-location-template.json') -Encoding utf8
    } finally { $reader.Dispose() }
} finally { $zip.Dispose() }

if ($IncludeDex) {
    $dex = Join-Path $workspace 'reference_app/dex/classes2.dex'
    $dexdump = Join-Path (Split-Path $aapt2 -Parent) 'dexdump.exe'
    if (!(Test-Path $dex)) { throw "Thiếu DEX đã extract: $dex" }
    if (!(Test-Path $dexdump)) { throw "Không tìm thấy dexdump: $dexdump" }
    Save-Text '07-dex-camerax-snippets.txt' {
        & $dexdump -d -n $dex 2>$null |
            Select-String -Pattern 'SampleCameraActivity|ProcessCameraProvider|PreviewView;.getSurfaceProvider|ImageCapture\$Builder;.setTargetAspectRatio' -Context 4,8 |
            Select-Object -First 120
    }
}

@"
Generated: $(Get-Date -Format o)
Package: $PackageName
APK: $apk
SHA256: $((Get-FileHash $apk -Algorithm SHA256).Hash)
DEX inspected: $IncludeDex
"@ | Out-File (Join-Path $out '00-manifest.txt') -Encoding utf8

Write-Host "Evidence written to: $out"
