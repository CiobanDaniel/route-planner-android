# Route Planner — Android

Multi-stop route planning for deliveries. Package: `com.danielcioban.routeplanner`.

Current app version: **0.5.0** (local restore point; see [CHANGELOG.md](CHANGELOG.md))

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

## Features

- Map background (Leaflet + HOT street tiles / Esri satellite & terrain in a WebView)
- Floating island UI; create / edit / delete routes and stops
- Long-press map, GPS, or **address search** to add stops
- **Stop library** as the canonical stop list; routes reference shared stops
- Per-stop **task checklists** (required tasks gate completion)
- Delivery mode with OSRM turn-by-turn + last polyline cache + straight-line fallback
- Drive to a place without creating a route; **trip history**; return-to-driving overlay
- Drag-and-drop stop reorder (up/down and move to top/bottom stay)
- **CSV import and export** of stops (library columns). JSON backup is the full restore (routes, library, tasks, history)
- Open in Google Maps / Waze; share route as text; duplicate and filter routes
- Resume delivery session; reset progress; optimize stop order (nearest-neighbor + 2-opt, then OSRM road times when the set is small)
- Background trip: ongoing Driving notification, Android 16 status-bar chip, spoken turns
- Lock-screen / always-on **nav HUD** (next stop, distance, maneuver — no map on the lock screen)
- Settings: theme, language (EN, RO, FR, DE, IT, ES, PT), units, keep screen on, speak turns, geofence, TTS verbosity, data saver
- Privacy policy link in About and Settings; **Report a problem** emails version + last routing error
- Local Room storage (no cloud sync yet)

## Stack

| Piece | Choice |
|--------|--------|
| UI | Kotlin, Jetpack Compose, Material 3 |
| Storage | Room + DataStore |
| Maps | Leaflet WebView + OSM-compatible tiles |
| Routing / geocode | OSRM / Nominatim (`local.properties`; Romania Docker pack in `deploy/routing/`) |
| License | Apache-2.0 |

## Docs for collaborators

| Doc | Purpose |
|-----|---------|
| [docs/README.md](docs/README.md) | Doc map: GitHub vs local, what is still true |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Branching, PRs, local checks |
| [CHANGELOG.md](CHANGELOG.md) | Version history |
| [SECURITY.md](SECURITY.md) | Vulnerability reporting |
| [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) | OSM / routing attribution |
| [docs/DEV_STATUS.md](docs/DEV_STATUS.md) | Protected behaviors, session log, what to do next |
| [docs/PRODUCT_BACKLOG.md](docs/PRODUCT_BACKLOG.md) | Idea menu and progress snapshot |
| [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md) | How to cut a release |
| [docs/MANUAL_SETUP.md](docs/MANUAL_SETUP.md) | Play / Firebase / branch protection (manual) |
| [docs/PRIVACY_DRAFT.md](docs/PRIVACY_DRAFT.md) | Privacy policy draft |
| [docs/privacy/index.html](docs/privacy/index.html) | Hostable privacy page (GitHub Pages `/docs`) |
| [docs/PLAY_LISTING.md](docs/PLAY_LISTING.md) | Play listing copy, screenshots, IARC, Vitals |
| [docs/play/](docs/play/) | Feature graphic PNG, IARC answers, Internal testing runbook |
| [docs/SELF_HOST_ROUTING.md](docs/SELF_HOST_ROUTING.md) | Romania OSRM + Nominatim Docker pack |
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
