# Product backlog and idea log

Saved menu of possible work (from the 2026-08-24 planning pass). **Not a commitment.**
Status: `[x]` done in the tree, `[ ]` still open, `[-]` skipped / later / needs a device or an account.

Related: [`DEV_STATUS.md`](DEV_STATUS.md) (protected behaviors + session log), [`CHANGELOG.md`](../CHANGELOG.md), [`README.md`](README.md) (what belongs on GitHub vs this PC).

**Current cut:** local **0.5.0** (`versionCode` 6). Data stays on-device. Do not treat `remoteId` as cloud.

---

## What is already in the app (progress snapshot)

Local courier loop on device:

- Routes and stops (Room), map-first Leaflet WebView, floating islands; languages EN, RO, FR, DE, IT, ES, PT
- Stop library as source of truth; routes reference `libraryStopId`; edit/delete global vs this-route
- Per-stop task checklists; required tasks gate complete / skip / jump
- Delivery: GPS-gated start, OSRM turn-by-turn, last polyline cache, then straight-line fallback + Retry
- Drive here (no route), trip history, return-to-driving (`popUpTo` Home)
- Background FGS trip, shade notification, Android 16 Live Update chip (separate notify)
- Lock-screen HUD (no Leaflet)
- Drag-and-drop reorder, move top/bottom, optimize (NN + haversine 2-opt + OSRM table ≤ 20)
- JSON backup **v6** (routes, library, tasks, templates, history, van-day, POD/COD, fuel log); SAF save/load; optional password (`RPENC1` AES-GCM); auto-backup folder (plaintext); Trash (routes/library); history CSV
- Round-trip, arrive-by (time or calendar date), service minutes, geofence auto-arrive, reverse / sort by time
- Preview path, jump ahead, run again, TTS for the whole trip, keep screen on
- Theme / units / map style remembered; About + OSM attribution; privacy draft + hostable `docs/privacy/` page; Report a problem email
- Account screen groundwork (no real auth); debug-only GPS joystick
- Configurable OSRM/Nominatim via `local.properties` (plus optional fallback URLs); Settings health check; optional Crashlytics **only if** `google-services.json` exists
- Upload signing from gitignored `keystore.properties`; R8 minify on release
- GPS origin on the route (no list row); I’m here; failed-delivery reasons + optional photo/signature; reschedule including a date; task templates
- Home park / next-stop card (Continue + Done / Resume when paused); unified **Mark done** copy; Done disabled when required tasks block
- Pause trip, vehicle profile, avoid tolls/motorways (self-hosted OSRM), Today/All/Archived, route color/van/shift, archive
- Night / dark streets use CARTO Dark Matter (not inverted HOT); north-up follow; scale bar; last-mile walk + dashed curb-to-door; reroute calm/normal/sharp
- Power-user Settings: TTS verbosity + mute on calls, geofence dwell, 12h/24h arrive-by, default round-trip / follow-me, keep-awake only while moving, data saver

**Still not done (ops):** device smoke-test on a phone, enable GitHub Pages + counsel pass, Play Console **clicks** (upload AAB, tester emails, IARC Submit, screenshot PNG upload), run `deploy/routing` on a VPS + HTTPS, commit/tag of 0.5.0. Firebase Auth/Analytics stay out (Gradle excludes them).

---

## Bucket 1 — Prove and fix what 0.5 claims

- [ ] Full **device smoke-test** (heading, islands, GPS-gated start) — you run this on a phone
- [x] **Android 16 chip**: separate promoted notification + `POST_PROMOTED_NOTIFICATIONS` (OEM may still omit the chip)
- [x] **Lock-screen HUD**: SCREEN_OFF, keep-awake, Done / End / Open map, no map on lock
- [x] **JSON backup round-trip tests** (library links, tasks, round-trip, geofence, history; in-progress → cancelled)
- [x] **CSV vs JSON** copy in Settings (two sections; JSON = restore, CSV = stop list)
- [x] **OSRM 429 / timeout** vs generic failure; honest straight-line banner + Retry
- [x] Road-aware optimize (OSRM table, ≤ 20 remaining pinned; haversine fallback)
- [x] **R8 minify** on release + keep rules for WebView JS bridge and Room entities
- [ ] Commit/tag **0.5.0** — only when you ask

---

## Bucket 2 — Van-day refinements

- [x] **Start from current GPS as origin** without a library pin or list row (route-level `originLatitude` / `originLongitude`; not an `isOrigin` stop row)
- [x] **Park / next-stop card** on Home (next stop, distance, Continue, Done)
- [x] **Unify Done** copy (`nav_mark_done`); disable when required tasks block (HUD / lock; notification omits Done and shows the task text). Romanian HUD still uses “Gata” vs geofence “Marchează făcut”
- [x] **Failed-delivery reasons** (not home, refused, closed, other) — bypasses required tasks
- [x] **Reschedule stop** (end of route / +1 hour / pick arrive-by)
- [x] **Task templates** (COD, ID check, photo of door; apply to stop or remaining stops)
- [x] One-tap “I’m here” that does not auto-complete on Start
- [x] Failed-delivery **photo** / signature
- [x] Reschedule to a **calendar date** (stops store `arriveByEpochMs`)
- [x] Pause trip (keep session, stop FGS/TTS/geofence) vs End
- [x] Haptic / beep on geofence arrive
- [x] Volume-key / media-button Done (opt-in, parked, arrived, tasks not blocked)
- [x] Bigger Done / Skip while moving; hide edit chrome until parked
- [x] **Don’t-optimize-this-stop** / fixed-order flag
- [x] Time windows: after optimize, **repair** an order that would be late (haversine ~40 km/h); not a hard constraint inside NN / 2-opt / OSRM table
- [x] Break / lunch stop that is not a delivery
- [x] Vehicle profile (car / bike / walk) for OSRM
- [x] Avoid tolls / motorways (needs self-hosted OSRM flags)
- [x] Open next stops in Maps (up to 3) / Waze to the **first** remaining stop
- [x] Share live next-stop SMS from HUD
- [x] Today vs all routes filter
- [x] Route color / van / shift name
- [x] Archive / hide finished routes
- [x] Undo mark-done (~10 s)
- [x] “Complete remaining tasks later” exception with a log
- [x] Per-stop phone / door code / structured fields / call button
- [x] Bulk complete remaining (with confirm)
- [x] Haptic on pin drop

---

## Bucket 3 — Library and places

- [x] Tags / zones
- [x] Recent / frequent at top of picker
- [x] Merge duplicate library pins
- [x] What3Words / plus codes field (W3W is typed text only; plus codes decode on device)
- [x] Reverse geocode on long-press
- [x] Hard “near me” search toggle
- [x] Saved searches
- [x] Library map clusters (library screen, ≥12 pins, zoom < 15)
- [x] Per-library default tasks and geofence radius
- [x] Import CSV into an **existing** route
- [x] Export **one route** as CSV
- [x] GPX / KML / GeoJSON
- [x] Contacts → search prefills; pick a geocoded result to add the stop
- [x] Copy coordinates / plus-code share

---

## Bucket 4 — Navigation and map

- [x] Larger next-next maneuver while moving
- [-] Speed vs limit (needs another data source)
- [x] Reroute aggressiveness slider
- [-] Offline tiles for a city bbox
- [-] Offline routing (device graph)
- [x] Night driving style that is not inverted streets
- [-] 3D buildings / pitch (fights leaflet-rotate — skip until heading is proven)
- [-] Live traffic
- [x] Scale bar / clearer north when follow is off
- [x] Pin collision when two stops share a building
- [x] Building entrance polish: OSRM street snap + optional walk last-mile + dashed curb-to-door when snap > 18 m (no separate entrance model)
- [x] Walking last-mile toggle
- [x] Fatter recenter / follow-me while driving
- [x] Keep map **north-up** option
- [x] Compass calibration hint
- [x] In-app “reload map assets” after `map.html` changes
- [-] Revisit MapLibre (ADR 0001) only after heading is green on a phone

---

## Bucket 5 — UI / UX / design

- [x] Driving vs planning chrome (two layouts, same data)
- [x] One-handed reach: Done/Skip lower
- [x] Fewer overlapping FABs on detail
- [x] First-run checklist / onboarding that can be dismissed
- [x] Reduce motion
- [x] Font scale: bottom island max height scales (capped); not an app-wide huge-text pass
- [x] TalkBack labels on primary chrome; **not** every nested FAB/icon
- [x] Landscape: 360 dp Home rail when width ≥ 700 dp (map still full-bleed)
- [x] Tablet / wide overlay: side map islands (not a cloud dispatcher); driving HUD stays bottom for reach
- [x] High-contrast / color-blind pins
- [x] Calmer driving HUD (less orange flash)
- [x] Romanian courier slang pass
- [x] More languages (EN, RO, FR, DE, IT, ES, PT in Settings). Extra locales later if needed.
- [x] Home-screen widget: next stop + Done
- [x] App shortcuts (last route, **drive home** when home is saved, library)
- [x] Quick Settings tile toggles **speak turns** (not keep-awake)
- [x] Notification Skip / Call actions
- [x] Predictive back polish
- [x] Edge-to-edge / cutouts on lock HUD
- [x] In-app explanation before FGS location

Drive-home shortcut uses the depot saved in Settings (Bucket 6). Shortcuts also include library, new route, and last opened route. QS tile toggles speak turns.

---

## Bucket 6 — New product surfaces

- [x] Proof of delivery (photo + timestamp + optional signature)
- [x] Barcode / QR → stop or task (Play scanner + typed paste)
- [x] COD amount + collected
- [x] Customer SMS / call from the stop (system Intent)
- [x] ETA text to customer (share sheet)
- [x] Multi-route day plan (morning + afternoon queue)
- [x] Dispatcher mode (local van name filter on Home; still no cloud)
- [x] Tablet / wide overlay (side panes; not a live cloud dispatcher map)
- [x] Stats from history (stops/hour, km, late)
- [x] Fuel / km log
- [x] Saved home / depot origin
- [x] Favorites / drive home
- [x] Optional calendar export for promised times
- [x] Print / PDF stop list
- [x] NFC tag at a regular client (write while the library dialog is open; tap-to-open via NDEF URI)
- [-] Bluetooth scanner
- [-] CarPlay / Android Auto (later product)

---

## Bucket 7 — Data / backup (sync rehearsal, no accounts)

- [x] SAF file save/load for JSON (not only share-sheet text)
- [x] Auto backup to a user-picked folder
- [x] Backup reminder if no export in 7 days
- [x] Selective import (route / library / history)
- [x] Conflict UI when LWW would overwrite **routes / library** (stops on “Use file” still overwrite)
- [x] Soft-delete / trash UI for **routes and library** (`deletedAtEpochMs`; route stops are still hard-deleted)
- [x] Duplicate a stop onto another route
- [x] History filters (week, cancelled, name search — not a route picker)
- [x] Export history CSV
- [x] Encrypted backup password (manual save/share; auto-backup folder stays plaintext)
- [x] Vacuum / delete trips older than 30 or 90 days (never in-progress)

JSON backup **v6** (this increment): POD/COD/barcode, shift slot, library favorite, trip km/late, fuel log. v1–v5 still import. Origin stop rows from older files are promoted onto the route then dropped.

---

## Bucket 8 — Routing / geocoding ops

- [x] Self-hosted or paid OSRM + Nominatim (set `local.properties`)
- [x] Settings health check (routing OK / search OK)
- [x] Second fallback base URL
- [x] Cache last successful polyline when the network dies
- [x] Nominatim 1 req/s + search debounce
- [x] OSM attribution in the Play listing

---

## Bucket 9 — Settings / power user

- [x] TTS verbosity / mute during calls
- [x] Geofence **dwell** time
- [x] 24h vs am/pm for arrive-by
- [x] Default round-trip for new routes
- [x] Default follow-me on/off for driving
- [x] Keep screen on only while moving
- [x] Data saver (no satellite, no matrix optimize)
- [x] Developer GPS sim stays **debug-only**

---

## Bucket 10 — Quality / internals

- [x] More tests: geofence, jump/skip (`completeStopsBefore` / `moveStopToEdge`), backup v3
- [x] UI tests for overlay menus (sibling Y does not move; vanilla `Application`)
- [x] StrictMode / WebView leak checks (debug StrictMode log; WebView `destroy` on release)
- [x] R8 mapping + Crashlytics deobfuscation when Firebase exists
- [x] Room `exportSchema = true`
- [x] Split stop row / dialogs out of `RouteDetailScreen` (the screen is still large)
- [x] Do not rewrite heading math or island overlay rules for cleanliness

---

## Bucket 11 — Play / privacy / crashes (no user accounts)

Repo pack is in tree. **You** still enable GitHub Pages, run counsel, and **click Console** (tester emails, IARC Submit, upload PNG/screenshots/AAB).

- [x] Internal testing runbook + tester CSV (`docs/play/INTERNAL_TESTING.md`) — adding emails is still Console
- [x] Phone screenshot shot list + `capture-screenshots.ps1` — you still capture ≥2 PNGs on a device
- [x] Content rating answer sheet (`docs/play/IARC.md`) — Submit is still Console
- [x] Feature graphic **1024×500 PNG** (`docs/play/feature-graphic.png`) — upload in listing
- [x] Hosted privacy URL + counsel — `docs/privacy/index.html` for Pages `/docs`; counsel still required before collecting emails
- [x] Crashlytics only (drop `app/google-services.json` locally; no Analytics, no Auth)
- [x] Play Vitals without Firebase — documented; Vitals work from Internal testing with no Firebase
- [x] In-app “report a problem” email with version + last routing error

---

## Bucket 12 — Cloud / accounts (later)

- [ ] Auth (Google / Entra / magic link)
- [ ] Sync library + routes; conflict UI better than LWW
- [ ] Share a route or stop (read vs edit)
- [ ] Team library
- [ ] Live dispatcher map

Do **not** start until buckets 1–2 and backup round-trip feel boringly reliable. That is when `remoteId` starts meaning cloud.
