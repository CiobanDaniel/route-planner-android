# Documentation map

Where planning and ops notes live, what is still true, and **what should go to GitHub** vs stay on this PC.

Related: [`AGENTS.md`](../AGENTS.md) (for assistants), [`CONTRIBUTING.md`](../CONTRIBUTING.md).

**Current cut:** local **0.5.0**. Data stays on-device. Do not start Bucket 12 (real Google/cloud auth) until asked.

---

## What to put on `main` (GitHub)

These are the product and the instructions a future clone needs. They contain no secrets.

| File | Keep on GitHub? | Why |
|------|-----------------|-----|
| [`README.md`](../README.md) | Yes | How to build |
| [`LICENSE`](../LICENSE), [`CHANGELOG.md`](../CHANGELOG.md), [`SECURITY.md`](../SECURITY.md) | Yes | Normal open-source surface |
| [`CONTRIBUTING.md`](../CONTRIBUTING.md), [`AGENTS.md`](../AGENTS.md) | Yes | How humans and agents work |
| [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md) | Yes | OSM / OSRM / tile credit (Play cares) |
| [`DEV_STATUS.md`](DEV_STATUS.md) | **Yes** | Protected map/heading/overlay rules. Long session log is noisy but it is how fixes survive a context reset. Do not keep it only on this PC. |
| [`PRODUCT_BACKLOG.md`](PRODUCT_BACKLOG.md) | Yes | Idea menu; `[x]` / `[ ]` / `[-]` |
| [`adr/`](adr/) | Yes | Why Leaflet, local-first sync, library as source of truth |
| [`MANUAL_SETUP.md`](MANUAL_SETUP.md), [`RELEASE_CHECKLIST.md`](RELEASE_CHECKLIST.md) | Yes | Play / GitHub / signing **steps** (no passwords) |
| [`PLAY_LISTING.md`](PLAY_LISTING.md), [`play/IARC.md`](play/IARC.md), [`play/INTERNAL_TESTING.md`](play/INTERNAL_TESTING.md) | Yes | Paste-ready Console copy |
| [`play/feature-graphic.png`](play/feature-graphic.png) (+ svg/html) | Yes | Listing asset, no personal map |
| [`play/testers.example.csv`](play/testers.example.csv) | Yes | Template only |
| [`play/capture-screenshots.ps1`](play/capture-screenshots.ps1), [`play/screenshots/README.md`](play/screenshots/README.md) | Yes | How to capture |
| [`PRIVACY_DRAFT.md`](PRIVACY_DRAFT.md), [`privacy/index.html`](privacy/index.html) | Yes | Draft + Pages-ready HTML. Counsel before production. |
| [`SELF_HOST_ROUTING.md`](SELF_HOST_ROUTING.md), [`deploy/routing/`](../deploy/routing/) | Yes | Romania Docker pack **without** OSM extracts |
| [`app/schemas/`](../app/schemas/) | Yes | Room schema JSON when the DB version changes |
| [`docs/assets/`](assets/) | Yes | Launcher icon sources |

CI (`.github/workflows/android-ci.yml`) already runs on PRs to `main`. Pushing these docs with the 0.5.0 working tree is appropriate; **tagging 0.5.0** is still only when you ask.

---

## What stays on this device

Gitignored. A clone does not need them, and some would leak location or accounts.

| Thing | Why local |
|-------|-----------|
| `local.properties`, `keystore.properties`, `*.jks` | SDK path, upload key, passwords |
| `app/google-services.json` | Firebase project |
| `docs/play/screenshots/*.png` | Play shots often show **your** map / depot |
| `docs/play/testers.csv` | Real tester emails (use the `.example` file in git) |
| `deploy/routing/data/` | Geofabrik / OSRM graph (hundreds of MB) |
| `.idea/`, `.cursor/`, `.gradle/`, `build/` | IDE and build cache |
| Encrypted backups (`*.rpenc`) if saved in the repo folder | User data |

---

## What is still true vs fossil

| Claim | Status |
|-------|--------|
| Kotlin/Android is the product; Expo repo is reference only | True |
| EN + RO only | **False** — Settings also has FR, DE, IT, ES, PT |
| Room v8 / backup v2 | **False** — Room **v12**, JSON backup **v6** |
| Supported release is 0.3.x | **False** — treat **0.5.x** as current |
| Bucket 12 / Google Sign-In is in the build | **False** — copy only, button disabled |
| `DEV_STATUS` “Next P0” about committing 0.3.1 | Fossil — current next is below |

**Current next (you, not more features):** device smoke-test; GitHub Pages + counsel for privacy; Play Console clicks; production OSRM over HTTPS; commit/PR when you ask. Do **not** start Bucket 12 until then.

---

## File roles (short)

- **`DEV_STATUS.md`** — Do not regress (islands, heading, GPS-gated start, dark elevation). Session log at the bottom. Smoke-test list. Assistants read this first for map/chrome/Room.
- **`PRODUCT_BACKLOG.md`** — Buckets 1–12. Checkboxes, not a promise.
- **`CHANGELOG.md`** — User-visible history. `[Unreleased]` until you cut a tag.
- **Play / privacy / routing** — Ops. Console and VPS still need your accounts.

Do not add a second “master plan” file. Update these in place when something ships or is skipped.
