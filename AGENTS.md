# Agent notes (Cursor / AI assistants)

## Stack

- Kotlin, Jetpack Compose, Material 3
- Room (local routes/stops), DataStore (settings + delivery session)
- Map: Leaflet in WebView (`app/src/main/assets/map.html`)
- Routing: OSRM; Geocoding: Nominatim; Package: `com.danielcioban.routeplanner`

## Conventions

- Prefer island UI components (`FloatingIsland`, `IslandDialog`) over stock dialogs
- User-visible strings: `values/strings.xml` + `values-ro/strings.xml`
- Brand accent: orange (`BrandColors` / `#E86A17`)
- Do not commit secrets (`local.properties`, keystores, `google-services.json`)
- Gradle must run on **JDK 21** (not 25) with current Gradle wrapper

## Before large changes

Read `CONTRIBUTING.md`, `CHANGELOG.md`, and relevant ADRs under `docs/adr/`.
