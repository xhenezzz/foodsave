# FoodSave: one-time setup (install tools, create DB, build, start)
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root
. (Join-Path $Root 'common.ps1')
$Log = Join-Path $Root 'setup_log.txt'
Start-Transcript -Path $Log -Force | Out-Null

function Step($m) { Write-Host "`n=== $m ===" -ForegroundColor Cyan }
function Fail($m) { Write-Host "`nERROR: $m" -ForegroundColor Red; Write-Host 'SETUP_RESULT=FAILED'; Stop-Transcript | Out-Null; exit 1 }

$cfg = Read-DotEnv $Root
$dbUrl  = if ($cfg['DB_URL']) { $cfg['DB_URL'] } else { 'jdbc:postgresql://localhost:5432/foodsave_db' }
$dbUser = if ($cfg['DB_USER']) { $cfg['DB_USER'] } else { 'postgres' }
if ($dbUrl -notmatch '//([^:/]+)(:(\d+))?/([^/?]+)') { Fail "Cannot parse DB_URL" }
$dbHost = $matches[1]; $dbPort = if ($matches[3]) { $matches[3] } else { '5432' }; $dbName = $matches[4]

Refresh-Path
$wgArgs = @('-e','--accept-source-agreements','--accept-package-agreements')

# ---------- 1. Java ----------
Step 'Java (need 17+)'
if ((Get-JavaMajor) -lt 17 -and -not (Find-JavaHome)) {
  if (-not (Has winget)) { Fail 'winget not found. Install "App Installer" from Microsoft Store.' }
  Write-Host 'Installing Eclipse Temurin JDK 21...'
  winget install --id EclipseAdoptium.Temurin.21.JDK @wgArgs | Out-Host
  Refresh-Path
}
$env:JAVA_HOME = Find-JavaHome
if (-not $env:JAVA_HOME) { Fail 'Java 17+ not found after install.' }
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Write-Host "JAVA_HOME = $env:JAVA_HOME"
cmd /c "java -version 2>&1" | Out-Host

# ---------- 2. Node ----------
Step 'Node.js (need 22+)'
$nodeMajor = 0
if (Has node) { $nv = (node -v); if ($nv -match 'v(\d+)') { $nodeMajor = [int]$matches[1] } }
if ($nodeMajor -lt 22) {
  if (-not (Has winget)) { Fail 'winget not found.' }
  Write-Host 'Installing Node.js LTS...'
  winget install --id OpenJS.NodeJS.LTS @wgArgs | Out-Host
  Refresh-Path
}
if (-not (Has node)) { Fail 'Node.js not found after install.' }
Write-Host "node $(node -v), npm $(npm -v)"

# ---------- 3. PostgreSQL ----------
Step 'PostgreSQL'
$psql = Find-Psql
if (-not $psql) {
  if (-not (Has winget)) { Fail 'winget not found.' }
  Write-Host ''
  Write-Host '>>> The PostgreSQL installer window will open.' -ForegroundColor Yellow
  Write-Host '>>> Click Next everywhere. When asked for a password, enter the value of DB_PASSWORD from the .env file.' -ForegroundColor Yellow
  Write-Host '>>> Keep port 5432. Uncheck Stack Builder at the end.' -ForegroundColor Yellow
  winget install --id PostgreSQL.PostgreSQL.17 --interactive @wgArgs | Out-Host
  Refresh-Path
  $psql = Find-Psql
}
if (-not $psql) { Fail 'psql.exe not found after install.' }
Write-Host "psql = $psql"
Get-Service -Name 'postgresql*' -ErrorAction SilentlyContinue | Format-Table Name, Status -AutoSize

$env:PGPASSWORD = $cfg['DB_PASSWORD']
$ok = $false
for ($i = 0; $i -lt 10; $i++) {
  & $psql -h $dbHost -p $dbPort -U $dbUser -d postgres -tAc 'select 1' 2>$null | Out-Null
  if ($LASTEXITCODE -eq 0) { $ok = $true; break }
  Start-Sleep -Seconds 3
}
if (-not $ok) {
  & $psql -h $dbHost -p $dbPort -U $dbUser -d postgres -tAc 'select 1'
  Fail "Cannot connect to PostgreSQL as '$dbUser'. Check that the password in .env (DB_PASSWORD) matches the postgres password."
}

Step "Database $dbName"
$exists = "$(& $psql -h $dbHost -p $dbPort -U $dbUser -d postgres -tAc "select 1 from pg_database where datname='$dbName'")".Trim()
if ($exists -ne '1') {
  & $psql -h $dbHost -p $dbPort -U $dbUser -d postgres -c "CREATE DATABASE $dbName"
  if ($LASTEXITCODE -ne 0) { Fail 'CREATE DATABASE failed' }
  Write-Host 'Created.'
} else { Write-Host 'Already exists.' }
$hasSchema = "$(& $psql -h $dbHost -p $dbPort -U $dbUser -d $dbName -tAc "select to_regclass('public.food_listings') is not null")".Trim()

# ---------- 4. Build backend ----------
Step 'Building backend (first time downloads Maven + libraries, may take several minutes)'
cmd /c "mvnw.cmd -q -Dmaven.test.skip=true package 2>&1" | Out-Host
if ($LASTEXITCODE -ne 0) { Fail 'Maven build failed (see above).' }
$jar = Find-Jar $Root
if (-not $jar) { Fail 'Jar not found in target\' }
Write-Host "Built $($jar.Name)"
$java = Join-Path $env:JAVA_HOME 'bin\java.exe'

# stop anything already on 8081
$busy = Get-NetTCPConnection -LocalPort 8081 -State Listen -ErrorAction SilentlyContinue
if ($busy) { Fail "Port 8081 is already in use (PID $($busy.OwningProcess)). Close that program and run setup again." }

# ---------- 5. Bootstrap schema ----------
if ($hasSchema -ne 't') {
  Step 'Creating tables (one-time bootstrap via Hibernate)'
  $out = Join-Path $Root 'bootstrap_log.txt'; $err = Join-Path $Root 'bootstrap_err.txt'
  $p = Start-Process -FilePath $java -WorkingDirectory $Root -NoNewWindow -PassThru `
       -RedirectStandardOutput $out -RedirectStandardError $err `
       -ArgumentList @('-jar', "`"$($jar.FullName)`"", '--spring.flyway.enabled=false', '--spring.jpa.hibernate.ddl-auto=update', '--server.port=8089')
  $started = $false
  for ($i = 0; $i -lt 120; $i++) {
    Start-Sleep -Seconds 2
    if ((Test-Path $out) -and (Select-String -Path $out -Pattern 'Started FoodSaveApplication' -Quiet)) { $started = $true; break }
    if ($p.HasExited) { break }
  }
  if (-not $p.HasExited) { Stop-Process -Id $p.Id -Force }
  if (-not $started) { Get-Content $out -Tail 60; Get-Content $err -Tail 30; Fail 'Bootstrap start failed (see bootstrap_log.txt).' }
  Write-Host 'Tables created.'
} else { Write-Host "`nSchema already present, skipping bootstrap." }

# ---------- 6. Frontend deps ----------
Step 'Installing frontend dependencies'
$fe = Join-Path $Root 'foodsave-frontend'
if (Test-Path (Join-Path $fe 'node_modules')) { Remove-Item -Recurse -Force (Join-Path $fe 'node_modules') }
Push-Location $fe
cmd /c "npm install 2>&1" | Out-Host
$npmOk = $LASTEXITCODE
Pop-Location
if ($npmOk -ne 0) { Fail 'npm install failed.' }

# ---------- 7. Start ----------
Step 'Starting backend and frontend in separate windows'
& (Join-Path $Root 'start_foodsave.ps1') -NoBrowser

Write-Host 'Waiting for backend...'
$up = $false
for ($i = 0; $i -lt 90; $i++) {
  Start-Sleep -Seconds 2
  try { $r = Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8081/api/listings?city=ALMATY' -TimeoutSec 3; $up = $true; Write-Host "Backend HTTP $($r.StatusCode)"; break }
  catch { if ($_.Exception.Response) { $up = $true; Write-Host "Backend HTTP $([int]$_.Exception.Response.StatusCode)"; break } }
}
if (-not $up) { Fail 'Backend did not answer on :8081 within 3 minutes. See backend_log.txt' }

$feUp = $false
for ($i = 0; $i -lt 30; $i++) {
  Start-Sleep -Seconds 2
  try { Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:5173' -TimeoutSec 3 | Out-Null; $feUp = $true; break } catch {}
}
if (-not $feUp) { Fail 'Frontend did not answer on :5173. See frontend_log.txt' }

Write-Host "`nAll good! Open http://localhost:5173" -ForegroundColor Green
Write-Host 'Next time just double-click start_foodsave.bat'
Write-Host 'SETUP_RESULT=OK'
Stop-Transcript | Out-Null
Start-Process 'http://localhost:5173'
