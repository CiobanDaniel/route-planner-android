# Play Internal testing — click path

This repo cannot add testers or upload an AAB. Do this in [Play Console](https://play.google.com/console) while signed in as the developer account that owns `com.danielcioban.routeplanner`.

Testers **do not** need an in-app account. They need a **Google account** on the test device.

## 0. Blockers (do these first)

- [ ] App created in Console with application id `com.danielcioban.routeplanner`
- [ ] Play App Signing enrolled (upload keystore is local `keystore.properties`, never git)
- [ ] Public HTTPS privacy URL pasted in Console (GitHub Pages `/docs` → `…/privacy/`)
- [ ] [`IARC.md`](IARC.md) submitted under Policy → App content
- [ ] Feature graphic [`feature-graphic.png`](feature-graphic.png) (1024×500, 24-bit PNG)
- [ ] At least **two** phone screenshots captured locally into [`screenshots/`](screenshots/) (PNGs are gitignored) — [`capture-screenshots.ps1`](capture-screenshots.ps1)
- [ ] Listing short/full description from [`../PLAY_LISTING.md`](../PLAY_LISTING.md) (keep OSM credit)
- [ ] Release AAB: `./gradlew :app:bundleRelease` with **JDK 21**

Release builds must talk to **HTTPS** OSRM/Nominatim (or the public demos). LAN `http://192.168.x.x` will fail on the Play AAB. Debug builds may use HTTP; see [`../SELF_HOST_ROUTING.md`](../SELF_HOST_ROUTING.md).

## 1. Testers email list

1. Play Console → **Test and release → Testing → Internal testing**
2. **Testers** tab → create an email list (name it e.g. `van-day`)
3. Add Google account emails, yours first. Template: [`testers.example.csv`](testers.example.csv). A real `testers.csv` stays gitignored.
4. Copy the **opt-in URL** (join the test) and send it to testers. They must open it while logged into that Google account, then install from Play.

Internal testing is up to **100** testers. They see the app only after opt-in; it is not a public listing.

## 2. Release

1. Internal testing → **Create new release**
2. Upload the AAB from `app/build/outputs/bundle/release/`
3. Release name: `0.5.0` (or the `versionName` you shipped)
4. Release notes (EN), example:

```
First internal build: multi-stop routes, turn-by-turn, stop library, on-device data.
Allow location. On Android 16, allow promoted notifications if you want the status-bar chip.
```

5. Save → Review → **Start rollout to Internal testing**

## 3. What to tell testers

- Allow **precise location** when Start asks. Delivery does not begin without a GPS fix.
- Background trip: allow the location foreground-service prompt (in-app explainer first).
- OSM / OSRM / Nominatim attribution is in **About**.
- No sign-in. Backup is Settings → JSON export if they want a copy off the phone.

## 4. After the first install

- Console → **Quality → Android Vitals** (works without Firebase)
- Optional Crashlytics: drop `app/google-services.json` locally and ship a new AAB
