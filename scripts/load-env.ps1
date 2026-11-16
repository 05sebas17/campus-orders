# Ejecutar con dot-sourcing desde la raíz: . .\scripts\load-env.ps1
$envPath = Join-Path $PSScriptRoot '..\.env'
if (!(Test-Path -LiteralPath $envPath)) { throw 'Copia .env.example a .env y configura DB_PASSWORD.' }
foreach ($line in Get-Content -LiteralPath $envPath) {
  if ($line -match '^([A-Z_][A-Z0-9_]*)=(.*)$') {
    [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
  }
}
$env:DB_URL = "jdbc:postgresql://localhost:$($env:DB_PORT)/$($env:DB_NAME)"
$env:SERVER_PORT = $env:BACKEND_PORT
