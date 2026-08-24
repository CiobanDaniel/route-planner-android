# Changelog

All notable changes to this project are documented in this file.

Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versioning follows [Semantic Versioning](https://semver.org/).

## [Unreleased]

## [0.4.0] — 2026-08-24

### Added
- Local stop library (device-only): save stops, add copies to routes, manage from the menu
- Groundwork for shared stops later (libraryStopId link; no accounts yet)
- Per-stop local task checklists, including required tasks and optional completion notes
- Launcher icon uses supplied map, route, and navigation-pin artwork
- Robolectric tests for stop-task completion gates and Room v2→v3 migration
- Account screen with debug preview sign-in; menu shows signed-in email
- JSON backup export/import in Settings (routes, library, stops, tasks)
- Duplicate a route (stops + tasks, progress reset) from the home list
- Filter routes on the home list; progress bar and approximate distance on each card
- Optimize stop order (nearest neighbor) from route detail and edit
- Preferred map style (map / satellite / terrain) is remembered and restored after driving
- Stop library is the canonical stop list; routes reference library stops and show where each is used

### Changed
- App version set to `0.4.0` (`versionCode` 5)
- Map tiles: Esri World Street Map for Map / Driving (dark theme inverts those street tiles), Esri hybrid satellite (roads + labels), Esri World Topo instead of OpenTopoMap
- Stop pins show stop numbers; completed stops are green
- Task checklist: editable titles, required chips, progress on stop rows; available during delivery
- List rows and menus use inset cards with borders so items no longer blend into the island
- Bottom islands on the map can be dragged down (grab bar) to show more of the map

### Fixed
- Room v3/v4 migration tests include the library and task indexes Room expects after upgrading to v5
- Removed duplicate English OSRM instruction builder; turn-by-turn display uses localized `ManeuverFormatter` only
- GPS-location stop name no longer falls back to hardcoded English in the ViewModel
- Heading triangle angle flipped back to match phone direction
- Required tasks now prevent a stop from being marked complete, skipped, or jumped over
- Map no longer opens on Timișoara; uses last-known GPS quickly and centers empty routes on you
- Floating islands and the attribution chip block map pan/zoom through their chrome
- Home menu uses a scrim (tap outside / Back closes) and no longer stacks with the map-layers menu
- Route detail attribution sits under the title beside the FAB stack instead of overlapping the title island; GPS/status copy stays in the title subtitle
- Lists in floating islands scroll from the whole list area, not only a few hit targets
- Saving an edited route no longer wipes per-stop tasks
- Follow-me rotates around your location (no look-ahead offset)
- Heading arrow tracks the phone when the map is panned or north-up; compass and GPS course share one geographic heading

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

[Unreleased]: https://github.com/CiobanDaniel/route-planner-android/compare/v0.4.0...HEAD
[0.4.0]: https://github.com/CiobanDaniel/route-planner-android/compare/v0.3.1...v0.4.0
[0.3.1]: https://github.com/CiobanDaniel/route-planner-android/compare/v0.3.0...v0.3.1
[0.3.0]: https://github.com/CiobanDaniel/route-planner-android/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/CiobanDaniel/route-planner-android/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/CiobanDaniel/route-planner-android/releases/tag/v0.1.0
