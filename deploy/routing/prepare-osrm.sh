#!/usr/bin/env bash
# Build the Romania OSRM car graph into ./data (needs ~8 GB RAM, ~8 GB disk).
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p data
PBF=data/romania-latest.osm.pbf
if [[ ! -f "$PBF" ]]; then
  echo "Downloading Romania OSM extract (Geofabrik)…"
  curl -L --fail --progress-bar -o "$PBF" \
    "https://download.geofabrik.de/europe/romania-latest.osm.pbf"
fi
IMAGE=ghcr.io/project-osrm/osrm-backend:v5.27.1
DATA_DIR="$(pwd)/data"
docker run --rm -v "${DATA_DIR}:/data" "$IMAGE" \
  osrm-extract -p /opt/car.lua /data/romania-latest.osm.pbf
docker run --rm -v "${DATA_DIR}:/data" "$IMAGE" \
  osrm-partition /data/romania-latest.osrm
docker run --rm -v "${DATA_DIR}:/data" "$IMAGE" \
  osrm-customize /data/romania-latest.osrm
echo "OSRM graph ready. Start routing with: docker compose up -d osrm"
