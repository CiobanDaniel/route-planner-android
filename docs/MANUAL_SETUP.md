# Manual setup (cannot be fully automated from the repo)

These steps need your GitHub / Google / legal / hosting accounts. Repo scaffolding is in place
where a file in git can help. **0.5.0 can ship to Internal testing without sign-in or cloud.**

## 1. GitHub repository settings

1. Open https://github.com/CiobanDaniel/route-planner-android/settings
2. **Branches → Add branch protection rule** for `main`:
   - Require a pull request before merging
   - Require status checks to pass: `Build, test, lint` (after CI has run once)
   - Require conversation resolution
   - Do **not** allow force pushes
3. **General → Features**: enable Issues (and Discussions if you want)
4. Confirm Actions are enabled so `.github/workflows/android-ci.yml` can run
5. After the first Dependabot PR appears, merge or tune ignore rules as needed

Optional:

```bash
gh api repos/CiobanDaniel/route-planner-android/branches/main/protection \
  --method PUT \
  --input - <<'EOF'
{
  "required_status_checks": { "strict": true, "contexts": ["Build, test, lint"] },
  "enforce_admins": true,
  "required_pull_request_reviews": { "required_approving_review_count": 1 },
  "restrictions": null,
  "allow_force_pushes": false,
  "allow_deletions": false
}
EOF
```

(Requires admin rights and that the CI check name matches exactly after the first run.)

## 2. Release signing (Play App Signing)

Play App Signing is a Console setting: Google holds the app-signing key; you keep an **upload**
keystore. The repo already reads `keystore.properties` (gitignored) and signs the release
build when that file exists.

1. Generate an upload keystore once, store it **outside** the repo:

   ```bash
   keytool -genkey -v -keystore upload-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
   ```

2. Copy `keystore.properties.example` to `keystore.properties` at the **repo root**. Set
   `storeFile` to an absolute path (or a path relative to the `app/` module).
3. `./gradlew :app:bundleRelease` (JDK 21). Upload the AAB to Play.
4. In Play Console: **Setup → App signing** — enroll Play App Signing if not already.
5. For CI-built releases later: store the keystore + passwords as GitHub Actions secrets.
   Never commit `*.jks`, `*.keystore`, or `keystore.properties`.

## 3. Google Play Console — Internal testing without accounts

Do this before promising a cloud product. Testers do not need in-app login.

1. Create the app for `com.danielcioban.routeplanner` if it does not exist.
2. **Internal testing** track: email list + rollout — [`docs/play/INTERNAL_TESTING.md`](play/INTERNAL_TESTING.md). Upload the 0.5.0 AAB, add yourself.
3. **Store listing** (can stay draft until production):
   - Short / full description from [`docs/PLAY_LISTING.md`](PLAY_LISTING.md)
   - Feature graphic: [`docs/play/feature-graphic.png`](play/feature-graphic.png) (1024×500)
   - At least two **phone** screenshots (home list, driving HUD). Capture locally: [`docs/play/capture-screenshots.ps1`](play/capture-screenshots.ps1). PNGs are gitignored. 7-inch / 10-inch only if you claim tablets.
4. **Content rating**: questionnaire answers in [`docs/play/IARC.md`](play/IARC.md) (utility / maps; no user-generated chat).
5. **Target audience**: 18+ unless you have a kids-policy reason not to.
6. **Privacy policy URL** (Play requires a public HTTPS page, not a repo file):
   - Hosted page in git: [`docs/privacy/index.html`](privacy/index.html)
   - Enable **GitHub Pages** on `main` / folder `/docs` → `https://ciobandaniel.github.io/route-planner-android/privacy/`
   - Paste that URL in Play Console. Optionally set `privacy.policy.url` in `local.properties` so About / Settings match.
   - In-app default until you override: GitHub `docs/PRIVACY_DRAFT.md`
   - Counsel review before collecting emails or a production listing.
7. Store listing copy and upload files: [`docs/PLAY_LISTING.md`](PLAY_LISTING.md), [`docs/play/`](play/)
8. OSM attribution is already in-app (About + map chip).

Play **Vitals** (crash rates, ANRs) start from Internal testing even with **no Firebase**. That is enough to ship Internal without Crashlytics.

## 4. Crash reporting (Crashlytics only — no Analytics, no Auth)

Play Vitals without Firebase is already useful. Crashlytics is better stack traces.

1. Create a Firebase project, add Android app `com.danielcioban.routeplanner`.
2. Download `google-services.json` into **`app/`** (already gitignored).
3. Rebuild. Gradle applies Google Services + Crashlytics **only when that file exists**,
   so CI stays green without Firebase. Analytics and Auth are **excluded** even if a
   transitive dependency tries to pull them in. Do not add `firebase-analytics` or `firebase-auth`.
4. In the Firebase console, do not enable Google Analytics for this app.
5. Update `docs/PRIVACY_DRAFT.md` / `docs/privacy/index.html` if the Crashlytics sentence
   is no longer accurate, then re-host.
6. Do **not** commit `google-services.json`.

## 5. Production routing / geocoding

Public `router.project-osrm.org` and `nominatim.openstreetmap.org` are demos. They will
fail in the field under load. Accounts/cloud will not help if directions die.

**In this repo:** Romania self-host pack — [`docs/SELF_HOST_ROUTING.md`](SELF_HOST_ROUTING.md) and [`deploy/routing/`](../deploy/routing/). Prepare the OSRM graph, `docker compose up -d osrm`, optionally `--profile nominatim`. Play/release builds need **HTTPS** on a VPS (`Caddyfile.example`). Debug builds may use `http://10.0.2.2:5000` (cleartext allowed in the debug source set only).

Then in `local.properties` (gitignored; see `local.properties.example`):

```
osrm.base.url=https://your-osrm.example
nominatim.base.url=https://your-nominatim.example
osrm.fallback.url=https://your-osrm-backup.example
nominatim.fallback.url=https://your-nominatim-backup.example
```

No trailing slash. Rebuild so `BuildConfig` updates. Fallback URLs are optional: if the primary returns 429, times out, or 5xx, the app tries the second host, then the last successful road polyline, then a straight line. Settings → Routing and search can ping both.

A small reverse proxy in front of self-hosted OSRM + Nominatim is the usual pattern.
Confirm tile provider ToS. OSM attribution is already in About; store listing copy is in [`docs/PLAY_LISTING.md`](PLAY_LISTING.md).

## 6. Legal entity & store identity

1. Decide personal vs company Play developer account
2. Finalize Privacy Policy + Terms of Service URLs
3. Keep `THIRD_PARTY_NOTICES.md` updated when adding SDKs

## 7. First public tags (optional cleanup)

After merging a version, tag from `CHANGELOG.md`:

```bash
git tag -a v0.5.0 -m "v0.5.0"
git push origin v0.5.0
```

Create a GitHub Release from that tag using the changelog section.
