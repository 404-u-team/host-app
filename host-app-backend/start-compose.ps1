$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root '.env'
$compose = Join-Path $PSScriptRoot 'compose.yaml'
$coursework = Join-Path $PSScriptRoot 'compose.kr3.yaml'
if (!(Test-Path $envFile)) {
    throw 'Create .env in the project root using .env.example.'
}
docker compose --env-file $envFile -f $compose -f $coursework up -d --build --wait
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
