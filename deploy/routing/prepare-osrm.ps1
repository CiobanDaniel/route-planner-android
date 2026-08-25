# Build the Romania OSRM car graph into .\data (needs ~8 GB RAM, ~8 GB disk).
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
New-Item -ItemType Directory -Force -Path data | Out-Null
$pbf = Join-Path (Get-Location) "data\romania-latest.osm.pbf"
if (-not (Test-Path $pbf)) {
    Write-Host "Downloading Romania OSM extract (Geofabrik)…"
    Invoke-WebRequest -Uri "https://download.geofabrik.de/europe/romania-latest.osm.pbf" -OutFile $pbf
}
$image = "ghcr.io/project-osrm/osrm-backend:v5.27.1"
$dataDir = (Resolve-Path ".\data").Path
docker run --rm -v "${dataDir}:/data" $image osrm-extract -p /opt/car.lua /data/romania-latest.osm.pbf
if ($LASTEXITCODE -ne 0) { throw "osrm-extract failed" }
docker run --rm -v "${dataDir}:/data" $image osrm-partition /data/romania-latest.osrm
if ($LASTEXITCODE -ne 0) { throw "osrm-partition failed" }
docker run --rm -v "${dataDir}:/data" $image osrm-customize /data/romania-latest.osrm
if ($LASTEXITCODE -ne 0) { throw "osrm-customize failed" }
Write-Host "OSRM graph ready. Start routing with: docker compose up -d osrm"
