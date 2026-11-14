# 一键编译脚本（Windows）
#
# 用法：
#   .\build.ps1             编译
#   .\build.ps1 -Install    编译并安装到已连接的手机
#
# 环境变量优先级说明：
#   如果你已经设置了 JAVA_HOME / ANDROID_HOME，脚本直接用你的。
#   没设置时才会回退到下面这些"本机便携环境"路径。

param([switch]$Install)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot

# ---- 1. JDK 17 ----
if (-not $env:JAVA_HOME) {
    $fallback = 'D:\XD\tools\jdk17'
    if (Test-Path $fallback) {
        $env:JAVA_HOME = $fallback
    } else {
        Write-Host '找不到 JDK。请安装 JDK 17 并设置 JAVA_HOME 环境变量。' -ForegroundColor Red
        exit 1
    }
}
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Write-Host "JDK: $env:JAVA_HOME"

# ---- 2. Android SDK ----
if (-not $env:ANDROID_HOME) {
    $fallback = 'D:\XD\tools\android-sdk'
    if (Test-Path $fallback) { $env:ANDROID_HOME = $fallback }
}
if ($env:ANDROID_HOME) { Write-Host "SDK: $env:ANDROID_HOME" }

# ---- 3. 部分机器系统临时目录不可写，改用工作区目录 ----
$localTmp = 'D:\XD\tools\tmp'
if (Test-Path $localTmp) {
    $env:TMP = $localTmp
    $env:TEMP = $localTmp
}

# Gradle 默认把依赖缓存放用户目录，这里不可写时改用工作区目录
$localGradleHome = 'D:\XD\tools\gradle-home'
if (-not $env:GRADLE_USER_HOME -and (Test-Path $localGradleHome)) {
    $env:GRADLE_USER_HOME = $localGradleHome
}

# ---- 4. 编译：优先用项目自带的 wrapper ----
$gradlew = Join-Path $root 'gradlew.bat'
if (-not (Test-Path $gradlew)) {
    Write-Host '找不到 gradlew.bat，请确认在项目根目录运行。' -ForegroundColor Red
    exit 1
}

Write-Host '正在编译...' -ForegroundColor Cyan
& $gradlew -p $root assembleDebug --console=plain

if ($LASTEXITCODE -ne 0) {
    Write-Host '编译失败，看上面的报错。' -ForegroundColor Red
    exit 1
}

$apk = Join-Path $root 'app\build\outputs\apk\debug\app-debug.apk'
Write-Host ''
Write-Host "编译成功：$apk" -ForegroundColor Green
Write-Host ("大小：" + [math]::Round((Get-Item $apk).Length / 1MB, 2) + ' MB')

# ---- 5. 可选：安装 ----
if ($Install) {
    & (Join-Path $root 'install.ps1')
}
