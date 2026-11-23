# FoodSave: start backend + frontend in two windows
param([switch]$NoBrowser)
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Start-Process powershell -WorkingDirectory $Root -ArgumentList @('-NoExit','-NoProfile','-ExecutionPolicy','Bypass','-File',"`"$(Join-Path $Root 'run_backend.ps1')`"")
Start-Process powershell -WorkingDirectory $Root -ArgumentList @('-NoExit','-NoProfile','-ExecutionPolicy','Bypass','-File',"`"$(Join-Path $Root 'run_frontend.ps1')`"")
if (-not $NoBrowser) { Start-Sleep -Seconds 25; Start-Process 'http://localhost:5173' }
