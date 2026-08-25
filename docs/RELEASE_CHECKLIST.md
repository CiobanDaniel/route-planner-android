# Release checklist

Use this when cutting a Play-ready or tagged GitHub release.

## 1. Version

- [ ] Bump `versionCode` (+1) and `versionName` (semver) in `app/build.gradle.kts`
- [ ] Move `[Unreleased]` notes in `CHANGELOG.md` into a new version section with today’s date
- [ ] Commit: `Release vX.Y.Z`

## 2. Quality

- [ ] CI green on `main`
- [ ] Smoke-test on a physical device: create route, search address, delivery nav, share, resume session, switch language in Settings
- [ ] Android 16: start a trip, press Home — shade **Driving** notification, and Live Update **chip** if the phone supports it (allow promoted notifications if asked)
- [ ] Two-device (or clear-data) JSON backup: library links, tasks, round-trip, geofence, trip history
- [ ] Confirm OSM / routing still work (or document known outages). If using public demos, say so.

## 3. Artifacts

- [ ] `keystore.properties` present locally (never committed)
- [ ] `./gradlew :app:bundleRelease` with **JDK 21**
- [ ] Sign with the **upload** keystore; Play App Signing holds the app-signing key
- [ ] Prefer **AAB** for Play Console

## 4. GitHub

- [ ] Tag: `git tag -a vX.Y.Z -m "vX.Y.Z"` and push tags
- [ ] Create a GitHub Release from the tag; paste changelog section

## 5. Play Console (Internal first — no in-app login required)

- [ ] Upload AAB to **Internal testing**; add tester emails — [`docs/play/INTERNAL_TESTING.md`](play/INTERNAL_TESTING.md)
- [ ] Store listing draft: description, [`docs/play/feature-graphic.png`](play/feature-graphic.png), ≥2 phone screenshots captured locally ([`docs/play/screenshots/`](play/screenshots/) — PNGs are gitignored)
- [ ] Content rating (IARC questionnaire) — [`docs/play/IARC.md`](play/IARC.md)
- [ ] Privacy policy **hosted** URL: GitHub Pages `/docs` → [`docs/privacy/index.html`](privacy/index.html) (`…/privacy/`). Counsel review before collecting emails. Optional `privacy.policy.url` in `local.properties`
- [ ] OSM attribution in the Play listing — paste from [`docs/PLAY_LISTING.md`](PLAY_LISTING.md); About + map chip already show credit in-app
- [ ] Production OSRM/Nominatim: [`docs/SELF_HOST_ROUTING.md`](SELF_HOST_ROUTING.md); set `local.properties` HTTPS URLs before the Play AAB
- [ ] Optional Crashlytics: drop `app/google-services.json` and rebuild (Analytics/Auth excluded). Mapping upload is on for minified release. Keep `app/build/outputs/mapping/release/` with the AAB if you ever need to re-process crashes.
- [ ] Watch **Android Vitals** in Play Console (works without Firebase)
- [ ] Promote Internal → Closed → Production when ready

## 6. After release

- [ ] Watch Play Vitals (and Crashlytics if enabled) for 24–48h
- [ ] File follow-up issues for any hotfixes
