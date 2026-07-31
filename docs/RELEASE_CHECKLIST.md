# Release checklist

Use this when cutting a Play-ready or tagged GitHub release.

## 1. Version

- [ ] Bump `versionCode` (+1) and `versionName` (semver) in `app/build.gradle.kts`
- [ ] Move `[Unreleased]` notes in `CHANGELOG.md` into a new version section with today’s date
- [ ] Commit: `Release vX.Y.Z`

## 2. Quality

- [ ] CI green on `main`
- [ ] Smoke-test on a physical device: create route, search address, delivery nav, share, resume session, EN/RO
- [ ] Confirm OSM / routing still work (or document known outages)

## 3. Artifacts

- [ ] `./gradlew assembleRelease` (or Android Studio Generate Signed Bundle)
- [ ] Sign with the **upload** keystore (never commit the keystore)
- [ ] Prefer **AAB** for Play Console

## 4. GitHub

- [ ] Tag: `git tag -a vX.Y.Z -m "vX.Y.Z"` and push tags
- [ ] Create a GitHub Release from the tag; paste changelog section

## 5. Play Console (when publishing)

- [ ] Upload AAB to Internal testing first
- [ ] Store listing, screenshots, content rating, privacy policy URL
- [ ] OSM attribution visible in listing or in-app About
- [ ] Promote Internal → Closed → Production when ready

## 6. After release

- [ ] Watch Play Vitals / Crashlytics for 24–48h
- [ ] File follow-up issues for any hotfixes
