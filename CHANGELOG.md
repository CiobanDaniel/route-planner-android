# Changelog

All notable changes to this project are documented in this file.

Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versioning follows [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Fixed
- Queue **Skip current** moves the stop later (same as HUD Skip) instead of opening failed-delivery
- Skip later retargets in-app navigation and the background trip when the next stop changes
- Jump ahead without a GPS fix no longer starts a trip; Retry / GPS return keep the jumped stop
- Copy-to-route toast is honest when the stop is already on the target; encrypted SAF save uses `.rpenc`
- Cached road banner no longer claims the network is down when you are online; report-a-problem only keeps an OSRM error if every host failed
- Keep-screen-on while moving respects Pause on the route screen

### Changed
- Visual polish: orange reserved for primary actions, quieter dark-mode labels, softer island chrome. Home route rows keep Start and fold Maps/share/archive/delete into a more menu. Driving HUD secondary actions wrap as muted links instead of stacked orange buttons.
- Island elevation: light on the face from upper-left; soft drop shadow cast lower-right onto the map (no surrounding white glow)
- Docs: `docs/README.md` maps planning files (GitHub vs local). Stale 0.3 / EN+RO / Room-v8 notes aligned to 0.5. `.gitignore` keeps Play screenshot PNGs, tester emails, IDE junk, and routing extracts off GitHub.

### Added
- Languages in Settings: English, Romanian, French, German, Italian, Spanish, Portuguese (`localeConfig` for Android 13+ per-app language). Google sign-in copy on Account (button disabled; no Auth SDK)
- Overlay-safe **tablet / landscape** layout: side map islands at ≥ 700 dp (Home rail fills height; remaining-stops rail beside the HUD when there is room); title chrome max 560 dp; dialogs / lock HUD / Settings-style screens cap around 720 dp
- Settings grouped into collapsible sections; choice lists are dropdowns (theme, language, units, TTS, vehicle, geofence, …)
- Play-ready **1024×500** feature graphic PNG; IARC answer sheet; Internal testing runbook; screenshot capture script (`docs/play/`)
- Romania OSRM + Nominatim Docker pack (`deploy/routing/`); debug builds may use LAN HTTP to that stack
- About / Settings **Report a problem** email (version + last routing error); hostable privacy page `docs/privacy/`; Play listing pack (screenshots, IARC, feature-graphic SVG)
- Room schema export (`app/schemas/`) for v12; debug StrictMode leak/network logs; overlay-menu reflow tests; geofence / jump-skip / backup v3 unit tests
- Settings: TTS verbosity and mute during calls; geofence dwell before auto-arrive; 12h/24h arrive-by clock; default round-trip for new routes; default follow-me on Start; keep screen on only while moving; data saver (no satellite, no OSRM Optimize matrix)
- Settings health check for routing and search; optional `osrm.fallback.url` / `nominatim.fallback.url`; last successful OSRM polyline kept when the network dies; Nominatim 1 req/s plus ~650 ms search debounce; Play listing OSM copy in `docs/PLAY_LISTING.md`
- JSON backup: SAF save/load, optional password (`RPENC1` + AES-GCM), auto-backup to a picked folder (always plaintext), 7-day reminder on Home, selective import + keep-mine vs use-file when LWW would overwrite
- Trash for routes and library Everywhere deletes; copy a stop onto another route; history week/cancelled/name filters + CSV share; trip vacuum older than 30/90 days (never in-progress)
- Room **v12**: proof-of-delivery paths + timestamp, COD amount/collected, stop barcode, route morning/afternoon slot, library favorite, trip distance/late counts, `fuel_log`. JSON backup format **v6** (v1–v5 still import)
- HUD Proof / Scan / Share ETA; optional POD photo + signature on Mark done; COD collect; Play barcode/QR scanner plus typed match (stop, task, geo, plus code, library NFC URI)
- Saved home/depot in Settings (Drive home shortcut + Home chip; optional origin for new routes); Home morning/afternoon + My van filter
- Library favorites in the picker; write NFC tag from a library pin (reader mode only while that dialog is open)
- Menu → Stats (from trip history) and Fuel log; ICS calendar + PDF stop-list share from route export
- Driving vs planning chrome on route detail; merged Add overlay; taller Done; dismissible first-run checklist; reduce motion; high-contrast pins; home widget; app shortcuts; speak-turns Quick Settings tile; notification Skip/Call; FGS location explainer
- Library tags/zones, plus codes (offline Open Location Code), optional What3Words **text** (no W3W API), usage-based recent/frequent picker, merge nearby duplicate pins, per-place default tasks and geofence radius
- Night map style (CARTO Dark Matter) instead of inverted daytime streets; north-up follow; scale bar and N; spider overlapping pins; optional walking last-mile; dashed curb-to-door; reroute calm/normal/sharp; in-app reload map
- Reverse geocode on long-press; hard “near me” search; saved searches; pick a contact as a search; copy lat/lng + plus code
- Library map clusters (no extra JS library). After `map.html` changes: reinstall or clear app data
- Import CSV into an existing route; export one route as CSV; share library/route as GPX, KML, or GeoJSON
- Room v11: library place fields, `library_default_tasks`, `saved_searches`. JSON backup format **v5** (v1–v4 still import)
- Start a route from current GPS as origin **without** a list row (**I’m here**)
- Home park card: next stop, distance, Continue, and Mark done (disabled when required tasks block); Resume when the trip is paused
- Failed-delivery reasons (not home, refused, closed, other) — skips required tasks — plus optional photo / signature
- Reschedule the current stop (end of route, +1 hour, arrive-by, or a calendar date)
- Task templates (COD, ID check, photo of door by default) in Settings; apply to one stop or remaining stops
- Pause trip (keeps the session; stops guidance / FGS / TTS / geofence)
- Vehicle profile (car / bike / walk) and avoid tolls / motorways (OSRM `exclude=`; public demo may ignore)
- Geofence vibrate / beep; opt-in volume-key Done when parked and arrived
- Break / lunch stops, fixed-order flag, per-stop phone / door code / call, bulk complete remaining, undo Mark done (~10 s), finish required tasks later (logged)
- Home Today / All / Archived, route color / van / shift, archive
- Room v10: route origin / color / van / shift / archived; stop windows, break, phone, evidence paths. JSON backup format **v4** (v1–v3 still import)

### Changed
- Map WebView is destroyed when the Compose map leaves composition (debug leak checks)
- Release builds minify with R8 (JS bridge + Room keep rules). Crashlytics uploads the R8 mapping file when `app/google-services.json` is present; Analytics and Auth are excluded
- OSRM 429/503 and timeouts use distinct straight-line banners instead of a generic roads-unavailable message
- Settings splits JSON backup (full restore) from CSV (stop list only)
- HUD, lock screen, notification, and Home use the same **Mark done** label; notification omits Done when required tasks block
- Optimize keeps fixed-order stops in place and reorders movable stops if a promised window would be missed
- While moving, HUD actions enlarge and search / library / edit chrome hide until parked
- Dark Map/Driving uses CARTO night streets (not inverted HOT). After `map.html` changes: Reload map in Tip hartă / Settings, or reinstall / clear app data
- Planning chrome (search/library/edit) vs driving chrome (layers + Recenter + HUD). Add stop during a trip is an overlay from the HUD, not extra FABs
- Lock HUD and navigating labels use muted text instead of orange flash; Romanian driving copy sounds more like van talk (Gata / Termină / Continuă cursa)

## [0.5.0] — 2026-08-24

Local restore point before `remoteId` means cloud. Data stays on-device.

### Added
- Debug-only developer account (Account screen, debug builds only) with an on-screen GPS joystick, speed presets, and jump to map center / next stop. Release builds are no-ops and sign the leftover session out.
- Drag-and-drop stop reorder on route detail and edit; move-to-top / move-to-bottom (up/down buttons stay)
- CSV stop import and **export** in Settings (library as `name,latitude,longitude,address,notes`)
- Privacy policy link in About and Settings (`docs/PRIVACY_DRAFT.md` on GitHub)
- Lock-screen / always-on nav HUD (next stop, distance, maneuver; Done / End / Open map). No map WebView on the lock screen
- Ongoing driving notification while a trip is active, with Done / End actions; tap returns to the trip
- Status-bar chip (Android 16 Live Updates) showing remaining distance during a trip
- Spoken turns continue in the background for the active trip
- Home search / long-press: Drive, save to library, add to an existing route, or start a new route from that pin
- Confirm before deleting a route; Start and Open in Maps on home cards
- App menu from every screen (overlay on the app root)
- Preview the path to the next stop without starting delivery; Run again after a finished route
- Jump ahead: go to a later stop without auto-completing the ones before, or mark them done
- Add stops from search or library while a delivery is running
- Speak turns (Settings, on by default) for the whole active trip, including background
- Library filter and add-to-route; pin stops on the edit-route map; save a one-off drive as a route
- Drive to a place without creating a route (search, long-press, or library). The path is forgotten when you arrive or cancel, and kept only if the app is closed before then
- Trip history (routes and one-off drives): stops reached, time, resume or open again
- Back-to-driving control from Settings, library, and other screens so an active trip is not lost
- Round-trip routes (return to the first pin after the last stop)
- Promised arrival time and minutes on site per stop, with a late warning on the next stop
- Remaining distance and a rough ETA on home route cards
- Auto-arrive geofence: entering a stop’s radius during delivery can mark it visited or done (app default, per-route override including off, optional per-stop radius)
- Reverse remaining stops, sort remaining by promised time, and open remaining pins in Google Maps as a multi-stop drive
- Per-stop ETA on the route list; home cards warn when remaining stops look late
- JSON backup format v2 includes trip history (`remoteId` merge). In-progress trips import as cancelled so they do not steal a live session. Format v1 still imports.
- Optional Play upload signing from gitignored `keystore.properties`
- Optional Firebase Crashlytics when `app/google-services.json` is present (file stays gitignored)
- `osrm.base.url` / `nominatim.base.url` in `local.properties` so production routing/geocoding are not hardcoded public demos

### Fixed
- Return-to-driving pops back to the trip with Home underneath, so Back leaves the drive instead of reopening Settings / the previous overlay screen
- Android 16 Live Update chip: posted as its own promoted notification (not the foreground-service one), requests `POST_PROMOTED_NOTIFICATIONS`, and calls `setRequestPromotedOngoing` / `setShortCriticalText` directly. The shade Driving notification still shows if the chip is unsupported.

### Changed
- App version set to `0.5.0` (`versionCode` 6)
- “Replace this drive?” names a saved route instead of a generic “Back to driving” label
- Starting Drive here during another trip asks to replace it instead of silently keeping the old one
- Keep screen on covers the whole active trip, not only while a turn is showing
- Speak turns covers the whole active trip, including background
- Map / Driving streets use Humanitarian OSM (HOT) tiles (thick colored roads) instead of Esri World Street Map; satellite and terrain stay Esri
- Optimize uses nearest-neighbor then haversine 2-opt, then OSRM duration-matrix 2-opt when remaining pinned stops are ≤ 20 (falls back to haversine if the table call fails)
- Settings copy distinguishes JSON backup (full restore) from CSV (stop list only)
- Arrive-by and minutes on site can be set while editing a route; share text includes those planning fields

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

[Unreleased]: https://github.com/CiobanDaniel/route-planner-android/compare/v0.5.0...HEAD
[0.5.0]: https://github.com/CiobanDaniel/route-planner-android/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/CiobanDaniel/route-planner-android/compare/v0.3.1...v0.4.0
[0.3.1]: https://github.com/CiobanDaniel/route-planner-android/compare/v0.3.0...v0.3.1
[0.3.0]: https://github.com/CiobanDaniel/route-planner-android/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/CiobanDaniel/route-planner-android/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/CiobanDaniel/route-planner-android/releases/tag/v0.1.0
