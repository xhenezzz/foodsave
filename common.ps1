# Shared helpers for FoodSave scripts
function Refresh-Path {
  $env:Path = [Environment]::GetEnvironmentVariable('Path','Machine') + ';' + [Environment]::GetEnvironmentVariable('Path','User')
}
function Has($c) { [bool](Get-Command $c -ErrorAction SilentlyContinue) }
function Read-DotEnv($root) {
  $h = @{}
  $f = Join-Path $root '.env'
  if (Test-Path $f) {
    Get-Content $f | ForEach-Object {
      if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$') { $h[$matches[1]] = $matches[2].Trim() }
    }
  }
  return $h
}
function Get-JavaMajor {
  if (-not (Has java)) { return 0 }
  $v = cmd /c "java -version 2>&1" | Select-Object -First 1
  if ($v -match '"(\d+)(\.(\d+))?') { $m = [int]$matches[1]; if ($m -eq 1) { return [int]$matches[3] }; return $m }
  return 0
}
function Find-JavaHome {
  $adopt = Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -Filter 'jdk-*' -ErrorAction SilentlyContinue |
           Sort-Object Name -Descending | Select-Object -First 1
  if ((Get-JavaMajor) -ge 17) {
    $jh = cmd /c "java -XshowSettings:properties -version 2>&1" |
          ForEach-Object { if ($_ -match 'java\.home = (.+)$') { $matches[1].Trim() } } | Select-Object -First 1
    if ($jh -and (Test-Path "$jh\bin\java.exe")) { return $jh }
  }
  if ($adopt) { return $adopt.FullName }
  return $null
}
function Find-Psql {
  $p = Get-ChildItem 'C:\Program Files\PostgreSQL\*\bin\psql.exe' -ErrorAction SilentlyContinue |
       Sort-Object FullName -Descending | Select-Object -First 1
  if ($p) { return $p.FullName }
  $c = Get-Command psql -ErrorAction SilentlyContinue
  if ($c) { return $c.Source }
  return $null
}
function Find-Jar($root) {
  Get-ChildItem (Join-Path $root 'target') -Filter 'FoodSave-*.jar' -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notlike '*.original' } | Select-Object -First 1
}
