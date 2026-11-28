# Privacy policy (draft)

> **Status:** draft for development. Have counsel review before a commercial Play Store listing or collecting accounts.
>
> **Play Console URL:** host [`docs/privacy/index.html`](privacy/index.html) on GitHub Pages (`/docs` on `main`) so you have a public HTTPS page. Do not paste this markdown blob into Play.

**App:** Route Planner (`com.danielcioban.routeplanner`)  
**Operator:** Cioban Daniel (update with legal entity when incorporated)  
**Last updated:** 2026-10-13

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
| Firebase Authentication (optional) | Google sign-in | Google account basic profile and Firebase account identifier, only after the user chooses Google sign-in. Routes stay on-device; no cloud sync. |
| Firebase Analytics (optional) | Improve general app flows | Pseudonymous analytics identifiers and limited app interaction events, only after the user enables it in Settings. No route names, addresses, coordinates, notes, e-mail, ads, or personalization. |
| Firebase Crashlytics (optional) | Stack traces for the maintainer | Technical crash diagnostics, only after the user enables it in Settings and the build is configured for Firebase. |

Google account deletion is available in the app. It deletes the Firebase Authentication account and does not delete device-local routes because cloud sync is not available. This policy must be expanded before any remote route storage is added.

## Location

Precise location is used to center the map, add GPS stops, and navigate, including a foreground location service while a trip is active. Permission is requested at runtime. Location is not sold.

## Children’s privacy

Not directed at children under 13 (or local equivalent).

## Contact

Use the maintainer contact on the GitHub profile for privacy questions. In-app **Report a problem** (About and Settings) opens an email draft.

## Changes

Material changes will bump the “Last updated” date and be noted in release notes when the app is commercially distributed.
