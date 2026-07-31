# Route Planner — Android

Multi-stop route planning for deliveries. Display name in the app: **Route Planner**.

## Run in Android Studio (emulator)

1. **File → Open** this folder: `C:\Users\danie\Desktop\PersonalProjects\route-planner`  
   (not a copy under `Personal Projects` with a space)
2. Trust the project if asked, then wait for **Gradle sync** to finish.
   - If inspections say `minSdkVersion is 1` or `Unresolved class MainActivity`, sync did not finish:  
     **File → Sync Project with Gradle Files**
3. Create a virtual device (once): **Tools → Device Manager → Create Device** → Pixel → API 34/35 → Finish.
4. Toolbar: select run config **app** (or `RoutePlanner.app.main`) and your emulator → green **Run**.

If Run is disabled: **Run → Edit Configurations → + → Android App**, module `RoutePlanner.app.main`, Launch = Default Activity.

Gradle JDK should be **jbr-21** (Android Studio’s embedded JDK):  
**Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK**.

## What this cycle includes

- Full-bleed **map background** (Leaflet + OpenStreetMap in a WebView — stable on emulators)
- **Floating island** UI (rounded panels/buttons with light + dark sculpted shadows)
- Create / edit / delete routes with ordered stops
- **Long-press map** to drop a stop; **GPS button** to add current location
- **Start delivery** mode: next stop, mark done, approx distance/bearing
- Local Room storage (works offline for route data)

Maps, shared multi-user sync, and onboard turn-by-turn come later.

## Stack

| Piece | Choice |
|--------|--------|
| UI | Kotlin, Jetpack Compose, Material 3 |
| Storage | Room (on-device) |
| Package | `com.danielcioban.routeplanner` |
| Auth / sync (later) | Firebase Auth + Firestore |
| Maps (later) | MapLibre + OSM recommended first; Google optional |

## Maps & weak-signal notes (planned)

Romanian side roads are often missing from map data. The app already lets you **store exact coordinates** for those stops. Later:

- Route on known roads as far as possible, then show **approx. distance/direction** for the last stretch
- Prefer **local routing** when the network drops (cached tiles / onboard routing engine)

## Project layout

```
app/src/main/java/com/danielcioban/routeplanner/
  MainActivity.kt
  data/          # Room + repository
  ui/            # screens & navigation
  util/          # geo helpers
```
