# Privacy policy (draft)

> **Status:** draft for development. Have counsel review before a commercial Play Store listing or collecting accounts.
>
> **Play Console URL:** host [`docs/privacy/index.html`](privacy/index.html) on GitHub Pages (`/docs` on `main`) so you have a public HTTPS page. Do not paste this markdown blob into Play.

**App:** Route Planner (`com.danielcioban.routeplanner`)  
**Operator:** Cioban Daniel (update with legal entity when incorporated)  
**Last updated:** 2026-08-25

## What the app stores on device

- Routes, stops, notes, and completion state (Room database)
- App settings (theme, language, units, keep-screen-on, speak turns)
- Active delivery session id (DataStore)
- Trip history (local Room log)
- Last routing error snippet if the user opens **Report a problem**
- Ongoing trip notification and lock-screen HUD while a drive is active (next stop name, remaining distance)

This data stays on the device unless the user shares a route via the system share sheet or sends a problem-report email they compose.

## What leaves the device

Depending on features used:

| Destination | Purpose | Data |
|-------------|---------|------|
| OpenStreetMap / tile providers | Show the map | Approximate map viewport requests |
| OSRM public router | Driving directions | Start/end coordinates |
| Nominatim | Address search | Search query text |
| Google Maps / Waze (optional) | External navigation | Destination coordinates via intent |
| User’s email app (optional) | Report a problem | App version, device model, last routing error, plus what they type |
| Google Play Vitals | Stability on Play installs | Crash/ANR metrics collected by Play — not Firebase |
| Firebase Crashlytics (optional) | Stack traces for the maintainer | Only if the build includes `google-services.json`. No Analytics, no Auth |

No account system is shipped yet. When authentication or cloud sync is added, this policy must be updated.

## Location

Precise location is used to center the map, add GPS stops, and navigate, including a foreground location service while a trip is active. Permission is requested at runtime. Location is not sold.

## Children’s privacy

Not directed at children under 13 (or local equivalent).

## Contact

Use the maintainer contact on the GitHub profile for privacy questions. In-app **Report a problem** (About and Settings) opens an email draft.

## Changes

Material changes will bump the “Last updated” date and be noted in release notes when the app is commercially distributed.
