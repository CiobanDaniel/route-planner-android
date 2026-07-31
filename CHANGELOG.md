# Changelog

All notable changes to this project are documented in this file.

Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versioning follows [Semantic Versioning](https://semver.org/).

## [Unreleased]

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

[Unreleased]: https://github.com/CiobanDaniel/route-planner-android/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/CiobanDaniel/route-planner-android/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/CiobanDaniel/route-planner-android/releases/tag/v0.1.0
