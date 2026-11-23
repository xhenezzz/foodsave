$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root
. (Join-Path $Root 'common.ps1')
$host.UI.RawUI.WindowTitle = 'FoodSave backend :8081 (close window to stop)'
Refresh-Path
$jh = Find-JavaHome
if (-not $jh) { Write-Host 'Java 17+ not found. Run setup_foodsave.bat first.' -ForegroundColor Red; return }
$jar = Find-Jar $Root
if (-not $jar) {
  $env:JAVA_HOME = $jh
  & (Join-Path $Root 'mvnw.cmd') -q -Dmaven.test.skip=true package
  $jar = Find-Jar $Root
}
& (Join-Path $jh 'bin\java.exe') -jar $jar.FullName --spring.flyway.baseline-on-migrate=true 2>&1 |
  Tee-Object -FilePath (Join-Path $Root 'backend_log.txt')
