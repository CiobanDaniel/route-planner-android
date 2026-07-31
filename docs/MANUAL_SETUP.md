# Manual setup (cannot be fully automated from the repo)

These steps need your GitHub / Google / legal accounts. Repo scaffolding is already in place where possible.

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

## 2. Release signing

1. Generate an upload keystore (once), store it **outside** the repo (password manager / secure drive)
2. Create `keystore.properties` locally (gitignored pattern already covers `*.jks` / `*.keystore`) — do **not** commit it
3. Wire `signingConfigs` in `app/build.gradle.kts` when you are ready for Play
4. For CI-built releases later: store keystore + passwords as GitHub Actions secrets

## 3. Google Play Console

1. Create the app listing for `com.danielcioban.routeplanner`
2. Complete store presence, content rating, target audience, privacy policy URL
3. Upload an **AAB** to Internal testing
4. Host a real privacy policy (start from `docs/PRIVACY_DRAFT.md`, then counsel review)

## 4. Crash reporting & analytics

1. Create a Firebase project (or use Play Vitals only at first)
2. Add the Android app; download `google-services.json` (already gitignored)
3. Add Crashlytics Gradle plugin + dependency
4. Decide analytics policy before shipping (update privacy draft)

## 5. Production map / routing

Public OSRM + Nominatim are fine for development only.

1. Plan self-hosted or paid routing / geocoding before scale
2. Confirm tile provider ToS / billing
3. Show OSM attribution in-app (About screen still to build)

## 6. Legal entity & store identity

1. Decide personal vs company Play developer account
2. Finalize Privacy Policy + Terms of Service URLs
3. Keep `THIRD_PARTY_NOTICES.md` updated when adding SDKs

## 7. First public tags (optional cleanup)

Historical tags may not exist yet. After merging 0.3:

```bash
git tag -a v0.3.0 -m "v0.3.0"
git push origin v0.3.0
```

Create a GitHub Release from that tag using `CHANGELOG.md`.
