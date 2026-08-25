# ADR 0001: Map via WebView + Leaflet (not MapLibre native)

## Status

Accepted (2026)

## Context

The app needs an interactive map for placing stops and showing routes on Android,
including emulators that historically struggled with some OpenGL map stacks.

## Decision

Render the map with **Leaflet inside a WebView** (`app/src/main/assets/map.html`),
bridged from Compose via JavaScript interfaces.

## Consequences

### Positive

- Stable on emulators and low-end devices
- Easy to iterate on map HTML/CSS/JS without full native map SDK wiring
- Tile/style switching is straightforward in JS

### Negative

- Gesture / lifecycle bridging is custom and fragile
- Harder to use native map SDK features (offline packs, 3D, etc.)
- Performance depends on WebView

## Alternatives considered

- **MapLibre Native** — preferred long-term for production maps; deferred after early OpenGL crash on Android 7 emulator
- **Google Maps SDK** — possible later; cost/ToS and Play Services coupling

## When to revisit

When targeting production scale offline maps, or when WebView gesture bugs block delivery UX.

**Vector maps (OpenFreeMap / MapLibre GL)** would look more modern than raster tiles, but they would replace leaflet-rotate. Do not switch until heading is re-proven on device (see `docs/DEV_STATUS.md`). Everyday streets are Humanitarian OSM (HOT) rasters in Leaflet; dark / Night streets are CARTO Dark Matter; satellite/terrain stay Esri.
