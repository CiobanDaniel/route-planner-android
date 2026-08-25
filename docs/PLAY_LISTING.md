# Play Console listing copy

Paste-ready text and a shot list for **Internal testing**. Edit tone if you like; **keep the OSM / routing attribution**.

Related: in-app About + map chip, [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md), [`docs/RELEASE_CHECKLIST.md`](RELEASE_CHECKLIST.md), [`docs/MANUAL_SETUP.md`](MANUAL_SETUP.md).

**You still do these in Play Console** (this repo cannot add testers or click Submit): create the app, Internal testing email list, upload AAB, content rating questionnaire, upload the 1024×500 PNG and phone screenshots. Files to upload are under [`docs/play/`](play/).

## Short description (80 characters max)

```
Multi-stop delivery routes. Map data © OpenStreetMap contributors.
```

(80 characters including the period.)

## Full description

```
Route Planner is a local-first multi-stop delivery app: build a route, drive it with turn-by-turn, mark stops done, and keep a stop library on the phone.

No sign-in is required. Routes, tasks, and trip history stay on the device unless you export a backup or share a route.

Maps: data © OpenStreetMap contributors. Everyday streets use Humanitarian OSM (HOT) tiles hosted by OSM France. Night / dark streets use CARTO Dark Matter. Satellite and terrain use Esri. Search uses Nominatim; turn-by-turn uses OSRM. See the in-app About screen and THIRD_PARTY_NOTICES.
```

Romanian (if you add a RO listing):

```
Route Planner e o aplicație local-first pentru livrări cu mai multe opriri: construiești ruta, o conduci cu ghidare, bifezi opririle și ții o bibliotecă de locuri pe telefon.

Nu e nevoie de cont. Rutele, sarcinile și istoricul rămân pe dispozitiv până exporti un backup sau partajezi o rută.

Hărți: date © contribuitorii OpenStreetMap. Străzile de zi cu zi folosesc dale Humanitarian OSM (HOT) găzduite de OSM France. Noaptea / tema întunecată folosesc CARTO Dark Matter. Satelitul și terenul folosesc Esri. Căutarea folosește Nominatim; ghidarea folosește OSRM. Vezi ecranul Despre din aplicație și THIRD_PARTY_NOTICES.
```

## Privacy policy URL (Play)

Play needs a **public HTTPS page**, not a GitHub blob.

1. Host [`docs/privacy/index.html`](privacy/index.html): GitHub → Settings → Pages → Deploy from branch `main` / folder `/docs`. The page is then `https://ciobandaniel.github.io/route-planner-android/privacy/`
2. Paste that URL in Play Console.
3. Optionally set `privacy.policy.url` in `local.properties` to the same URL and rebuild so About / Settings match.
4. Counsel review before collecting emails or a production listing. The HTML is still a draft until then.

## Feature graphic (1024 × 500)

Play: **Main store listing → Feature graphic**. PNG or JPEG, **exactly 1024×500**, 24-bit (no alpha), no device frames, no Play badge.

Upload [`docs/play/feature-graphic.png`](play/feature-graphic.png). Vector source: [`feature-graphic.svg`](play/feature-graphic.svg). Regenerating the PNG: open [`feature-graphic.html`](play/feature-graphic.html) at 100% zoom or redraw from the SVG.

## Screenshots (phone — at least two)

Capture on a physical phone or emulator (portrait). Script: [`docs/play/capture-screenshots.ps1`](play/capture-screenshots.ps1) → [`docs/play/screenshots/`](play/screenshots/). Show OSM credit in at least one shot (map chip or About). Captured PNGs are **gitignored** (they often show your real map); keep them on this PC and upload in Console.

| # | File name | Screen | Why |
|---|-----------|--------|-----|
| 1 | `01-home.png` | Home route list over the map | Core product |
| 2 | `02-driving.png` | Driving HUD (follow-me, next stop, Done) | Delivery |
| 3 | `03-about.png` | About (OSM / OSRM / Nominatim paragraph) | Attribution for reviewers |
| 4 | `04-library.png` | Stop library (optional) | Places |
| 5 | `05-routing.png` | Settings → Routing and search (optional) | Honesty about demo vs self-hosted |

7-inch / 10-inch screenshots only if you claim tablets.

## Content rating (IARC)

Full answer sheet: [`docs/play/IARC.md`](play/IARC.md). Submit in Console (**Policy → App content**). Do not invent a rating certificate in git.

## Internal testing testers

Click-by-click: [`docs/play/INTERNAL_TESTING.md`](play/INTERNAL_TESTING.md). Email list template: [`docs/play/testers.example.csv`](play/testers.example.csv). This repo cannot add testers for you.

## Crashes: Play Vitals vs Crashlytics

- **Android Vitals** (Play Console → Quality) works with **no Firebase**. Crash rate and ANRs from Play installs, including Internal testing.
- **Crashlytics** is optional: drop gitignored `app/google-services.json`, rebuild. Gradle wires Crashlytics **only** then, and **excludes Analytics and Auth**. Mapping upload is on for minified release.

In-app **Report a problem** (About and Settings) emails version + last routing error. Set `support.email` in `local.properties` so the To: line is filled.
