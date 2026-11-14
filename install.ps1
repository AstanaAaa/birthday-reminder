# 安装到手机 + 检查崩溃
#
# 用法：
#   .\install.ps1              装最新的 apk 并启动
#   .\install.ps1 -ShowData    顺便打印 App 里保存的好友数据
#   .\install.ps1 -Log         装完实时看日志（Ctrl+C 退出）

param(
    [switch]$ShowData,
    [switch]$Log
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot

# 找 adb：优先 PATH，其次 ANDROID_HOME，最后本机便携路径
$adb = (Get-Command adb -ErrorAction SilentlyContinue)?.Source
if (-not $adb) {
    $sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { 'D:\XD\tools\android-sdk' }
    $candidate = Join-Path $sdk 'platform-tools\adb.exe'
    if (Test-Path $candidate) { $adb = $candidate }
}
if (-not $adb) {
    Write-Host '找不到 adb。请安装 Android platform-tools 或设置 ANDROID_HOME。' -ForegroundColor Red
    exit 1
}

# adb 需要在临时目录写日志，系统目录不可写时改用工作区目录
if (Test-Path 'D:\XD\tools\tmp') {
    $env:TMP = 'D:\XD\tools\tmp'
    $env:TEMP = $env:TMP
}

$apk = Join-Path $root 'app\build\outputs\apk\debug\app-debug.apk'
$pkg = 'com.xd.birthdayreminder'

if (-not (Test-Path $apk)) {
    Write-Host "找不到 APK：$apk" -ForegroundColor Red
    Write-Host '先运行 .\build.ps1 编译。' -ForegroundColor Yellow
    exit 1
}

Write-Host '正在检测手机...' -ForegroundColor Cyan
& $adb start-server | Out-Null
$devices = & $adb devices | Select-String -Pattern '\sdevice$'

if (-not $devices) {
    Write-Host ''
    Write-Host '没检测到手机。请依次确认：' -ForegroundColor Yellow
    Write-Host '  1. 数据线连的是「传输文件」模式，不是纯充电'
    Write-Host '  2. 手机已开启「开发者选项 → USB 调试」'
    Write-Host '  3. 手机上弹出的「允许 USB 调试吗」点了允许'
    Write-Host '  4. 小米/红米还要在开发者选项里打开「USB 安装」'
    Write-Host '  5. 华为/荣耀：打开「仅充电时允许 ADB 调试」'
    exit 1
}

Write-Host '正在安装...' -ForegroundColor Cyan
& $adb install -r $apk
if ($LASTEXITCODE -ne 0) {
    Write-Host '安装失败。如果提示 INSTALL_FAILED_UPDATE_INCOMPATIBLE，先卸载手机上的旧版本。' -ForegroundColor Red
    exit 1
}

Write-Host '启动 App...' -ForegroundColor Cyan
& $adb shell am start -n "$pkg/.MainActivity" | Out-Null
Start-Sleep -Seconds 2

if ($ShowData) {
    Write-Host ''
    Write-Host '==== App 里保存的好友数据 ====' -ForegroundColor Cyan
    & $adb shell run-as $pkg cat files/friends.json 2>&1
}

Write-Host ''
Write-Host '==== 崩溃检查 ====' -ForegroundColor Cyan
$crash = & $adb logcat -d -t 300 2>&1 | Select-String -Pattern 'FATAL EXCEPTION|AndroidRuntime.*birthdayreminder'
if ($crash) { $crash } else { Write-Host '没有发现崩溃记录。' -ForegroundColor Green }

if ($Log) {
    Write-Host ''
    Write-Host '==== 实时日志（Ctrl+C 退出）====' -ForegroundColor Cyan
    & $adb logcat -v color
}
