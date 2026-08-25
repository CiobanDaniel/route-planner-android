# Third-party notices

Route Planner uses open map and routing services. Keep this file accurate when adding SDKs.

## OpenStreetMap

Map data © OpenStreetMap contributors.

- License: [ODbL](https://www.openstreetmap.org/copyright)
- Usage: base map tiles / geocoding context

When you ship publicly, show OSM attribution in the app (About / map footer) and in store listings.

## Leaflet

- Project: [Leaflet](https://leafletjs.com/)
- License: BSD-2-Clause
- Usage: map rendering inside an Android WebView (`app/src/main/assets/map.html`)

## Tile providers

Everyday **Map** / **Driving** streets: Humanitarian OSM (HOT) raster tiles hosted by [OSM France](https://www.openstreetmap.fr/usage/) (OSM data). Dark theme and **Night** use [CARTO Dark Matter](https://carto.com/attribution/) rasters (not inverted HOT tiles). Satellite and terrain stay Esri. Identify the app in the WebView User-Agent; cache tiles; do not hammer the public tile servers. Self-host or use a keyed plan (MapTiler, Stadia, Esri) if traffic grows.

**Satellite:** Esri World Imagery plus Esri Reference overlays (roads and place names).

**Terrain:** Esri World Topo Map.

Respect each provider’s tile usage policy and attribution. Vector MapLibre / OpenFreeMap is deferred while heading uses leaflet-rotate.

## OSRM (Open Source Routing Machine)

- Demo server: `https://router.project-osrm.org` (not for a van day)
- Self-host pack (Romania extract): [`docs/SELF_HOST_ROUTING.md`](docs/SELF_HOST_ROUTING.md)
- License: BSD-ish (see upstream)
- Usage: driving routes and turn instructions

## Nominatim

- Service: `https://nominatim.openstreetmap.org` (demo; usage policy below)
- Self-host pack: [`docs/SELF_HOST_ROUTING.md`](docs/SELF_HOST_ROUTING.md)
- Usage policy: https://operations.osmfoundation.org/policies/nominatim/
- Usage: address / place search

Must send a valid identifying User-Agent (already set in `NominatimGeocodingClient`).
Do not hammer the public instance; cache results and debounce searches. Self-host or use a provider for production scale.

## Android / Google libraries

AndroidX, Material, Play Services Location, OkHttp, and other Gradle dependencies retain their upstream licenses (typically Apache-2.0).
Generate a full dependency license report before a commercial store release if required by counsel.
