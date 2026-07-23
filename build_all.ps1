# =============================================================
#  Map Chameleon — 一键构建所有 MC 版本范围的 jar
#  用法: .\build_all.ps1
#  产物: output\map-chameleon-1.2.2+mc1.20.5-1.21.11.jar
#        output\map-chameleon-1.2.2+mc1.20-1.20.4.jar
#
#  MC 26.x 暂不可用：需等 Mojang 官方映射发布到 Fabric Maven
#  (届时添加: -Pminecraft_version=26.x -Pjava_version=25)
# =============================================================
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

# 清除上次产物
if (Test-Path output) { Remove-Item -Recurse -Force output }
New-Item -ItemType Directory -Force output | Out-Null

# ── MC 1.20.5 ~ 1.21.11（Yarn 映射）──
Write-Host "`n========== Building MC 1.20.5-1.21.11 ==========" -ForegroundColor Cyan
.\gradlew.bat clean build --no-daemon `
    "-Pminecraft_version=1.20.5" `
    "-Pyarn_mappings=1.20.5+build.1" `
    "-Pfabric_version=0.97.8+1.20.5" `
    "-Pjava_version=21"
Copy-Item build\libs\map-chameleon-*.jar output\
Write-Host ">> output\$(Get-ChildItem output\map-chameleon-*+mc1.20.5-1.21.11.jar | ForEach-Object { $_.Name })" -ForegroundColor Green

# ── MC 1.20 ~ 1.20.4（Legacy Yarn 映射）──
Write-Host "`n========== Building MC 1.20-1.20.4 ==========" -ForegroundColor Cyan
.\gradlew.bat clean build --no-daemon `
    "-Pminecraft_version=1.20.4" `
    "-Pyarn_mappings=1.20.4+build.3" `
    "-Pfabric_version=0.96.11+1.20.4" `
    "-Pjava_version=17"
Copy-Item build\libs\map-chameleon-*.jar output\
Write-Host ">> output\$(Get-ChildItem output\map-chameleon-*+mc1.20-1.20.4.jar | ForEach-Object { $_.Name })" -ForegroundColor Green

# ── MC 26.x（Mojang 映射，待启用）──
# 当 Mojang 官方映射发布后取消下面注释：
# Write-Host "`n========== Building MC 26.x ==========" -ForegroundColor Cyan
# .\gradlew.bat clean build --no-daemon `
#     "-Pminecraft_version=26.2" `
#     "-Pfabric_version=0.155.2+26.2" `
#     "-Pjava_version=25"
# Copy-Item build\libs\map-chameleon-*.jar output\

Write-Host "`n========== DONE ==========" -ForegroundColor Green
Get-ChildItem output | ForEach-Object { Write-Host "  $($_.Name)  ($('{0:N0} KB' -f ($_.Length / 1KB)))" }
