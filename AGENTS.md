# Agent notes (Cursor / AI assistants)

## Stack

- Kotlin, Jetpack Compose, Material 3
- Room (local routes/stops + stop library), DataStore (settings + delivery session)
- Map: Leaflet in WebView (`app/src/main/assets/map.html`)
- Routing: OSRM; Geocoding: Nominatim; Package: `com.danielcioban.routeplanner`

## Continuity (read first)

**Mandatory before map / heading / floating chrome / Room changes:**

→ [`docs/DEV_STATUS.md`](docs/DEV_STATUS.md)

That file is the protected-behavior log, modularity rules, smoke tests, and backlog. Update it when you finish meaningful work so a context reset does not undo fixes.

What belongs on GitHub vs this PC: [`docs/README.md`](docs/README.md).

## Conventions

- Prefer island UI components (`FloatingIsland`, `IslandDialog`) over stock dialogs
- **Overlays only for menus** — Dialog / Popup / `Box(fillMaxSize) + align`; never expand inside a height-wrapping chrome `Box`/`Column` (see DEV_STATUS)
- User-visible strings: `values/strings.xml` (English default) plus `values-ro`, `values-fr`, `values-de`, `values-it`, `values-es`, `values-pt` (leading spaces → `\u0020`)
- Brand accent: orange (`BrandColors` / `#E86A17`)
- Do not commit secrets (`local.properties`, keystores, `google-services.json`)
- Gradle must run on **JDK 21** (not 25) with current Gradle wrapper
- After editing `map.html`, reinstall or clear app data (WebView asset cache)

## Before large changes

Read `docs/DEV_STATUS.md`, `docs/README.md`, `CONTRIBUTING.md`, `CHANGELOG.md`, and relevant ADRs under `docs/adr/`.
