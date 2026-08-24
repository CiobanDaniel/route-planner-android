# Development status & agent continuity log

**Purpose:** Survive context resets. Read this **before** changing map heading, floating UI chrome, Room schema, or delivery flow.
**Last updated:** 2026-08-24
**Branch context:** `feature/stop-tasks` → **0.4.0** PR (library source of truth, tasks, courier tools, map/heading/UI polish). Play Console / Crashlytics stay out of this release.

**Implementation decision (2026-08-05):** This Kotlin/Android repository is the active product implementation. The Expo/React Native repository is retained only as an earlier prototype/reference and must not receive parallel feature work.

---

## How to use this file

1. **Before editing a sensitive area**, check the matching section under [Do not regress](#do-not-regress--protected-behaviors) and [How it works](#how-it-works-by-area).
2. **After finishing a meaningful change**, append a short entry to [Session / change log](#session--change-log) and update [Next](#next--prioritized-backlog).
3. Prefer **modular, overlay-safe UI** (see [Modularity rules](#modularity-rules-ui)). Do not put expandable menus inside height-wrapping `Row`/`Column`/`Box` that also holds sibling chrome.

Related docs: `AGENTS.md`, `CHANGELOG.md`, `docs/adr/0001-map-webview-leaflet.md`, `docs/MANUAL_SETUP.md`.

---

## Product snapshot

| Area | Status |
|------|--------|
| Local routes / stops (Room) | Done |
| Map-first UI (Leaflet WebView) | Done |
| Delivery mode + OSRM turn-by-turn | Done |
| Address search (Nominatim) | Done |
| Settings (theme / EN-RO / units / keep screen on) | Done |
| Delivery session persist/resume | Done |
| Stop library (device-local) | Done — canonical stops; routes reference `libraryStopId` |
| Per-stop task checklists (local) | Done in working tree; **not committed** |
| Kotlin/Android client | Active implementation |
| Accounts / cloud sync / share library | Groundwork (session store, remoteId, JSON backup); auth TBD |
| Play signing / Crashlytics / privacy URL | Manual / pending |

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
- App hamburger menu is overlaid on the **root** `Box` with a full-size scrim (tap outside / Back closes). Opening hamburger closes layers and vice versa.
- `FloatingIsland` (and the attribution chip) must `blockMapPassThrough` so island chrome is a hit target; the WebView map must not pan/zoom through empty island area. **Do not consume leftover pointer events** — that steals list scrolling.
- Nested round buttons inside an island use `FloatingCircleButton(embedded = true)` — no second drop shadow.
- Lists inside islands need a **bounded height** (`heightIn` + `LazyColumn(Modifier.weight(1f))`) so they actually scroll.
- Bottom map islands that can hide the map use `CollapsibleBottomIsland`: grab-bar drag only (not the list), height change absorbed by the weighted spacer, no expanding menu inside the sheet.

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

- Light theme may use dual “sculpted” shadows.
- Dark theme: **`useHighlightShadow = false`** — drop shadow only. Forcing high-alpha light shadows looks like white fog.

**Key files:** `ui/theme/Theme.kt`, `ui/components/FloatingIsland.kt`.

### 6. String resources

- Leading spaces in string XML are stripped by Android. Use `\u0020` (e.g. `maneuver_onto`).
- Always update **EN + RO** together.

---

## How it works (by area)

### Map (Leaflet WebView)

| Piece | Role |
|-------|------|
| `assets/map.html` | Leaflet + leaflet-rotate, styles, user marker, nav polyline, gestures |
| `RouteMapBackdrop.kt` | AndroidView WebView bridge; pushes style, follow, route, user, focus |
| `MapViewMode` | map / driving / satellite / terrain tile ids |
| `MapLayersMenuDialog` | Overlay menu; toggles style + Follow me |
| ADR `docs/adr/0001-map-webview-leaflet.md` | Why WebView instead of native GL maps |

Bridge JS entry points: `setUserLocation`, `setDriveFollow`, `resumeDriveFollow`, `setMapStyle`, `setRoute`, `setNavRoute`, `flyToPlace`, `clearNavRoute`.

### Routes & delivery

| Piece | Role |
|-------|------|
| `RouteEntity` / `StopEntity` / `RouteDao` | Per-route stops; `isCompleted`; optional `libraryStopId` |
| `RouteRepository` | CRUD, reorder, library copy-on-add |
| `RouteDetailScreen` + `ViewModel` | Map pins, search, library pick, delivery HUD, queue |
| `DeliveryHud` / `DeliveryStopQueueSheet` | Nav UI; skip / jump (jump marks earlier unfinished done) |
| `DeliverySessionStore` | DataStore active route for resume |
| `OsrmRoutingClient` + `ManeuverFormatter` | Road route; localize instructions at display time |

Stop creation today: long-press, address search, GPS, or library pick. Pinned stops are created in the library first; the route stores a reference (`libraryStopId`) plus per-route order/completion/tasks. Edit/delete of a shared stop asks global vs this-route (fork).

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

- Theme / language / units / keep screen on → DataStore.
- About screen + OSM attribution chip.
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
   }
   ```

2. **One job per composable**
   Button ≠ menu. Picker ≠ repository. Formatter ≠ HTTP client. Prefer shared chrome (`MapChromeState`) and heading merge (`MergeUserHeading`) over copy-pasting between list/detail.

3. **No multi-root composables that emit layout siblings into a parent `Column`**
   Wrap in a single `Box` if you must emit Dialog + button from one function (Dialog itself is windowed; still keep one layout root for the anchor). Detail dialogs live in `RouteDetailDialogs.kt`; delivery HUD phases are private composables in `DeliveryHud.kt`.

4. **State that affects layout height** (messages, banners)
   Prefer `align` overlays over inserting extra islands into a weighted column that shifts the bottom HUD.
   Short GPS/status copy can replace the **title-island subtitle** (same pattern as home) so it does not sit on the title or the right FAB column.
   Islands must `blockMapPassThrough` so WebView pan/zoom does not steal gestures from chrome.

5. **Room / data**
   Prefer additive migrations. Library is the source of truth for place data; route stops are references plus per-route state (order, completion, tasks).

6. **Feature flags / seams for accounts later**
   Keep library and routes local-first; any sync API should sit beside `RouteRepository`, not inside Compose screens.

---

## Next — prioritized backlog

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
- [x] JSON backup export/import in Settings (merge by `remoteId`; rehearsal for sync payloads).
- [x] ADR `docs/adr/0002-local-first-sync.md` (LWW merge, local-first rules).
- [ ] Sync routes + stop library to server; conflict policy beyond LWW.
- [ ] Share stops/routes with edit permissions.
- [ ] Do **not** start server sync until P0 heading + layout stay green on device.

### P5 — Release / ops (manual)

- [ ] Play App Signing, Play Console, privacy policy URL.
- [ ] Crashlytics / App Insights if desired.
- [ ] Branch protection / release checklist (`docs/RELEASE_CHECKLIST.md`).
- [ ] README feature list still says “0.3” generically — update for library, tasks, empty-route onboarding when tagging next release.

### P6 — Remaining polish (non-blocking)

- [x] Localize OSRM fallback instructions in `OsrmRoutingClient.formatInstruction` (still English; display path uses `ManeuverFormatter` when OSRM returns structured steps). Removed duplicate English formatter; HUD uses `ManeuverFormatter` only.
- [x] Replace hardcoded `"GPS stop"` default in `RouteDetailViewModel.addStopAtCurrentLocation` with `R.string.gps_stop_default_name`.
- [x] `testDebugUnitTest` + `assembleDebug` + `lintDebug` pass on 2026-08-22 (stop tasks + Robolectric tests).

### P7 — Courier tools (local)

- [x] Persist preferred map style (map / satellite / terrain); restore it after driving follow.
- [x] Home list: filter routes, progress bar, approximate distance, duplicate route.
- [x] Nearest-neighbor stop order (detail + edit). Completed stops stay first; unpinned stay last.
- [ ] 2-opt / road-aware optimize (OSRM matrix) if NN is not enough in the field.
- [ ] Per-stop time windows / promised arrival.
- [ ] Persist last map center+zoom independently of GPS fly.

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
8. Dark mode: islands have drop shadow, **no white halo**; secondary text readable.
9. Add stop → save to library → other route → add from library.
10. Start delivery only works with GPS; OSRM fail shows approx banner + Retry.
11. Empty route → onboarding card (search / library / long-press hint); centers on GPS when available.
12. Stop details → add required task → cannot mark stop done / skip / jump until task complete.
13. Reset route progress → task completion records clear too.
14. Home: filter routes; duplicate opens a copy with tasks and empty progress.
15. Detail / Edit: Optimize reorders pinned stops; completed stay first.
16. Switch to satellite, leave the screen, come back — style is still satellite. End delivery restores that style, not always Map.

---

## Working tree

Landed in **0.4.0** (this PR). Next product increment after merge: courier-facing map/UI polish (modern high-contrast streets, then related P7 items). Play/store ops and accounts/sync stay later.

---

## Session / change log

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
| Map JS / heading | `app/src/main/assets/map.html` |
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
| DB version | `data/local/AppDatabase.kt` (v4: remoteId + tombstones) |
| Account / backup | `data/account/*`, `data/backup/RouteBackupManager.kt`, `ui/account/*` |
| Stop order | `util/RouteOrderOptimizer.kt` |
| Network / offline | `util/NetworkStatus.kt` |

---

## Reminder for future agents

> If something already works on device (heading, overlay menus, empty-route centering), **prefer additive changes**. Do not rewrite heading math, map defaults, or chrome layout “for cleanliness” without running the smoke-test list and updating this file.
