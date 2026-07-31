# Contributing

Thanks for helping with Route Planner. This guide keeps the repo workable for a small team.

## Prerequisites

- Android Studio (current stable)
- **Gradle JDK 21** (not JDK 25 — Gradle 8.14.x does not support 25 yet)
- Android SDK / emulator or a physical device
- Git

Confirm JDK in Android Studio:  
**Settings → Build Tools → Gradle → Gradle JDK → 21**

## Branching

| Branch | Purpose |
|--------|---------|
| `main` | Stable, releasable history |
| `route-planner/versionX.Y` or `feature/…` | Active work |
| `fix/…` | Bug fixes |

1. Branch from an up-to-date `main`
2. Keep PRs focused (one concern when possible)
3. Open a PR into `main` with a short summary + test plan
4. Do not force-push `main`

## Commit messages

Prefer concise, imperative subjects:

- `Add Nominatim address search`
- `Fix AppCompat theme crash on launch`
- `Bump version to 0.3.0`

## Before you open a PR

- [ ] `./gradlew assembleDebug testDebugUnitTest lintDebug` passes locally (or CI is green)
- [ ] No secrets committed (`local.properties`, keystores, `google-services.json`)
- [ ] User-facing strings added to `values/strings.xml` **and** `values-ro/strings.xml` when UI text changes
- [ ] `CHANGELOG.md` updated under `[Unreleased]` for user-visible or process-visible changes
- [ ] Version bumped in `app/build.gradle.kts` only when cutting a release (maintainers)

## Code style

- Kotlin + Jetpack Compose; match existing patterns (islands, ViewModels, repositories)
- Prefer small, readable functions over clever abstractions
- Avoid drive-by refactors in feature PRs

## Reviews

- At least one approving review before merge once the team has 2+ people
- Author should not merge their own PR when reviewers are available

## Questions

Open a GitHub Discussion or Issue. For security problems, see [SECURITY.md](SECURITY.md).
