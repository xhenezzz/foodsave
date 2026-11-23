$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $Root 'common.ps1')
$host.UI.RawUI.WindowTitle = 'FoodSave frontend :5173 (close window to stop)'
Refresh-Path
Set-Location (Join-Path $Root 'foodsave-frontend')
if (-not (Test-Path 'node_modules')) { cmd /c "npm install 2>&1" }
cmd /c "npm run dev -- --port 5173 --strictPort 2>&1" | Tee-Object -FilePath (Join-Path $Root 'frontend_log.txt')
