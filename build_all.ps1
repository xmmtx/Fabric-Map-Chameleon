# =============================================================
#  Map Chameleon — 一键构建两个 MC 版本范围的 jar
#  用法: .\build_all.ps1
#  产物: output\map-chameleon-1.0.0+mc1.20.5-26.2.jar
#        output\map-chameleon-1.0.0+mc1.20-1.20.4.jar
# =============================================================
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

# 清除上次产物
if (Test-Path output) { Remove-Item -Recurse -Force output }
New-Item -ItemType Directory -Force output | Out-Null

# ── MC 1.20.5 ~ 26.2 ──
Write-Host "`n========== Building MC 1.20.5-26.2 ==========" -ForegroundColor Cyan
.\gradlew.bat clean build --no-daemon `
    "-Pminecraft_version=1.20.5" `
    "-Pyarn_mappings=1.20.5+build.1" `
    "-Pfabric_version=0.97.8+1.20.5" `
    "-Pjava_version=21"
Copy-Item build\libs\map-chameleon-*.jar output\
Write-Host ">> output\$(Get-ChildItem output\map-chameleon-*+mc1.20.5-26.2.jar | ForEach-Object { $_.Name })" -ForegroundColor Green

# ── MC 1.20 ~ 1.20.4 ──
Write-Host "`n========== Building MC 1.20-1.20.4 ==========" -ForegroundColor Cyan
.\gradlew.bat clean build --no-daemon `
    "-Pminecraft_version=1.20.4" `
    "-Pyarn_mappings=1.20.4+build.3" `
    "-Pfabric_version=0.96.11+1.20.4" `
    "-Pjava_version=17"
Copy-Item build\libs\map-chameleon-*.jar output\
Write-Host ">> output\$(Get-ChildItem output\map-chameleon-*+mc1.20-1.20.4.jar | ForEach-Object { $_.Name })" -ForegroundColor Green

Write-Host "`n========== DONE ==========" -ForegroundColor Green
Get-ChildItem output | ForEach-Object { Write-Host "  $($_.Name)  ($('{0:N0} KB' -f ($_.Length / 1KB)))" }
