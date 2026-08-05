# Development status & agent continuity log

**Purpose:** Survive context resets. Read this **before** changing map heading, floating UI chrome, Room schema, or delivery flow.
**Last updated:** 2026-08-05
**Branch context:** Work accumulated on `polish/real-use-v0.3.1` / mainline toward post-0.3.1 (see `CHANGELOG.md`).

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
| Stop library (device-local) | Done (groundwork) |
| Kotlin/Android client | Active implementation |
| Accounts / cloud sync / share library | Not started |
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
- App hamburger menu is overlaid on the **root** `Box`, not inside the top chrome’s measuring box.

**Key files:** `ui/map/MapLayersButton.kt`, `ui/map/MapChromeState.kt`, `ui/routes/RouteListScreen.kt`, `ui/routes/RouteDetailScreen.kt`.

### 2. User heading triangle (compass / follow-me)

**Hard lessons:**

- Stale **GPS course** while stationary locks heading north — prefer **remapped compass** when not moving; GPS course when `speed ≥ ~1.4 m/s`.
- leaflet-rotate puts **`markerPane` on `norotatePane`** (screen-fixed markers). Tip CSS must be **relative to map bearing**.
- Current tip formula in `map.html` `updateUserMarkerVisual()`:

  ```js
  angle = (lastBearing - mapBearing + 360) % 360
  ```

  Flipping this without a device test reintroduces left/right mirror bugs. If it looks mirrored again, change **one** place and re-test on device (flat + upright); do not thrash Kotlin and JS together.
- Recompute tip on map **`rotate` / `rotateend` / `moveend`** (`onMapRotated`) so manual map twist does not desync the chevron.
- Avoid animated `panTo` while calling `setBearing` — they race and snap north. Prefer `setView(..., { animate: false })`.
- Compass: `RememberDeviceBearing` remaps for **flat** vs **upright** + display rotation, then inverts the sensor azimuth once to match Leaflet's rotation direction. Do not invert both Kotlin and `map.html`.
- After editing `app/src/main/assets/map.html`, **reinstall / clear app data** — WebView assets are easy to cache stale.

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

Stop creation today: long-press, address search, GPS button (non-nav), add-from-library. Edit screen is mainly name/notes/reorder (new-route form historically hid stops — prefer detail for adding pins).

### Stop library (local groundwork)

| Piece | Role |
|-------|------|
| `StopLibraryEntity` + `StopLibraryDao` | Device-local reusable stops |
| Room **v2** + `MIGRATION_1_2` | Creates `stop_library`; adds `stops.libraryStopId` |
| Menu → Stop library | List / delete |
| Detail: bookmark FAB | Picker → `addStopFromLibrary` (copy onto route) |
| Add-pin dialog | Optional “Also save to library” |
| Stop details | “Save to library” |

**Not yet:** accounts, sync, multi-user edit, share library over network. `libraryStopId` is the hook for later.

### Settings / chrome

- Theme / language / units / keep screen on → DataStore.
- About screen + OSM attribution chip.
- Island UI: `FloatingIsland`, `FloatingCircleButton`, `IslandDialog`.

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
   Prefer `align` overlays over inserting rows into a weighted column that shifts the bottom HUD.

5. **Room / data**
   Prefer additive migrations. Copy-on-add for library→route so delivery progress stays per-route.

6. **Feature flags / seams for accounts later**
   Keep library and routes local-first; any sync API should sit beside `RouteRepository`, not inside Compose screens.

---

## Next — prioritized backlog

### P0 — Stabilize before more features

- [ ] Review and commit the current `0.3.1` polish/library working set as a focused baseline before adding unrelated features. It currently spans Room, map, delivery, library, and localization work.
- [ ] Device smoke-test checklist (below) on a physical phone; confirm heading left/right + after manual map rotate.
- [x] Verify the local baseline with JDK 21: `testDebugUnitTest`, `lintDebug`, and `assembleDebug` pass on 2026-08-05.
- [ ] If heading still wrong: document the **one** formula that works on that device in this file; stop alternating blindly.
- [ ] Push / open PR for current polish; tag when ready (`v0.3.1` or `v0.3.2` per changelog).

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

- [ ] Add an additive Room migration with a per-stop task table: title, required flag, completion state, completion timestamp, and completion note.
- [ ] Add a stop-detail task checklist using the existing island/dialog conventions; keep menus as overlays.
- [ ] Require all required tasks before a stop can be marked complete, while preserving the existing route/delivery progress behavior.
- [ ] Add repository and ViewModel tests for task state transitions and the Room migration.
- [ ] Keep this fully local-first. Do not add accounts or cloud sync as part of this increment.

### P4 — Accounts & sharing (after local groundwork feels solid)

- [ ] Auth (e.g. Entra / Firebase / custom) — product decision TBD.
- [ ] Sync routes + stop library; conflict policy.
- [ ] Share stops/routes with edit permissions.
- [ ] Do **not** start this until P0 heading + layout stay green on device.

### P5 — Release / ops (manual)

- [ ] Play App Signing, Play Console, privacy policy URL.
- [ ] Crashlytics / App Insights if desired.
- [ ] Branch protection / release checklist (`docs/RELEASE_CHECKLIST.md`).

---

## Device smoke-test checklist

Run after any change to `map.html`, bearing, or map chrome:

1. Cold start → map leaves world view quickly (lastKnown / GPS), not stuck on a random city.
2. Open **empty** new route → centers on you, not a default city.
3. Follow me / delivery: turn phone left/right — triangle matches (flat on table and upright).
4. Manually twist the map with two fingers — triangle still matches phone heading.
5. Open Tip hartă — other islands/FABs **do not move**.
6. Open hamburger menu — routes list does not jump down.
7. Dark mode: islands have drop shadow, **no white halo**; secondary text readable.
8. Add stop → save to library → other route → add from library.
9. Start delivery only works with GPS; OSRM fail shows approx banner + Retry.

---

## Session / change log

### 2026-08-05 — Driving heading investigation

- Physical test reports mirrored left/right movement in driving mode with the original upright-device mapping.
- An attempted `-Z` upright-axis change stopped visible heading updates on-device and was reverted immediately. Do not reuse it.
- Inverted the final Kotlin compass azimuth once (`360 - azimuth`) after physical confirmation that heading updates were mirrored. The Leaflet marker formula (`phone heading - map bearing`) remains unchanged; do not invert it too.

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
| DB version | `data/local/AppDatabase.kt` |

---

## Reminder for future agents

> If something already works on device (heading, overlay menus, empty-route centering), **prefer additive changes**. Do not rewrite heading math, map defaults, or chrome layout “for cleanliness” without running the smoke-test list and updating this file.
