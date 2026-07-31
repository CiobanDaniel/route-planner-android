# Privacy policy (draft)

> **Status:** draft for development. Have counsel review before a commercial Play Store listing or collecting accounts / analytics.

**App:** Route Planner (`com.danielcioban.routeplanner`)  
**Operator:** Cioban Daniel (update with legal entity when incorporated)  
**Last updated:** 2026-07-31

## What the app stores on device

- Routes, stops, notes, and completion state (Room database)
- App settings (theme, language, units, keep-screen-on)
- Active delivery session id (DataStore)

This data stays on the device unless the user shares a route via the system share sheet.

## What leaves the device

Depending on features used:

| Destination | Purpose | Data |
|-------------|---------|------|
| OpenStreetMap / tile providers | Show the map | Approximate map viewport requests |
| OSRM public router | Driving directions | Start/end coordinates |
| Nominatim | Address search | Search query text |
| Google Maps / Waze (optional) | External navigation | Destination coordinates via intent |

No account system is shipped yet. When authentication or cloud sync is added, this policy must be updated.

## Location

Precise location is used to center the map, add GPS stops, and navigate. Permission is requested at runtime. Location is not sold.

## Children’s privacy

Not directed at children under 13 (or local equivalent).

## Contact

Use the maintainer contact on the GitHub profile for privacy questions.

## Changes

Material changes will bump the “Last updated” date and be noted in release notes when the app is commercially distributed.
