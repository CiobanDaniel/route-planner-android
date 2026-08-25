# IARC / Play content rating — answer sheet

Play Console cannot be filled from this repo. Copy these answers into **Policy → App content → Content rating**. Submit there. Do not invent a rating certificate or PEGI/ESRB icon in git.

Official how-to: [Content rating requirements](https://support.google.com/googleplay/android-developer/answer/188189).

This sheet matches **Route Planner 0.5** as shipped: local-first courier app, no sign-in, no ads, no in-app chat, no UGC feed.

## Before the questionnaire

1. Host a public HTTPS **privacy policy** (GitHub Pages `/docs` → `docs/privacy/index.html`). Play blocks rating/listing without a URL.
2. **Target audience and content** (separate card on App content): this app is **not designed for children**. Pick 18+ (or the highest adult-only band Play shows). Do **not** tick under-13 / Families.
3. **Ads**: declare **No ads**.
4. Email on the rating form: an address you read (IARC sends the certificate there).

## Category

Choose **Utility** (or **Maps & navigation** / **Tools** if that is what the form offers). **Not** a game. **Not** a social network.

## Questionnaire answers

Wording in Console changes. Match **intent**, not a screenshot of this table.

| Topic | Answer | Why |
|--------|--------|-----|
| User-generated content visible to other users | **No** | Routes stay on the device unless the user exports/shares. No public feed. |
| Users can communicate / chat / social | **No** | SMS/call to a customer opens the **system** app for that stop’s phone number. There is no in-app messenger. |
| Users share location with other users | **No** | GPS is for on-device navigation. Coordinates go to **your** OSRM/Nominatim (or the public demos) as routing/search requests, not to other drivers. |
| Users can find / meet strangers | **No** | |
| Violence, blood, weapons as entertainment | **No** | Map pins and a van HUD only. |
| Sexual content / nudity | **No** | |
| Profanity / hate speech as content | **No** | |
| Drugs, alcohol, tobacco as content | **No** | Optional COD amount is a delivery field, not substance content. |
| Gambling / simulated gambling | **No** | |
| Digital or physical goods for sale **in the app** | **No** | No Play Billing, no catalog. |
| Users can buy random items / loot boxes | **No** | |
| Ads, including third-party ad SDKs | **No** | Crashlytics is optional and Analytics/Auth are excluded. |
| The app is a web browser or unrestricted web view | **No** | Leaflet map tiles only, not general browsing. |
| Users can publish or moderate other people’s posts | **No** | |
| Horror / fear | **No** | |

If a question asks whether location is **collected**: GPS is used **on the device** for navigation. If the form distinguishes “shared with the developer’s servers”, say routing/search requests go to the hosts in Settings (self-hosted or OSM demos). The privacy page must match.

## After Submit

- Play shows calculated ratings (PEGI, ESRB, USK, …). For a utility maps app with all **No**s above, expect a **low** rating (often PEGI 3 / ESRB Everyone). That is IARC’s call, not ours.
- If a rating looks wrong, retake the questionnaire or appeal via the link in the IARC email. Do not paste fake rating art into the store listing.
- Re-take the questionnaire when you add chat, ads, UGC, or payments.

## App access / other App content cards

| Card | This app |
|------|----------|
| App access | No login. Testers install from the Internal testing opt-in link. |
| Ads | No |
| Content ratings | This questionnaire |
| Target audience | 18+, not for children |
| News | Not a news app |
| COVID-19 | No |
| Data safety | Fill from `docs/privacy/index.html` (location, approximate network, no account) |
