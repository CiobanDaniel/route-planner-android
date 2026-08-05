# Changelog

All notable changes to this project are documented in this file.

Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versioning follows [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- Local stop library (device-only): save stops, add copies to routes, manage from the menu
- Groundwork for shared stops later (libraryStopId link; no accounts yet)

### Fixed
- Heading triangle angle flipped back to match phone direction
- Map no longer opens on Timișoara; uses last-known GPS quickly and centers empty routes on you

## [0.3.1] — 2026-07-31

### Added
- About screen (menu) with map / routing credits
- Visible OpenStreetMap attribution chip on map screens
- Clearer warning when road routing falls back to a straight line, with Retry
- Delivery stop queue: peek remaining stops, skip current, jump ahead
- Reorder stops on the route detail list (up/down)
- Localized turn-by-turn maneuver instructions (EN/RO)

### Fixed
- Start no longer enters delivery mode before GPS is available
- Stop list shows address hints, pin status, and unit-aware “from previous” distance
- Warn when remaining stops are missing map pins before Start
- Follow-me heading prefers device compass (no stale GPS north lock); map bearing no longer fights animated pans
- Map type menu opens as an overlay so it no longer pushes other floating controls
- Compass remapped for flat-on-table and upright phone poses; user triangle tracks geographic heading even if map rotation fails
- Map type picker uses a Dialog window so layout never reflows
- Map type / app menus are hoisted to the screen root so they cannot grow a wrapping Box and shove other islands
- User heading triangle stays locked to the phone when the map is rotated by hand
- Heading triangle mirroring fixed for leaflet-rotate’s screen-fixed marker pane
- Dark mode: drop-shadow elevation only (no white halo); stronger muted text contrast
- Turn instructions keep a space before “onto” / “pe”
## [0.3.0] — 2026-07-31

### Added
- Address search (Nominatim) to add stops from place names
- Share route as text (stops, notes, coordinates, map links)
- Persist/resume delivery session across app restarts
- Reset delivery progress / end-and-reset options
- Delete stop from the map stop-details dialog
- Straight-line navigation fallback when OSRM road routing fails
- Broader English / Romanian string coverage for delivery and edit flows
- Team scaffolding: CI, contributing guide, security policy, notices, Dependabot

### Changed
- App version set to `0.3.0` (`versionCode` 3)
- Brand accent unified to orange (from earlier 0.2 work)

## [0.2.0] — 2026-07-31

### Added
- Map-first UI (Leaflet + OSM tiles in WebView)
- Floating island UI panels
- Long-press / GPS stop placement
- Delivery mode with OSRM turn-by-turn guidance
- External Google Maps / Waze fallback
- Settings: theme, language (EN/RO), distance units, keep screen on
- Orange brand accent and launcher icon
- AppCompat theme fix for launch crash

## [0.1.0] — 2026-07

### Added
- Local route / stop CRUD with Room
- Basic list and edit flows
- Straight-line distance helpers between stops

[Unreleased]: https://github.com/CiobanDaniel/route-planner-android/compare/v0.3.1...HEAD
[0.3.1]: https://github.com/CiobanDaniel/route-planner-android/compare/v0.3.0...v0.3.1
[0.3.0]: https://github.com/CiobanDaniel/route-planner-android/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/CiobanDaniel/route-planner-android/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/CiobanDaniel/route-planner-android/releases/tag/v0.1.0
