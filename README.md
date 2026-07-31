# Route Planner — Android

Multi-stop route planning for deliveries. Package: `com.danielcioban.routeplanner`.

Current app version: **0.3.0**

## Quick start

1. Open this folder in **Android Studio**
2. Set **Gradle JDK** to **21**  
   (`Settings → Build Tools → Gradle → Gradle JDK`)  
   Do **not** use JDK 25 with Gradle 8.14.x
3. Sync Gradle, then Run on an emulator (API 34+) or device

CLI (JDK 21 on `PATH` / `JAVA_HOME`):

```bash
./gradlew assembleDebug testDebugUnitTest lintDebug
```

On Windows: `gradlew.bat …`

## Features (0.3)

- Map background (Leaflet + OSM tiles in a WebView)
- Floating island UI; create / edit / delete routes and stops
- Long-press map, GPS, or **address search** to add stops
- Delivery mode with OSRM turn-by-turn + straight-line fallback
- Open in Google Maps / Waze; share route as text
- Resume delivery session; reset progress
- Settings: theme, EN/RO, metric/imperial, keep screen on
- Local Room storage

## Stack

| Piece | Choice |
|--------|--------|
| UI | Kotlin, Jetpack Compose, Material 3 |
| Storage | Room + DataStore |
| Maps | Leaflet WebView + OSM-compatible tiles |
| Routing / geocode | OSRM / Nominatim (dev endpoints) |
| License | Apache-2.0 |

## Docs for collaborators

| Doc | Purpose |
|-----|---------|
| [CONTRIBUTING.md](CONTRIBUTING.md) | Branching, PRs, local checks |
| [CHANGELOG.md](CHANGELOG.md) | Version history |
| [SECURITY.md](SECURITY.md) | Vulnerability reporting |
| [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) | OSM / routing attribution |
| [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md) | How to cut a release |
| [docs/MANUAL_SETUP.md](docs/MANUAL_SETUP.md) | Play / Firebase / branch protection (manual) |
| [docs/PRIVACY_DRAFT.md](docs/PRIVACY_DRAFT.md) | Privacy policy draft |
| [docs/adr/](docs/adr/) | Architecture decision records |
| [AGENTS.md](AGENTS.md) | Notes for AI coding assistants |

## Project layout

```
app/src/main/java/com/danielcioban/routeplanner/
  MainActivity.kt
  data/          # Room, settings, routing, geocoding, delivery session
  ui/            # screens, map, components
  util/          # geo, share, external navigation
app/src/main/assets/map.html
```

## CI

Pull requests to `main` run GitHub Actions: unit tests, lint, assemble debug.
