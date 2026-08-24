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

## Tile providers (Esri)

Everyday **Map** / **Driving** streets: Esri World Street Map (OSM and other sources). Dark theme inverts the same street tiles so roads stay light on land; satellite and terrain are not inverted.

**Satellite:** Esri World Imagery plus Esri Reference overlays (roads and place names).

**Terrain:** Esri World Topo Map.

Respect each provider’s tile usage policy and attribution. Esri’s public raster services are intended for fair-use; self-host or use a keyed commercial plan (MapTiler, Stadia, etc.) if traffic grows. Vector MapLibre / OpenFreeMap is deferred while heading uses leaflet-rotate.

## OSRM (Open Source Routing Machine)

- Demo server: `https://router.project-osrm.org` (not for heavy production)
- License: BSD-ish (see upstream)
- Usage: driving routes and turn instructions

For production, run your own OSRM instance or a commercial routing API.

## Nominatim

- Service: `https://nominatim.openstreetmap.org`
- Usage policy: https://operations.osmfoundation.org/policies/nominatim/
- Usage: address / place search

Must send a valid identifying User-Agent (already set in `NominatimGeocodingClient`).
Do not hammer the public instance; cache results and debounce searches. Self-host or use a provider for production scale.

## Android / Google libraries

AndroidX, Material, Play Services Location, OkHttp, and other Gradle dependencies retain their upstream licenses (typically Apache-2.0).
Generate a full dependency license report before a commercial store release if required by counsel.
