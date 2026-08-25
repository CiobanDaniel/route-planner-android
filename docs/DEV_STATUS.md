# Development status & agent continuity log

**Purpose:** Survive context resets. Read this **before** changing map heading, floating UI chrome, Room schema, or delivery flow.
**Last updated:** 2026-08-25
**Branch context:** `feature/courier-street-contrast` — **0.5.0** local cut. Play Console listing, hosted privacy URL, and production OSRM/Nominatim still need your accounts.

**Implementation decision (2026-08-05):** This Kotlin/Android repository is the active product implementation. The Expo/React Native repository is retained only as an earlier prototype/reference and must not receive parallel feature work.

---

## How to use this file

1. **Before editing a sensitive area**, check the matching section under [Do not regress](#do-not-regress--protected-behaviors) and [How it works](#how-it-works-by-area).
2. **After finishing a meaningful change**, append a short entry to [Session / change log](#session--change-log) and update [Next](#next--prioritized-backlog).
3. Prefer **modular, overlay-safe UI** (see [Modularity rules](#modularity-rules-ui)). Do not put expandable menus inside height-wrapping `Row`/`Column`/`Box` that also holds sibling chrome.

Related docs: `AGENTS.md`, `CHANGELOG.md`, [`docs/README.md`](README.md) (what belongs on GitHub), `docs/adr/0001-map-webview-leaflet.md`, `docs/MANUAL_SETUP.md`.

---

## Product snapshot

| Area | Status |
|------|--------|
| Local routes / stops (Room) | Done |
| Map-first UI (Leaflet WebView) | Done |
| Delivery mode + OSRM turn-by-turn | Done |
| Address search (Nominatim) | Done |
| Settings (theme / language / units / keep screen on / speak turns) | Done — EN, RO, FR, DE, IT, ES, PT; TTS verbosity, mute on calls, geofence dwell, 12h/24h, default round-trip/follow, keep-awake-while-moving, data saver |
| Delivery session persist/resume | Done — route + one-off path; Return-to-driving overlay |
| Background trip + notification / status-bar chip | Done — FGS shade notification; Android 16 Live Update is a **separate** promoted notification + `POST_PROMOTED_NOTIFICATIONS` |
| Trip history | Done (Room v12; JSON backup **v6** includes it) |
| Stop library (device-local) | Done — canonical stops; routes reference `libraryStopId` |
| Per-stop task checklists (local) | Done |
| Kotlin/Android client | Active implementation |
| Accounts / cloud sync / share library | Groundwork (session store, remoteId, JSON backup v6); Google/Auth TBD. Account UI already has Google sign-in strings (disabled). Debug-only developer tools (joystick GPS sim) |
| Play signing / Crashlytics | Repo-ready: `keystore.properties` signs release; Crashlytics if `google-services.json` is present (no Analytics/Auth). Play Vitals need no Firebase. Listing files in `docs/play/` (PNG graphic, IARC sheet, tester runbook) — Console clicks still you |
| Privacy policy URL (in-app) | Done — About + Settings. Default GitHub draft; override `privacy.policy.url`. Host `docs/privacy/index.html` on Pages; counsel before collecting emails |
| Routing / geocoding bases | Done — `local.properties` + optional fallback. Romania Docker pack in `deploy/routing/`. Public demos remain Gradle defaults until you set URLs. |

**Stack:** Kotlin, Compose, Material 3, Room, DataStore, Leaflet in WebView, OSRM, Nominatim. Package `com.danielcioban.routeplanner`. **Gradle → JDK 21** (not Studio JBR 25).

---

## Do not regress — protected behaviors

These were fixed after painful iteration. **Do not “simplify” without device re-test.**

### 1. Floating islands must not reflow each other

**Symptom that was fixed:** Opening “Tip hartă” / menus pushed FABs and other islands down.

**Rules:**

- Expandable UI (map layers, app menu, pickers) must be a **Dialog**, **Popup**, or sibling on a **`Box(Modifier.fillMaxSize())` root** with `align(...)`.
- Never put an expanding panel **inside** a height-wrapping `Box { Row { … }; if (open) Menu }` — the menu grows the Box and shoves content below.
- `MapLayersButton` is **button only**; menu is `MapLayersMenuDialog` via `MapLayersMenuHost` / `MapChromeState`.
- App hamburger is `AppMenuOverlay` on the **app** root `Box` in `RoutePlannerApp` (not inside a screen’s height-wrapping chrome). Tap scrim or Back closes it.
- Return-to-driving must `popUpTo` the home list so Back from the trip is Home, not Settings/library. Do not `navigate` the drive screen on top of the overlay you left.
- `FloatingIsland` (and the attribution chip) must `blockMapPassThrough` so island chrome is a hit target; the WebView map must not pan/zoom through empty island area. **Do not consume leftover pointer events** — that steals list scrolling.
- Nested round buttons inside an island use `FloatingCircleButton(embedded = true)` — no second drop shadow.
- Lists inside islands need a **bounded height** (`heightIn` + `LazyColumn(Modifier.weight(1f))`) so they actually scroll.
- Bottom map islands that can hide the map use `CollapsibleBottomIsland` on phones: grab-bar drag only (not the list), height change absorbed by the weighted spacer, no expanding menu inside the sheet.
- At width ≥ 700 dp, map sheets use `AdaptiveMapSheet` / `AdaptiveSheetSlot` (`ui/layout/AppPanes.kt`): a **full-height side `FloatingIsland`** aligned `CenterEnd` in a **fillMaxSize Box** (same overlay rule as Home’s rail). Driving HUD stays a bottom island (thumb reach); remaining-stops rail only when there is room beside the HUD. Jump/skip still open existing dialogs — no expanding menu in the pane.

**Key files:** `ui/map/MapLayersButton.kt`, `ui/map/MapChromeState.kt`, `ui/components/FloatingIsland.kt`, `ui/routes/RouteListScreen.kt`, `ui/routes/RouteDetailScreen.kt`.

### 2. User heading triangle (compass / follow-me)

**Hard lessons:**

- Stale **GPS course** while stationary locks heading north — prefer **remapped compass** when not moving; GPS course when `speed ≥ ~1.4 m/s`.
- leaflet-rotate puts **`markerPane` on `norotatePane`** (screen-fixed markers). Tip CSS must be **relative to map bearing**.
- Geographic heading (compass + GPS course) is clockwise from north. leaflet-rotate’s `setBearing` is the **opposite** sense — invert **only** in `map.html` `applyMapBearing()` as `(360 - heading) % 360`. Do not also invert Kotlin.
- Current tip formula in `map.html` `updateUserMarkerVisual()`:

  ```js
  angle = (lastBearing + mapBearing + 360) % 360
  ```

  (`lastBearing` is geographic heading; `mapBearing` is Leaflet’s inverted bearing.) Follow-me keeps the chevron pointing up; north-up / paused follow still tracks the phone. Do not flip this without a device test (flat + upright).
- Follow-me **centers on the user marker**. Do not `setView` a look-ahead point — leaflet-rotate pivots around map center, so an offset makes you orbit a nearby point.
- Recompute tip on map **`rotate` / `rotateend` / `moveend`** (`onMapRotated`) so manual map twist does not desync the chevron.
- Avoid animated `panTo` while calling `setBearing` — they race and snap north. Prefer `setView(..., { animate: false })`.
- Compass: `RememberDeviceBearing` remaps for **flat** vs **upright** + display rotation only. After editing `app/src/main/assets/map.html`, **reinstall / clear app data**.

**Key files:** `assets/map.html`, `ui/location/RememberDeviceBearing.kt`, `ui/location/RememberUserLocation.kt`, `ui/location/MergeUserHeading.kt`, `ui/map/RouteMapBackdrop.kt`.

### 3. Map must not stick on Timișoara

- Default Leaflet view is **world** (`setView([20,0], 2)`), not Timișoara.
- `LastKnownMapCenter` seeds instant recenter across screens.
- `RememberUserLocation` applies **lastKnown** first, then balanced current location.
- `RouteMapBackdrop` / `MapWebState.hasFlownToUser`: **first fix always flies** for that WebView instance.
- Empty routes: detail bumps `recenterToken` once when GPS arrives and there are no pins.

**Key files:** `map.html`, `ui/map/LastKnownMapCenter.kt`, `ui/location/LocationHelpers.kt`, `ui/map/RouteMapBackdrop.kt`, `ui/routes/RouteDetailScreen.kt`.

### 4. Delivery start gated on GPS

- Do **not** set `deliveryActive` before navigation can start with a fix.
- Approximate OSRM fallback must show honest banner + Retry (`nav_approx_roads_unavailable`).

### 5. Dark mode elevation

- Light on the **face** (upper-left sheen, clipped to the island). Cast shadow is **lower-right on the map**, not a white rim around the panel.
- Dark theme: **no surrounding light shadow** (`useHighlightShadow = false`) — that reads as fog. A very low-alpha face sheen on the island itself is OK.

**Key files:** `ui/theme/Theme.kt`, `ui/components/FloatingIsland.kt`.

### 6. String resources

- Leading spaces in string XML are stripped by Android. Use `\u0020` (e.g. `maneuver_onto`).
- Always update the English default **and** every shipped locale (`values-ro`, `values-fr`, `values-de`, `values-it`, `values-es`, `values-pt`).

---

## How it works (by area)

### Map (Leaflet WebView)

| Piece | Role |
|-------|------|
| `assets/map.html` | Leaflet + leaflet-rotate, styles, user marker, nav polyline, gestures |
| `RouteMapBackdrop.kt` | AndroidView WebView bridge; `onRelease` destroys the WebView (blank + drop `AndroidBridge`); pushes style, follow, route, user, focus |
| `MapViewMode` | map / driving / satellite / terrain tile ids |
| `MapLayersMenuDialog` | Overlay menu; toggles style + Follow me |
| ADR `docs/adr/0001-map-webview-leaflet.md` | Why WebView instead of native GL maps |

Bridge JS entry points: `setUserLocation`, `setDriveFollow`, `resumeDriveFollow`, `setMapStyle`, `setRoute`, `setNavRoute`, `flyToPlace`, `clearNavRoute`.

### Routes & delivery

| Piece | Role |
|-------|------|
| `RouteEntity` / `StopEntity` / `RouteDao` | Per-route stops; `isCompleted`; optional `libraryStopId` |
| `RouteRepository` | CRUD, reorder, library copy-on-add |
| `RouteDetailScreen` + `RouteDetailStopRow` + `ViewModel` | Map pins, search, library pick, delivery HUD, queue; stop list row is split out |
| `DeliveryHud` / `DeliveryStopQueueSheet` | Nav UI; skip / jump (jump marks earlier unfinished done) |
| `DeliverySessionStore` | DataStore: active route **or** one-off dest for crash resume |
| `TripGuidanceService` | FGS `location`: GPS + OSRM while the trip is active; shade notification; separate Android 16 Live Update chip; lock-screen HUD on SCREEN_OFF |
| `LockScreenHudActivity` | Compose-only always-on HUD (no Leaflet). Done / End / Open map |
| `QuickDriveScreen` | Temporary path to a place; no Room route; forgotten on arrive/cancel |
| `TripHistoryEntity` / history screen | Log of route + one-off trips; resume in-progress |
| `OsrmRoutingClient` + `ManeuverFormatter` | Road route; localize instructions at display time |

Stop creation today: long-press, address search, GPS, or library pick. Pinned stops are created in the library first; the route stores a reference (`libraryStopId`) plus per-route order/completion/tasks. Edit/delete of a shared stop asks global vs this-route (fork). GPS **I’m here** writes route origin coords only (not a list row).

### Stop library (canonical)

| Piece | Role |
|-------|------|
| `StopLibraryEntity` + `StopLibraryDao` | Canonical place (name, address, coords, notes) |
| Room **v5** + `MIGRATION_4_5` | Index on `stops.libraryStopId`; backfill library rows for unlinked pinned stops |
| Menu → Stop library | Map pins + list; usage (“used on Route A, B”); long-press to add |
| Route stop | Reference + `position` / `isCompleted` / tasks |
| Edit/delete scope | Global (all routes) or this-route copy; library delete can remove everywhere |

**Not yet:** accounts, sync, multi-user edit, share library over network.

### Stop tasks (local, working tree)

| Piece | Role |
|-------|------|
| `StopTaskEntity` + `StopTaskDao` | Per-stop tasks: title, required, completed, timestamp, completion note |
| Room **v3** + `MIGRATION_2_3` | Creates `stop_tasks` with FK cascade on stop delete |
| `StopTaskChecklist.kt` | Add/complete/delete tasks in stop-details dialog |
| `RouteRepository.setStopCompleted` | Blocks completion when required tasks remain |
| Delivery skip / queue jump | Same required-task gate via `StopCompletionResult` |

Task completion records reset when route progress is reset (`resetStopTaskCompletionsForRoute`).

### Settings / chrome

- Theme / language (EN, RO, FR, DE, IT, ES, PT) / units / keep screen on → DataStore. Android 13+ also lists those locales via `localeConfig`.
- About screen + OSM attribution chip. **Report a problem** (`ProblemReport`) emails version + last OSRM error. Privacy URL from `BuildConfig.PRIVACY_POLICY_URL`.
- Island UI: `FloatingIsland`, `FloatingCircleButton`, `IslandDialog`, `CollapsibleBottomIsland`.

---

## Modularity rules (UI)

Goal: new features should **compose overlays**, not reshape existing chrome.

1. **Screen shell pattern**

   ```text
   Box(fillMaxSize) {
     RouteMapBackdrop(...)          // full bleed
     Column/Row chrome              // fixed-size controls only
     // overlays: Dialog / align(TopEnd) menus / status chips
     // wide: AdaptiveMapSheet align(CenterEnd) in a weighted fillMaxSize Box
   }
   ```

2. **One job per composable**
   Button ≠ menu. Picker ≠ repository. Formatter ≠ HTTP client. Prefer shared chrome (`MapChromeState`) and heading merge (`MergeUserHeading`) over copy-pasting between list/detail.

3. **No multi-root composables that emit layout siblings into a parent `Column`**
   Wrap in a single `Box` if you must emit Dialog + button from one function (Dialog itself is windowed; still keep one layout root for the anchor). Detail dialogs live in `RouteDetailDialogs.kt`; stop list rows in `RouteDetailStopRow.kt`; delivery HUD phases are private composables in `DeliveryHud.kt`.

4. **State that affects layout height** (messages, banners)
   Prefer `align` overlays over inserting extra islands into a weighted column that shifts the bottom HUD.
   Short GPS/status copy can replace the **title-island subtitle** (same pattern as home) so it does not sit on the title or the right FAB column.
   Islands must `blockMapPassThrough` so WebView pan/zoom does not steal gestures from chrome.

5. **Room / data**
   Prefer additive migrations. Commit Room JSON under `app/schemas/` (`exportSchema = true`). Library is the source of truth for place data; route stops are references plus per-route state (order, completion, tasks).

6. **Feature flags / seams for accounts later**
   Keep library and routes local-first; any sync API should sit beside `RouteRepository`, not inside Compose screens.

---

## Next — prioritized backlog

**Current next is not more features.** See [`PRODUCT_BACKLOG.md`](PRODUCT_BACKLOG.md) for buckets. **Do not start Bucket 12** until you ask.

You still: device smoke-test (below); GitHub Pages + counsel; Play Console clicks; production OSRM over HTTPS; commit/PR when you ask.

The P0–P8 lists under this heading are a **fossil** of 0.3–0.4 (PR #20, etc.). Leave them as history. Unchecked items there that still matter are: phone smoke-test, Play Console clicks, commit/tag when you ask.

### P0 — Stabilize before more features

- [x] Review and commit the current `0.3.1` polish/library working set as a focused baseline before adding unrelated features. Merged to `main` as PR #20.
- [ ] Device smoke-test checklist (below) on a physical phone; confirm heading left/right + after manual map rotate.
- [x] Verify the local baseline with JDK 21: `testDebugUnitTest`, `lintDebug`, and `assembleDebug` pass on 2026-08-05 (`main`); `testDebugUnitTest` + `assembleDebug` also pass on 2026-08-22 (stop-task working tree).
- [ ] If heading still wrong: document the **one** formula that works on that device in this file; stop alternating blindly.
- [x] Push / open PR for current polish (merged to `main` as PR #20).

### P1 — Stop library UX (still local)

- [x] Add-from-library + manage from **Edit route** as well as detail.
- [x] Edit library stop fields (name/notes/coords) on library screen.
- [x] Optional: remove a stop from a route without deleting the library entry (already true for copies); make UX obvious.
- [x] Deduplicate / update-from-library when `libraryStopId` is set.

### P2 — Courier polish

- [x] Remaining hardcoded EN strings (EditViewModel errors, external nav helpers, etc.).
- [x] Offline / poor-GPS messaging.
- [x] Stronger empty-route onboarding (“drop first stop”).

### P3 — Stop tasks and completion records (next product increment)

**Goal:** make a stop an operational work item, not only a pin with free-form notes.

- [x] Add an additive Room migration with a per-stop task table: title, required flag, completion state, completion timestamp, and completion note.
- [x] Add a stop-detail task checklist using the existing island/dialog conventions; keep menus as overlays.
- [x] Require all required tasks before a stop can be marked complete, while preserving the existing route/delivery progress behavior.
- [x] Add repository and ViewModel tests for task state transitions and the Room migration.
- [x] Keep this fully local-first. Do not add accounts or cloud sync as part of this increment.
- [x] **Commit + PR** the working tree (Room v3–v5, tasks, library source of truth, launcher, heading/map/UI).
- [x] Bump `versionName` / `CHANGELOG` when shipping (`0.4.0`, `versionCode` 5).

### P4 — Accounts & sharing (after local groundwork feels solid)

- [ ] Auth (e.g. Entra / Firebase / custom) — product decision TBD.
- [x] Account session store + Account screen (preview sign-in on debug builds; metadata only, no tokens).
- [x] Stable `remoteId` on routes/stops/library/tasks (Room v4) + `deletedAtEpochMs` tombstone column.
- [x] JSON backup export/import in Settings (merge by `remoteId`; rehearsal for sync payloads). Format v2 includes trip history.
- [x] ADR `docs/adr/0002-local-first-sync.md` (LWW merge, local-first rules).
- [ ] Sync routes + stop library to server; conflict policy beyond LWW.
- [ ] Share stops/routes with edit permissions.
- [ ] Do **not** start server sync until P0 heading + layout stay green on device.

### P5 — Release / ops (manual)

- [x] Upload-key `signingConfigs` from gitignored `keystore.properties` (Play App Signing still enabled in Console).
- [x] Feature graphic PNG, IARC answer sheet, Internal testing runbook, screenshot capture script (`docs/play/`). Console upload / Submit / tester emails still you.
- [ ] Play Console **clicks**: upload AAB, tester emails, IARC Submit, screenshot PNG upload.
- [x] Romania OSRM + Nominatim Docker pack (`deploy/routing/`, `docs/SELF_HOST_ROUTING.md`). You still run prepare + HTTPS on a VPS for Play/release.
- [x] Play listing privacy: in-app link + hostable `docs/privacy/index.html`. Enable Pages + counsel still manual.
- [x] Crashlytics Gradle wiring (applies only when `app/google-services.json` exists; Analytics/Auth excluded; release mapping upload on). Create the Firebase app and drop the file in.
- [x] Branch protection / release checklist (`docs/RELEASE_CHECKLIST.md`).
- [x] README feature list updated for library, tasks, drive-here, CSV, HUD, privacy link.

### P6 — Remaining polish (non-blocking)

- [x] Localize OSRM fallback instructions in `OsrmRoutingClient.formatInstruction` (still English; display path uses `ManeuverFormatter` when OSRM returns structured steps). Removed duplicate English formatter; HUD uses `ManeuverFormatter` only.
- [x] Replace hardcoded `"GPS stop"` default in `RouteDetailViewModel.addStopAtCurrentLocation` with `R.string.gps_stop_default_name`.
- [x] `testDebugUnitTest` + `assembleDebug` + `lintDebug` pass on 2026-08-22 (stop tasks + Robolectric tests).

### P7 — Courier tools (local)

- [x] Persist preferred map style (map / satellite / terrain); restore it after driving follow.
- [x] Home list: filter routes, progress bar, approximate distance, duplicate route.
- [x] Nearest-neighbor stop order (detail + edit). Completed stops stay first; unpinned stay last.
- [x] High-contrast HOT streets for Map / Driving (Leaflet raster; Esri sat/terrain; no MapLibre).
- [x] 2-opt after nearest-neighbor (straight-line; round-trip optional).
- [x] 2-opt / road-aware optimize (OSRM table durations, remaining pinned ≤ 20; haversine fallback).
- [x] CSV stop import **and export** (Settings). JSON backup vs CSV is explained in Settings.
- [x] Per-stop promised arrival + minutes on site; remaining ETA on home/detail.
- [x] Persist last map center+zoom independently of GPS fly.
- [x] Reverse remaining stops; sort remaining by promised time; open remaining in Google Maps (multi-stop).
- [x] Auto-arrive geofence (visited / complete), app default + per-route off/override, per-stop radius.
- [x] Drive to a library/search/map pin without creating a route (temporary path; persist only if the app is killed mid-drive).
- [x] Trip history log + resume; Return-to-driving overlay when leaving the HUD for Settings/library/etc.
- [x] Looser map-hub UX: place actions, menu on every screen, preview path, jump choice, run again, TTS (foreground).
- [x] Ongoing driving notification + Android 16 status-bar chip; trip stays alive in the background.
- [x] Drag-and-drop stop reorder (up/down stay) + move to top/bottom on detail and edit.
- [x] CSV stop import (Settings) → new route + library stops; CSV export of the library.
- [x] “Replace this drive?” shows the saved route name when the active trip is a route.
- [x] Lock-screen / always-on nav HUD (Compose; no map WebView on the lock screen).

### P8 — Van-day (local, in tree)

- [x] R8 minify on release + JS bridge / Room keep rules.
- [x] OSRM 429 / timeout vs generic failure (honest straight-line banner).
- [x] Settings: JSON backup vs CSV as two sections.
- [x] GPS origin as route-level coords (no list row, no library pin); **I’m here** does not auto-complete on Start.
- [x] Home park / next-stop card (Continue + Mark done; Resume when paused).
- [x] Unify Mark done copy; disable / omit when required tasks block (HUD, lock, notification, Home).
- [x] Failed-delivery reasons (bypass required tasks) + optional photo/signature; reschedule including a calendar date; task templates.
- [x] Pause trip (session stays, FGS/TTS/geofence stop); haptic/beep on geofence; opt-in volume-key Done when parked and arrived.
- [x] Bigger HUD actions while moving; hide search/library/edit until parked.
- [x] Fixed-order flag; hard time-window repair on optimize; break/lunch stops; vehicle profile; OSRM exclude flags.
- [x] Open next 3 (Maps + Waze); SMS next stop; Today/All/Archived; color/van/shift; archive; undo Done (~10 s); defer required tasks with a log; phone/door/call; bulk complete remaining; haptic on pin drop.
- [ ] Device smoke-test of the above on a phone.
- [ ] Commit/tag when you ask.

---

## Device smoke-test checklist

Run after any change to `map.html`, bearing, or map chrome:

1. Cold start → map leaves world view quickly (lastKnown / GPS), not stuck on a random city.
2. Open **empty** new route → centers on you, not a default city.
3. Follow me / delivery: turn phone left/right — triangle matches (flat on table and upright).
4. Manually twist the map with two fingers — triangle still matches phone heading.
5. Open Tip hartă — other islands/FABs **do not move**.
6. Open hamburger menu — routes list does not jump down; tap scrim or Back closes it. Hamburger and Tip hartă must not both be open.
7. Drag / tap on an island (title, route list, HUD) must not pan the map underneath.
8. Dark mode: islands have a **lower-right** drop shadow on the map, **no surrounding white halo**. A slight sheen on the island face is OK.
9. Add stop → save to library → other route → add from library.
10. Start delivery only works with GPS; OSRM fail shows approx banner + Retry.
11. Empty route → onboarding card (search / library / long-press hint); centers on GPS when available.
12. Stop details → add required task → cannot mark stop done / skip / jump until task complete.
13. Reset route progress → task completion records clear too.
14. Home: filter routes; duplicate opens a copy with tasks and empty progress.
15. Detail / Edit: Optimize reorders pinned stops; completed stay first. Reverse / By time / Open in Maps stay on the stop list, not inside wrapping chrome.
16. Switch to satellite, leave the screen, come back — style is still satellite. End delivery restores that style, not always Map.
18. Delivery + auto-arrive: walk into a stop radius → Visited (or Done if that mode). Required tasks still block Done. Edit route Off disables it even if Settings is on.
19. Home search / long-press / library Drive here → temporary path, no new route. Arrive or Cancel forgets it; kill the app mid-drive → resume from home card or history.
20. Leave driving for Settings / library / another screen → **Back to driving** chip; home keeps the resume card instead.
21. Menu → Trip history: in-progress resume, completed route opens the route, completed one-off starts a new path to the same dest.
22. Home search / long-press → This place: Drive / Save library / Add to route / New route. Delete a route asks first. Card Start needs GPS; Open in Maps works from the card.
23. If a drive is already running, Drive here asks Replace vs Keep (Keep returns to the current trip).
24. Hamburger from Settings / library / edit / driving — menu overlays the app root; islands do not reflow. Return-to-driving still works.
25. Delivery: search/library/edit FABs hide; HUD **Add stop** opens search or library. Queue jump asks “go only” vs “mark earlier done”. All-done → Run again (GPS still required). Preview path draws a line without entering delivery.
26. Speak turns (Settings, on by default) reads the next maneuver only while the driving screen is in front — not in the background.
27. Library filter + add-to-route. Edit route: search or long-press adds a pin on an empty new route.
28. Start a trip, Home the phone: ongoing **Driving** notification stays; tap it returns to the trip. On Android 16, allow notifications **and** promoted Live Updates if asked. The status-bar chip (remaining distance) should appear after you leave the app — it is not the shade notification. If the OEM has no Live Updates, the shade notification is still the fallback. Notification **Done** marks the stop (required tasks still block). **End trip** clears the session without wiping stop checkmarks. **HUD** (or lock the phone with keep-screen-on) shows the lock-screen nav HUD — Open map returns to the trip. No Leaflet on the lock screen.
29. Detail / Edit: drag the handle to reorder; up/down still work; double-arrow moves to top/bottom.
30. Settings → Import CSV stops with `name,lat,lng` → new route on home + library entries. Export CSV shares the library. JSON backup (not CSV) restores routes, tasks, and trip history. About / Settings privacy link opens the GitHub draft.
32. Start a route, open Settings, **Back to driving**, then Back — you should be on **Home**, not Settings. Delivery is still active (resume card).
33. Debug build only: Account → Sign in as developer → stick moves the user marker; Walk/City speeds; Map center / Next stop teleport. Sign out restores real GPS. Release builds must not show this.
34. Two-phone backup rehearsal: export JSON on phone A, import on phone B (or after clear-data). Library links, tasks, round-trip, geofence, history, GPS origin, failure reason, and task templates should round-trip. An in-progress trip in the file must not become the live session on B.
35. Detail: **I’m here** stores GPS as the route origin (no list row, no library pin). Start delivery does **not** auto-complete a dummy origin stop.
36. During delivery: **Can’t complete** asks for a reason, then optional photo/signature, and marks the stop failed without required tasks. **Reschedule** can pick a calendar date. HUD **Mark done** is disabled (and the shade Done action is hidden) while required tasks are open unless you Fail or explicitly finish tasks later.
37. Home while a trip is active: park card shows the next stop and Continue / Mark done. When paused, Resume restarts guidance. Mark done stays disabled when tasks block.
38. Settings: JSON backup and CSV are separate sections. Task templates seed COD / ID / photo; apply a template on a stop or remaining stops. Vehicle / avoid flags / geofence haptic / volume-key Done.
39. Dark theme Map/Driving (or Night layer): streets are a real dark basemap, not inverted HOT. After `map.html` changes: **Reload map** in layers/Settings, or reinstall / clear app data.
40. Follow me: north-up toggle keeps the map north-up; the blue triangle still tracks the phone. Scale bar and N stay on screen; N is clearer when follow is paused.
41. Two pins in the same building spread slightly. Nav line may dash from the curb to the door; Settings “Walk the last meters” adds a foot path.
42. While moving, Then is larger. Recenter FAB is larger during follow. Low compass accuracy shows a dismissible figure-8 hint.
43. First launch: dismissible checklist. First trip: location-in-background explainer before the foreground service starts. GPS still required to start.
44. During delivery, detail chrome is Recenter + layers (not search/library/edit FABs). HUD **Add stop** opens search/library. Done is a 56 dp thumb row. Notification can Skip (moves current stop to the end) or Call when the stop has a phone.
45. Settings: Reduce motion, high-contrast pins. After pin/map HTML changes: **Reload map**. Home widget shows next stop + Done (Done hidden when required tasks block). Long-press the launcher for Library / New route / last route. QS tile toggles speak turns.
46. Wide (≥ 700 dp): Home route list is a 360 dp right rail that fills leftover height. Other map screens use a 400 dp side island instead of a short bottom sheet. During a trip on extra-wide layouts, Done/Skip stay bottom-left; remaining stops overlay on the right (queue dialog still works). Title islands max 560 dp; dialogs, lock HUD, and Settings-style screens cap around 720 dp. Lock HUD uses safe drawing insets including the punch-hole.
47. Settings: save home/depot, Drive home from Home chip and launcher shortcut. New route can start from that origin when “use as origin” is on.
48. Edit route: morning/afternoon chip, COD amount, barcode. HUD Mark done can ask for photo/signature/COD; Skip proof still requires required tasks. Scan matches a stop, open task, geo pin, or library NFC URI.
49. Library: favorite chip; picker Favorites first. Write NFC only while the write dialog is open, then reader mode unbinds. Tap a written tag opens Drive here to that pin.
50. Menu → Stats / Fuel log. Route export: ICS (promised times) and PDF stop list via FileProvider `cache/exports/`. JSON backup v6 includes `fuelLogs`.
51. Settings: Save file / Share file for JSON (optional password). Import picker + selective sections + keep-mine when the file is newer. Auto-backup folder (plaintext). Home reminder after 7 days. Menu → Trash. History chips + CSV. Copy stop onto another route. Trip vacuum 30/90 days.
52. Settings → Routing and search: Check routing/search. Fallback OSRM/Nominatim URLs. Last road polyline if the network dies (banner, then straight line). Nominatim 1 req/s. Play listing copy in `docs/PLAY_LISTING.md`.
53. Settings: spoken detail + mute on calls; geofence dwell 0/3/5/10 s; arrive-by 12h vs 24h; new routes default round-trip; Start follow-me default; keep screen on only while moving; data saver hides satellite and skips OSRM Optimize matrix. Recenter still turns follow on. GPS-gated start and required-task Done unchanged.
54. Internals: Room schema JSON in `app/schemas/` (v12 forward). Debug StrictMode log-only. Map WebView is destroyed on Compose release. Overlay menus stay Dialog / root `Box` (unit test). Jump-ahead prefix complete lives on the repository.
55. About / Settings: **Report a problem** opens email with version and last OSRM error. Privacy URL from `privacy.policy.url` (default GitHub draft). Play listing pack + hostable privacy HTML in `docs/`. Crashlytics stays optional and Analytics/Auth-free.

---

## Working tree

**0.5.0** on `feature/courier-street-contrast`, plus unreleased Bucket 2–11 work. Play/ops pack: feature graphic PNG, IARC sheet, Internal testing runbook, Romania OSRM/Nominatim Docker. Next: you run `prepare-osrm`, Pages, Console clicks, device smoke-test. **Do not start Bucket 12** until you ask.

---

## Session / change log

### 2026-08-25 — Docs hygiene + gitignore

- Added `docs/README.md`: which planning files belong on GitHub `main` vs stay on this PC. `DEV_STATUS` / backlog / ADRs / Play templates stay in git; captured screenshot PNGs, `testers.csv`, routing extracts, and secrets stay local.
- Snapshot fixes: languages are EN+RO+FR+DE+IT+ES+PT; Room **v12** / backup **v6**; SECURITY supported line is 0.5.x. Fossil P0–P8 left as history; current next is still device smoke-test / Pages / Console / HTTPS OSRM — not Bucket 12.
- Overlay menus / heading math / `map.html` unchanged. Did **not** commit.

### 2026-08-25 — Island lighting (face vs cast shadow)

- Islands/FABs: highlight lives **on the panel** from the upper-left (gradient + lighter rim). Drop shadow is offset lower-right onto the map, softer blur, no white glow behind the chrome. Dark mode still has no surrounding halo.
- Overlay menus / heading math / `map.html` unchanged. `:app:compileDebugKotlin` green. Did **not** commit.

### 2026-08-25 — UI polish (contrast, density, quieter chrome)

- Palette: orange is for CTAs and selection; metadata uses slate/green. Dark muted text is actually secondary. Island borders/shadows and notes banners are cooler, less orange wash. Tonal buttons use a gray fill so the HUD is not a wall of orange.
- Home: each route row shows Start + overflow (Popup) instead of six icon buttons. Menu icons are muted. Account Google copy is one body + privacy, not four stacked paragraphs.
- Driving HUD: secondary actions in one wrapping row of muted links (Skip, Call, Maps, …). Grab bar is quieter. Overlay menus / heading math / map.html unchanged.
- `:app:compileDebugKotlin` and `:app:testDebugUnitTest` green. Did **not** commit, start Bucket 12, or change overlay-safe menu placement.

### 2026-08-25 — Languages: FR, DE, IT, ES, PT + Google-ready copy

- Settings language picker: English, Romanian, French, German, Italian, Spanish, Portuguese. `AppLanguage.fromTag` uses the primary subtag (`pt-BR` → Portuguese). `res/xml/locales_config.xml` + `android:localeConfig` for Android 13+ system per-app language.
- Full `strings.xml` for each locale (same keys as English). EN/RO account copy expanded; Google sign-in / privacy / unlink strings are in every locale. Account screen shows a **disabled** Continue with Google button — no Google Auth SDK, no tokens.
- `:app:compileDebugKotlin` and `:app:testDebugUnitTest` green. Did **not** commit, start Bucket 12, or change overlay-safe menus / heading math.

### 2026-08-25 — Settings accordion + dropdowns

- Settings is grouped into collapsible sections (Display, Navigation, Home, Vehicle, Routing, Auto-arrive, Data, Templates). One section open at a time; headers show a short summary.
- Theme, language, units, clock, TTS, reroute, vehicle, geofence, dwell, and trip retention use a **DropdownMenu** (Popup), not extra rows in wrapping chrome. Back/menu FABs stay in the title row.
- `:app:compileDebugKotlin` green. Did **not** commit, start Bucket 12, or change overlay-safe map menus / heading math.

### 2026-08-25 — Overlay-safe tablet / wide layout

- Shared breakpoints in `AppPanes` (700 dp wide, 360 dp Home rail, 400 dp map sheets, 440 dp HUD, 560 dp title chrome, 720 dp readable). Side panes are Box overlays (`align` + `fillMaxHeight`), not extra height inside FAB chrome.
- Home rail list fills leftover height. Route detail / library / edit / history / trash / fuel use a side island on wide screens. Driving HUD stays bottom (one-handed); remaining-stops rail only when HUD + list both fit. Jump/skip still use existing dialogs. Empty lists wrap instead of a tall blank pane.
- Settings, About, Account, Stats, IslandDialog, lock HUD: content capped around 720 dp, centered or start-aligned. Title islands max 560 dp.
- `:app:compileDebugKotlin` and `:app:testDebugUnitTest` green. Did **not** commit, run a device smoke-test, start Bucket 12, or change overlay-safe menus / heading math.

### 2026-08-25 — Play ops pack + Romania routing stack

- Play-ready **1024×500** `docs/play/feature-graphic.png` (24-bit). IARC answer sheet, Internal testing click path, tester CSV, screenshot capture script.
- `deploy/routing/`: OSRM car graph (Romania PBF) + optional Nominatim. Debug builds may use LAN/emulator HTTP (`network_security_config`). Release/Play still needs HTTPS.
- Did **not** log into Play Console, download the OSM extract, enable Pages, or start Bucket 12. Heading math / overlay menus / GPS-gated start / required-task Done unchanged.

### 2026-08-25 — Pre–Bucket 12 audit (fix wrong wiring)

- Queue **Skip current** was opening failed-delivery; it now moves the stop later like HUD Skip. HUD/queue skip retargets in-app routing; FGS watches the route so the next destination updates.
- Jump without a GPS fix no longer sets `deliveryActive` / starts a trip. Retry and the next fix keep the jumped stop.
- Copy-to-route: exclude current/archived routes; honest toasts (already on target vs copied). Encrypted SAF save is `.rpenc` / octet-stream. Share no longer resets the 7-day reminder (cancel looked like a backup).
- Cached polyline banner: online vs offline copy. Report-a-problem records an OSRM error only when every host fails; success clears it. Nominatim fallback host shown in Settings. Pause turns keep-awake off on route detail.
- Backlog wording corrected where checkboxes overclaimed (history “route” filter, Waze multi-stop, QS keep-awake, TalkBack/font-scale/two-pane, time-window “hard” optimize, trash = routes/library, Play ops still you).
- `:app:compileDebugKotlin` and `:app:testDebugUnitTest` green. Did **not** start Bucket 12, commit, run a device smoke-test, or change overlay-safe menus / heading math.

### 2026-08-25 — Bucket 11: Play / privacy / crashes

- About and Settings: **Report a problem** `mailto:` with version, Android, device, last OSRM error (`LastRoutingErrorStore`, persisted). `support.email` / `privacy.policy.url` in `local.properties`. Mailto in `queries`.
- Hostable privacy page `docs/privacy/index.html` (GitHub Pages `/docs`). Draft + Crashlytics-optional / Play Vitals / no Auth. Counsel still required before collecting emails.
- Crashlytics remains optional via gitignored `google-services.json`. Gradle excludes `firebase-analytics` and `firebase-auth`. Play Vitals documented as working without Firebase.
- Play pack: listing copy, screenshot shot list, IARC hints, feature-graphic SVG in `docs/play/`. Console testers / PNG upload / IARC submit still you.
- `:app:compileDebugKotlin` and `:app:testDebugUnitTest` green. Did **not** commit, run a device smoke-test, enable Pages, loosen GPS-gated start / required-task Done, or change overlay-safe menus / heading math.

### 2026-08-25 — Bucket 10: quality / internals

- Tests: `StopGeofence` (inside, accuracy, radius, action), jump/skip (`moveStopToEdge` + `completeStopsBefore`, required-task block), JSON backup **v3** (origin row promoted, `failureReason`, `taskTemplates`), overlay menus (Tip hartă Dialog and app menu do not move wrapping siblings).
- Debug-only StrictMode (`penaltyLog`, not death). `RouteMapBackdrop` `onRelease` blanks the WebView, drops `AndroidBridge`, and `destroy()`s it. JS bridge / heading / `pushAll` untouched.
- Room `exportSchema = true` → `app/schemas/` (v12 JSON generated on compile; no historical 1–11 files). When `google-services.json` is present, release Crashlytics mapping upload is forced on. `RouteDetailStopRow` split from detail screen.
- `:app:compileDebugKotlin` and `:app:testDebugUnitTest` green. Did **not** commit, run a device smoke-test, loosen GPS-gated start / required-task Done, or change overlay-safe menus / heading math.

### 2026-08-25 — Bucket 9: settings / power user

- Settings: TTS short/normal/verbose + mute during in-call / communication audio (no `READ_PHONE_STATE`). Geofence dwell 0/3/5/10 s before auto-arrive (leave-radius resets). Arrive-by clock system / 24h / am/pm. New routes default round-trip (edit, place, CSV, save-from-drive). Start uses default follow-me; Recenter / jump still enable follow. Keep screen on can be moving-only (~1.4 m/s). Data saver hides satellite and skips OSRM `/table` on Optimize (haversine 2-opt stays). Developer GPS sim still debug-only.
- Map follow during a trip is `chrome.driveFollow`, not “delivery is active”. Heading formula / `map.html` untouched. Room still **v12**. `:app:compileDebugKotlin` and `:app:testDebugUnitTest` green. Did **not** commit, run a device smoke-test, loosen GPS-gated start / required-task Done, or change overlay-safe menus / heading math.

### 2026-08-25 — Bucket 8: routing / geocoding ops

- Primary OSRM/Nominatim still from `local.properties` (`osrm.base.url`, `nominatim.base.url`). Optional `osrm.fallback.url` / `nominatim.fallback.url` — tried on 429, timeout, or 5xx, then last successful road polyline (`RoutePolylineCache`, ~100 m key), then straight line. Banner `nav_cached_route` when the cached line is shown.
- Settings → Routing and search: host labels, demo hint, Check routing and search (OSRM `nearest`, Nominatim `/status` then a tiny search). Does not ping on every open.
- Nominatim: 1.1 s global gate + 650 ms debounce on search and reverse. Plus-code decode still skips HTTP.
- Play listing OSM / HOT / CARTO / Esri / OSRM / Nominatim copy in `docs/PLAY_LISTING.md`. `:app:compileDebugKotlin` and `:app:testDebugUnitTest` green. Did **not** commit, run a device smoke-test, loosen GPS-gated start / required-task Done, or change overlay-safe menus / heading math.

### 2026-08-25 — Bucket 7: data / backup (sync rehearsal)

- SAF CreateDocument / OpenDocument for JSON (not share-sheet text only). Optional password wraps the same v6 JSON as `RPENC1` + salt + IV + AES-GCM. Auto-backup to a user-picked folder is always plaintext (password is not stored). Home shows a muted 7-day reminder.
- Import: decrypt if needed, then selective chips (routes / library / history / templates / fuel) and keep-mine vs use-file when LWW would overwrite. Skipped counts after import.
- Soft-delete: Home route delete and library Everywhere go to Trash (`deletedAtEpochMs`); Purge hard-deletes. This-route-only stop remove is still a hard delete. Backup export still includes tombstones.
- Duplicate a stop onto another route. History: All / this week / cancelled + name filter; share filtered CSV. Settings vacuum deletes trips older than 30/90 days except `IN_PROGRESS`. Auto vacuum + folder write on app open (24 h interval).
- Room still **v12**; JSON still **v6** (encryption is a wrapper). No WorkManager. `androidx.documentfile:documentfile:1.1.0` (there is no 1.1.1). `:app:compileDebugKotlin` and `:app:testDebugUnitTest` green (library Everywhere delete now asserts a Trash tombstone). Did **not** commit, run a device smoke-test, loosen GPS-gated start / required-task Done, or change overlay-safe menus / heading math.

### 2026-08-25 — Bucket 6: courier ops (Room v12, backup v6)

- POD photo/signature + timestamp on Done (optional; Settings “ask proof”). Skip proof still goes through `completeNextStop` (required tasks still block). COD amount on edit/stop details; collected on the proof dialog. Barcode field + Play scanner / typed paste (`BarcodeMatch`).
- Home/depot in Settings; Drive home shortcut + Home chip; new routes can use home as origin. Morning/afternoon shift slot + Home grouping; local My van filter (tablet dispatcher layout still skipped).
- Library favorites in the picker; NFC write only while `NfcWriteDialog` is open (`enableReaderMode` then unbind). NDEF URI `https://routeplanner.local/l/{remoteId}` opens Drive here.
- Stats from trip history (km, late, stops/hour, COD). Fuel/km log. ICS + PDF export via `cache/exports/`. JSON backup **v6** (v1–v5 still import).
- Skipped: Bluetooth scanner, CarPlay / Android Auto, extra languages, tablet dispatcher layout.
- Did **not** commit, run a device smoke-test, loosen GPS-gated start / required-task Done, or change overlay-safe menus / heading math. NFC reader mode must not stay bound after the write dialog closes.

### 2026-08-24 — Bucket 5: UI / UX / design

- Planning vs driving chrome on route detail. Search + library merge into one Add overlay (Dialog). During a trip, Add is on the HUD so FABs stay Recenter + layers.
- Taller Done/End (56 dp). Skip later moves the current stop to the end of the route (same as notification Skip). Call on the shade when the next stop has a phone. Done still omitted when required tasks block.
- Dismissible first-run checklist on home. One-time FGS location explainer before background GPS starts. Predictive back (`enableOnBackInvokedCallback`). Lock HUD uses `WindowInsets.safeDrawing`.
- Settings: reduce motion (sheets + map fly-to + tap haptics), high-contrast pins (square pending / round done, blue/green). Tip formula unchanged. After `map.html`: Reload map.
- Home widget (next stop + Done), static shortcuts (library / new route) + dynamic last route, QS tile for speak turns. Landscape home uses a 360 dp side rail.
- Skipped: extra languages, tablet dispatcher layout, drive-home shortcut (needs saved home in Bucket 6).
- Did **not** commit, run a device smoke-test, loosen GPS-gated start / required-task Done, or change overlay-safe menus / heading math.

### 2026-08-24 — Bucket 4: navigation and map

- Night driving: dark theme Map/Driving and explicit Night style use CARTO Dark Matter. Removed CSS invert of HOT tiles. Scale bar + N compass (larger when follow is off). Nearby route pins spider ~10 m.
- Follow can stay **north-up** (Settings + layers). Tip formula unchanged: `angle = (lastBearing + mapBearing + 360) % 360`. Invert still only in `applyMapBearing()`.
- Reroute calm / normal / sharp. Optional walk last-mile from snapped curb to pin. Dashed curb-to-door when the pin is off the road.
- Larger then-maneuver while moving; fatter Recenter while following. Compass figure-8 hint when accuracy is low. Layers / Settings **Reload map** clears WebView asset cache.
- Skipped: speed limits, live traffic, offline tiles/graph, 3D pitch, MapLibre (need data or heading-on-device).
- Did **not** commit, run a device smoke-test, loosen GPS-gated start / required-task Done, or change overlay-safe menus.

### 2026-08-24 — Bucket 3: library and places (Room v11, backup v5)

- Library pins: comma tags/zones, plus code (local Open Location Code), optional What3Words **typed text** (no W3W SDK), last-used/use-count, default geofence radius, default tasks copied onto a route stop.
- Picker shows recent then frequent then A–Z. Merge nearby duplicates (~35 m + similar name) reassigns `libraryStopId`.
- Search: plus-code paste, hard Near me (`bounded=1`), saved searches, contact pick (optional `READ_CONTACTS`). Long-press reverse-geocodes via Nominatim (debounced). Copy lat/lng + plus code.
- Library map clusters in `setRoute` 4th arg (no extra JS lib). **Reinstall / clear app data** after this `map.html` change. Tip/heading math untouched.
- CSV into an existing route; one-route CSV export; GPX/KML/GeoJSON for library or a route. JSON backup **v5** (v1–v4 still import).
- Plus-code encoder uses Google’s integer pair algorithm (known Zurich sample `8FVC9G8F+…`). GeoJSON export is built as text so JVM unit tests do not hit Android `org.json` stubs. `:app:testDebugUnitTest` green.
- Did **not** commit, run a device smoke-test, loosen GPS-gated start / required-task Done, or change overlay-safe menus.

### 2026-08-24 — Bucket 2 remainder (Room v10, origin, pause, van-day UI)

- Route origin is route-level (`originLatitude` / `originLongitude`); leftover `isOrigin` stop rows are deleted on migrate 9→10. **I’m here** does not auto-complete on Start.
- Pause keeps the delivery session and stops FGS/TTS/geofence. Home park card shows Resume when paused. HUD enlarges Done while moving and hides search/library/edit until parked.
- Fail flow: reason then optional camera photo / signature (FileProvider). Reschedule can pick a calendar date (`arriveByEpochMs`). Undo Mark done ~10 s (root overlay, not inside HUD).
- Optimize honors `isFixedOrder` segments and repairs late time windows. Breaks skip the required-task gate. Vehicle profile + `exclude=` on OSRM (public demo may ignore exclude).
- Home: Today / All / Archived chips, color strip, van/shift, archive. Edit: color chips, phone/door, break, fixed, calendar arrive-by.
- JSON backup **v4** (v1–v3 still import). Volume-key Done stays opt-in + parked + arrived. Successful Done still requires required tasks unless Fail or explicit defer.
- Did **not** commit, run a device smoke-test, loosen GPS-gated start, or change heading math / overlay-safe menus.

### 2026-08-24 — Van-day: GPS origin, park card, fail/reschedule, templates, R8

- Saved the idea menu in `docs/PRODUCT_BACKLOG.md`. This session implemented bucket 1 except commit/device smoke-test, plus the listed bucket 2 items.
- Room v9: `stops.isOrigin`, `stops.failureReason`, `task_templates`. JSON backup format v3 (v1–v2 still import). GPS origin is not written to the library; Start delivery auto-completes origin rows.
- Home park card shows next stop + Continue + Mark done. HUD / lock / notification / Home share **Mark done**; Done is disabled in UI and omitted from the shade notification when required tasks block. Failed delivery bypasses that gate.
- Release `isMinifyEnabled` + keep rules for the WebView JS bridge and Room entities. OSRM 429/503 and timeouts have distinct approx banners. Settings splits JSON vs CSV and lists task templates.
- Did **not** commit, run a device smoke-test, loosen GPS-gated start, or change heading math / overlay-safe menus. Successful Done still requires required tasks.

### 2026-08-24 — Local 0.5.0: Live Update chip, backup v2, Play ops wiring

- Android 16 island: Live Update is **not** the FGS notification (ID 41). A second promoted notification (ID 42, channel `trip_live_update`) calls `setRequestPromotedOngoing` / `setShortCriticalText` directly. Runtime `POST_PROMOTED_NOTIFICATIONS` is requested with `POST_NOTIFICATIONS`. No full-screen intent on the chip. Shade Driving notification remains the fallback on OEMs without Live Updates.
- Version `0.5.0` (`versionCode` 6). JSON backup format v2 includes trip history (in-progress imports as cancelled). CSV export of the library; Settings copy distinguishes JSON vs CSV.
- Optimize: after haversine NN+2-opt, OSRM `/table` duration 2-opt for remaining pinned ≤ 20.
- `local.properties` `osrm.base.url` / `nominatim.base.url` → `BuildConfig`. Public demos remain the default.
- Release signing from gitignored `keystore.properties`. Crashlytics plugins apply only if `app/google-services.json` exists.
- Robolectric: backup round-trip (library link, tasks, round-trip, geofence, history); format v1 still imports.
- Did **not** create a Play listing, host a privacy URL, or stand up production OSRM — those need your Google/hosting accounts. Did **not** touch heading math, overlay-safe menus, GPS-gated start, or required-task gates.

### 2026-08-24 — Return-to-driving back stack + debug developer sim

- **Back to driving** now `popUpTo` home then opens the active trip, so Back from the drive is the home list (where you start a route), not Settings/library/the screen you left.
- Debug-only **developer** sign-in (Account, `BuildConfig.DEBUG` only): on-screen GPS joystick, speed presets, jump to map center / next stop. Implementation lives in `src/debug`; `src/release` is a no-op. Release `Application` signs out a leftover `providerId=developer` session. Not a Google (or any real) account; must not ship.
- Did **not** touch heading math, overlay-safe menus, GPS-gated start (sim counts as a fix in debug), or required-task gates.

### 2026-08-24 — Reorder, CSV, privacy URL, lock-screen HUD

- Stop rows (detail + edit): drag handle reorders by stepping up/down; up/down buttons stay; double-arrow moves to top/bottom. Repository `moveStopToEdge`; detail moves serialized with a mutex.
- Settings CSV import: flexible headers (`name`/`lat`/`lng`, comma or semicolon), quoted fields; creates library stops and a new route named from the file.
- About and Settings link to `docs/PRIVACY_DRAFT.md` on GitHub. README feature list updated.
- “Replace this drive?” uses the saved route name when the active trip is a route (`activeRouteId`), not the generic back-to-driving label.
- Lock-screen HUD (`LockScreenHudActivity`): Compose-only next-stop / distance / maneuver. SCREEN_OFF during a keep-awake trip opens it; notification HUD action; Done / End / Open map. No Leaflet on the lock screen. `USE_FULL_SCREEN_INTENT`.
- Did **not** touch heading math, overlay-safe menus, GPS-gated start, or required-task gates.

### 2026-08-24 — Background trip, notification, status-bar chip

- Active route or one-off drive starts `TripGuidanceService` (foreground `location`). GPS and the next-stop path keep updating after Home / another screen. Swiping the app away does not kill the trip (`stopWithTask=false`).
- Ongoing **Driving** notification: next stop, remaining distance/ETA, progress. Actions: **Done** (same required-task gate as in-app) and **End trip** (keeps stop progress). Tap opens the active drive screen.
- Android 16 Live Update (`ProgressStyle` + promoted ongoing): status-bar chip with a short remaining-distance label on devices that show Live Updates. Older Android keeps a normal ongoing notification.
- Spoken turns continue for the whole trip when the setting is on (service TTS; screen TTS is skipped while the service runs). Notification permission is requested when a trip starts (Android 13+).
- Did **not** touch heading math or overlay-safe menus. GPS-gated start and required tasks still apply.

### 2026-08-24 — Loosen restrictive UX (map hub)

- Home search / long-press opens **This place** (drive, save library, add to route, new route), not drive-only. Deleting a route confirms. Cards have Start (GPS-gated autostart) and Open in Maps.
- Drive here while another trip is active **asks** to replace or keep; keep returns to the current drive. Does not silently steal the trip.
- App menu lives on `RoutePlannerApp` root (`AppMenuOverlay`). Every screen has the hamburger; overlays only.
- Delivery: add stop via search/library/long-press while driving. Jump ahead asks whether to leave earlier stops or mark them done. Run again after all complete. Preview path without entering delivery. Keep-awake for the whole trip. Foreground TTS for maneuvers (Settings).
- Library filter + add-to-route. Edit route search / long-press to pin. Quick drive can save as a route.
- Kept: GPS-gated start, required-task gates, Back does not end delivery, heading formula, overlay-safe menus.

### 2026-08-24 — Drive here, trip history, return-to-driving

- One-off path: search / long-press home map / library **Drive here**. No Room route. Arrive or cancel clears the session; DataStore + `trip_history` keep it only if the dest is not done yet (crash/close resume).
- If a drive is already active, Drive here **asks** to replace or keep (see later session). User can keep and return to the current trip.
- Room **v8** `trip_history`. JSON backup format **v2** exports trips (remap `routeRemoteId` / `libraryRemoteId`). In-progress rows import as cancelled. Menu → Trip history. In-progress resume; completed route opens the route; completed one-off starts a new path to the same dest.
- `ReturnToDrivingBar` on the app root `Box` (overlay-safe). Hidden on home (resume card) and on the matching drive screen.
- Delivery start/progress/end writes trip history. Back from driving still does **not** call `endDelivery`.
- Did **not** touch heading math or put menus inside height-wrapping chrome.

### 2026-08-24 — Auto-arrive geofence

- During delivery, GPS entering a stop radius can **mark visited** or **mark done**. Required tasks still block completion (visited is recorded instead).
- **Settings** holds the app default (off / visited / done + radius 20–500 m, default 50 m).
- **Edit route** can inherit that default, force off, or pick visited/done for that route; optional route radius. Each stop can override the radius.
- Poor GPS (accuracy worse than the radius) does not trigger. Reset progress clears visited too. Room **v7**.

### 2026-08-24 — Local route-planning tools

- Optimize is nearest-neighbor **then 2-opt**. Edit route can mark **round trip** (return to first pin in distance + after last stop, HUD “Drive back to start”).
- Stops can have **arrive-by** and **minutes on site** (detail dialog and edit-route rows). Home cards show remaining distance + rough ETA from GPS; detail/HUD show ETA and late warning per remaining stop.
- Reverse remaining stops; sort remaining by promised time; **Open in Maps** sends remaining pins as a multi-stop Google Maps drive (round trip appends the first pin).
- Map camera (pan/zoom) is saved in DataStore and restored when a WebView opens, so GPS fly no longer steals a panned view. My Location / empty-route recenter still flies.
- Room **v6**: `routes.roundTrip`, `stops.arriveByMinutes`, `stops.serviceMinutes`. Backup JSON fields are optional so v1 backups still import.

### 2026-08-24 — HOT streets (courier contrast)

- Map / Driving switched from Esri World Street Map to Humanitarian OSM (HOT) raster tiles (`tile.openstreetmap.fr/hot`). Roads are thick and colored; last-mile alleys stay visible. Dark theme still inverts **street tiles only**.
- Leaflet-rotate / heading formula unchanged. Satellite and terrain stay Esri. WebView User-Agent now includes `RoutePlannerAndroid/{version}` for OSM France tile policy.
- After `map.html`: **reinstall / clear app data**.

### 2026-08-24 — CI Room migration tests

- GitHub `testDebugUnitTest` failed `migrate3To4` / `migrate4To5` with `IllegalStateException` when Room opened the migrated DB (v2→v3 passed).
- Cause: v3/v4 fixtures omitted `index_stop_library_name`, `index_stop_library_updatedAtEpochMs`, and (v4) `index_stop_tasks_stopId`. Room v5 still expects those indexes after chained migrations.
- Tests now create the `databases/` dir, keep Room's connection open until `room.close()`, and enable `includeAndroidResources` for Robolectric on Linux.

### 2026-08-24 — Ship 0.4.0

- Version `0.4.0` (`versionCode` 5). Working tree from library-as-source-of-truth through collapsible sheets is this release. Play/store and cloud sync stay out.

### 2026-08-24 — Collapsible bottom islands

- Bottom map sheets (home routes, library list, route stops, delivery HUD, edit route) have a grab bar. Drag down or tap to peek more map; drag up to restore. Handle-only drag so lists still scroll; top chrome does not reflow.

### 2026-08-24 — Follow pivot + heading when not centered

- Follow-me now `setView`s the user, not a 55 m look-ahead — leaflet-rotate was pivoting around that offset so the arrow orbited a nearby point.
- Compass azimuth is geographic again (no Kotlin `360 - azimuth`). Leaflet invert is only `applyMapBearing((360 - heading) % 360)` so GPS course and compass share one space.
- Chevron CSS is `(heading + mapBearing) % 360` so the arrow tracks the phone when the map is north-up or follow is paused. Reinstall / clear app data after `map.html`.

### 2026-08-24 — Street contrast + list chrome

- Map / Driving streets are Esri World Street Map (dark-gray roads). Dropped CARTO Voyager / Dark Matter — Voyager’s white roads and Dark Matter hairlines were unreadable.
- Dark theme **inverts street tiles only** (`body.theme-dark.style-streets`); satellite and terrain are not filtered. Driving adds a mild extra contrast in light theme.
- Lists and menus use `IslandListItem` cards (`rowSurface` + `fieldBorder`) so rows separate. Islands keep a visible `fieldBorder` stroke so they don’t vanish on pale maps.
- After `map.html` change: **reinstall / clear app data**.

### 2026-08-24 — Library source of truth, tasks, map contrast, list scroll

- Library is canonical: creating a pinned stop writes the library first; routes keep a reference. Edit/delete of a shared stop asks global vs this-route (fork) vs cancel.
- Library map shows pins; list rows show which routes use each stop. Room v5 backfills unlinked pinned stops.
- Task checklist is card-based (edit title, required chip, progress on stop rows); tasks stay usable during delivery.
- Island `blockMapPassThrough` no longer consumes leftover pointers (that stole list scrolling). Home/detail lists use `LazyColumn(weight)`.

### 2026-08-22 — Floating chrome hit-testing and overlays

- `FloatingIsland` (and the attribution chip) block map pass-through so WebView pan/zoom cannot steal gestures from empty island chrome.
- Home hamburger uses a root-Box scrim; Back / tap outside closes. Opening hamburger dismisses layers and vice versa.
- Detail attribution sits under the title in the left chrome column (beside the FAB stack), not over the title island. GPS/status copy uses the title subtitle instead of a second overlay on the FABs.
- Nested round buttons inside islands use `embedded` (no double shadow). Layers menu keeps Dialog + light scrim; still not inside wrapping chrome.

### 2026-08-22 — Map tiles and chrome (Leaflet kept)

- Did **not** switch to MapLibre / OpenFreeMap: vector maps would replace leaflet-rotate heading.
- Street map: Esri World Street Map for Map / Driving. Dark theme inverts those tiles only (`style-streets`). Driving adds extra contrast in light theme.
- Satellite: Esri imagery + transportation + place-name overlays.
- Terrain: Esri World Topo (OpenTopoMap was slow / low max-zoom).
- Numbered stop pins; hide duplicate Leaflet attribution (Compose chip updated).
- After this `map.html` change: reinstall or clear app data (WebView asset cache).

### 2026-08-22 — Courier tools (duplicate, optimize, map style)

- Preferred map style persisted in DataStore; ending driving follow restores satellite/terrain, not always Map.
- Home: route filter, progress bar, approx distance, duplicate (copies tasks, resets progress).
- Nearest-neighbor optimize on detail (keeps completed prefix) and edit. Unpinned stops stay last.

### 2026-08-22 — Account/sync groundwork (P4 prep)

- Room v4: `remoteId` + `deletedAtEpochMs` on routes, stops, library, tasks; migration 3→4.
- `AccountSessionStore` + Account screen; menu wired (logout from menu; debug preview sign-in).
- JSON backup export/import in Settings via `RouteBackupManager`.
- ADR 0002 local-first sync strategy.

### 2026-08-22 — P3 tests + P6 polish

- Removed dead English `formatInstruction` in `OsrmRoutingClient` (HUD already uses localized `ManeuverFormatter`).
- Dropped ViewModel fallback `"GPS stop"`; screen passes `gps_stop_default_name` (EN/RO).
- Added Robolectric tests for repository task gates and Room 2→3 migration; ViewModel layer is thin delegation only.

### 2026-08-22 — Backlog audit (this review)

- Confirmed PR #20 on `main`: library, modularity refactor, P1 library UX, P2 courier polish.
- Documented uncommitted stop-task increment (Room v3), launcher icon, heading/inpection fixes.
- P3 implementation marked done; tests + commit/PR remain. Added smoke-test items for tasks and empty-route onboarding.

### 2026-08-06 — Inspection remediation

- Added `ACCESS_NETWORK_STATE` for the connectivity check, explicit locale handling for distance formatting, and proper plural resources in English and Romanian.
- Added basic document metadata to the WebView map asset and simplified a redundant route-update condition. Reinstall or clear app data before testing the changed map asset.
- Verified the JavaScript bridge methods are individually annotated with `@JavascriptInterface`; the generic `addJavascriptInterface` IDE report is suppressed locally as a false positive.
- Launcher artwork uses the supplied map-and-pin PNG unchanged. `docs/assets/route-planner-icon.svg` is a lossless SVG wrapper around its adjacent PNG; Android uses the PNG for the legacy and API 26+ adaptive icon.

### 2026-08-05 — Stop tasks (initial local implementation)

- Added Room v3 / migration 2→3 with a per-stop task table: title, required state, completion state, timestamp, and optional completion note.
- Added a modular task checklist to stop details. Required outstanding tasks block stop completion, delivery skip, and queue jump-over; resetting route progress resets task completion records too.
- Added Robolectric tests for repository task gates and Room 2→3 migration; ViewModel layer is thin delegation only.

### 2026-08-05 — Driving heading investigation

- Physical test reports mirrored left/right movement in driving mode with the original upright-device mapping.
- An attempted `-Z` upright-axis change stopped visible heading updates on-device and was reverted immediately. Do not reuse it.
- **Superseded 2026-08-24:** do **not** invert azimuth in Kotlin. That made the chevron mirror whenever the map was not following (north-up / pan away). Invert only in `applyMapBearing`.

### 2026-08-05 — Kotlin implementation handover

- Chose this Kotlin/Compose Android project as the active implementation; the Expo/React Native repository is now a frozen prototype/reference.
- Verified the existing codebase with JDK 21: `testDebugUnitTest`, `lintDebug`, and `assembleDebug` all pass.
- Preserved the existing uncommitted `polish/real-use-v0.3.1` working set. Review and commit it as a baseline before starting task data-model work.
- Identified documentation drift: the README listed 0.3.0 while `app/build.gradle.kts` and the changelog identify 0.3.1; README aligned.

### 2026-08-01 — P2 courier polish

- Localized Edit route errors + external nav chooser (EN/RO).
- GPS: permission / waiting / poor-accuracy hints; home subtitle shows live location message.
- Offline OSRM fallback uses `nav_approx_offline`; empty route detail shows drop-first-stop onboarding (search / library / long-press).

### 2026-08-01 — Library link UX + update/dedupe

- Delete-from-route copy clarifies library entry is kept when linked; “From library” badge on rows.
- Add-from-library dedupes by `libraryStopId` (opens existing); picker shows Already on route / Open.
- Stop details: Update library + Refresh from library when linked; status notices for add/save/refresh.

### 2026-08-01 — Edit library stops

- Tap a library row → edit name / address hint / notes / lat / lng (`EditLibraryStopDialog`).
- Delete asks for confirmation; copy-on-route behavior unchanged (library-only edits).
- EN/RO strings for edit/delete copy.

### 2026-08-01 — Library on Edit route

- Edit screen: bookmark FAB + “Add from library” + `StopLibraryPickerDialog` (Manage → library screen).
- Draft stops keep `libraryStopId` through load/save (`EditableStop` → `StopDraft`).
- New routes can receive library stops before first save; empty-state copy updated EN/RO.

### 2026-08-01 — Modularity refactor (no heading/JS changes)

- Extracted shared GPS/compass merge: `ui/location/MergeUserHeading.kt` (`mergeWithCompass` / `rememberMergedUserFix`).
- Extracted map chrome: `ui/map/MapChromeState.kt` + `MapLayersMenuHost`; wired list + detail.
- Peeled detail dialogs into `ui/routes/RouteDetailDialogs.kt`; dialogs hosted inside detail’s root `Box`.
- Split `DeliveryHud` into phase composables (behavior unchanged).
- **Did not touch** `map.html` tip math, compass remapping, or empty-route / lastKnown centering.

### 2026-07-31 — Real-use polish & layout/heading wars

- Delivery: GPS-gated start; approx-route honesty; stop-row clarity; About + attribution.
- Delivery stop queue (skip / jump); detail reorder; EN/RO maneuvers.
- Map layers / menus converted to true overlays (Dialog + root `Box`) after in-flow menus shoved chrome.
- Heading: compass remapping, GPS vs compass hybrid, tip vs map bearing, rotate listeners; multiple mirror flips — **treat as fragile**.
- Dark mode shadow/contrast fixes.
- Map default away from Timișoara; `LastKnownMapCenter`; first-fly + empty-route recenter.

### 2026-08-01 — Stop library groundwork + this log

- Room v2: `stop_library`, `stops.libraryStopId`, migration 1→2.
- Menu library screen; detail picker / save / also-save.
- Created this continuity doc; modularity rules for overlays.

*(Append newer entries above this line in reverse chronological order, or add dated subsections below.)*

---

## File cheat sheet (sensitive)

| Concern | Primary paths |
|---------|----------------|
| Map JS / heading | `app/src/main/assets/map.html` (night tiles, scale/N, pin spider, north-up; **do not change** tip formula) |
| Map bridge | `ui/map/RouteMapBackdrop.kt` |
| Layers menu | `ui/map/MapLayersButton.kt`, `ui/map/MapChromeState.kt` |
| Heading merge | `ui/location/MergeUserHeading.kt` |
| Compass | `ui/location/RememberDeviceBearing.kt` |
| GPS | `ui/location/RememberUserLocation.kt`, `LocationHelpers.kt` |
| Islands / shadows | `ui/components/FloatingIsland.kt`, `ui/theme/Theme.kt` |
| Detail chrome | `ui/routes/RouteDetailScreen.kt`, `RouteDetailDialogs.kt` |
| Delivery HUD | `ui/routes/DeliveryHud.kt` |
| Home chrome | `ui/routes/RouteListScreen.kt` |
| Library | `data/local/StopLibrary*.kt`, `ui/library/*` |
| Stop tasks | `data/local/StopTask*.kt`, `ui/routes/StopTaskChecklist.kt` |
| DB version | `data/local/AppDatabase.kt` (v11: library tags/plus/w3w/usage/default geofence, `library_default_tasks`, `saved_searches`; JSON backup v5) |
| Drive here / return chip | `ui/drive/*`, `ui/RoutePlannerApp.kt` |
| Trip history | `data/local/TripHistory*.kt`, `ui/history/*` |
| Account / backup | `data/account/*`, `data/backup/RouteBackupManager.kt`, `ui/account/*` |
| Stop order | `util/RouteOrderOptimizer.kt` |
| Network / offline | `util/NetworkStatus.kt` |

---

## Reminder for future agents

> If something already works on device (heading, overlay menus, empty-route centering), **prefer additive changes**. Do not rewrite heading math, map defaults, or chrome layout “for cleanliness” without running the smoke-test list and updating this file.
