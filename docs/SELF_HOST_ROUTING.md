# Production OSRM + Nominatim (Romania)

Public `router.project-osrm.org` and `nominatim.openstreetmap.org` are **demos**. They rate-limit and go down. This folder runs the same HTTP APIs the app already speaks, using the **Romania** Geofabrik extract (the van-day home market).

The app is not hardcoded to these containers. After they are up, put the URLs in gitignored `local.properties` and **rebuild**.

Related: [`MANUAL_SETUP.md`](MANUAL_SETUP.md) §5, [`local.properties.example`](../local.properties.example).

## What you get

| Service | Image | Host port | App setting |
|---------|--------|-----------|-------------|
| OSRM car graph | `ghcr.io/project-osrm/osrm-backend:v5.27.1` | `5000` | `osrm.base.url` |
| Nominatim search/reverse | `mediagis/nominatim:4.4` | `8080` | `nominatim.base.url` |

OSRM URL shape matches the app: `{base}/route/v1/driving/…`, `{base}/table/v1/…`, `{base}/nearest/v1/…`.

Nominatim URL shape matches the app: `{base}/search`, `{base}/reverse`, `{base}/status`.

## Hardware

| Stack | RAM | Disk | First run |
|-------|-----|------|-----------|
| OSRM Romania (car) | ~8 GB | ~8 GB | extract 15–40 min |
| Nominatim Romania (`address` style, no Wikipedia) | ~16 GB | ~40 GB | **hours** |

Bike/walk Settings still hit the **car** graph (OSRM ignores the profile name on a single `osrm-routed`). That is enough for a van. Extra bicycle/foot containers are optional later.

Avoid-tolls / avoid-motorways need OSRM `exclude=` classes. Stock `car.lua` in this image supports `toll` and `motorway`; confirm with a real route after the graph is built.

## 1. Install Docker Desktop

Enable Linux containers. From `deploy/routing/`:

## 2. Build the OSRM graph (once)

Windows:

```powershell
pwsh .\prepare-osrm.ps1
```

macOS / Linux:

```bash
chmod +x prepare-osrm.sh
./prepare-osrm.sh
```

This downloads `romania-latest.osm.pbf` from Geofabrik (do not commit it) and writes `*.osrm*` under `data/`.

## 3. Start routing

```bash
docker compose up -d osrm
```

Check (Timișoara):

```bash
curl "http://127.0.0.1:5000/nearest/v1/driving/21.23,45.75?number=1"
```

You should see `"code":"Ok"`.

## 4. Start search (optional the first week)

```bash
docker compose --profile nominatim up -d
```

Watch logs until Nominatim finishes the import (`docker compose logs -f nominatim`). Then:

```bash
curl "http://127.0.0.1:8080/status"
curl "http://127.0.0.1:8080/search?q=Timisoara&format=json&limit=1"
```

Set a real `NOMINATIM_PASSWORD` in the environment before the first import if this host will be on the public internet.

## 5. Point the Android app at the stack

Edit **`local.properties`** (never commit it). Rebuild so `BuildConfig` updates. Settings → Routing and search should show the new hosts; tap **Check routing and search**.

**Emulator on the same PC as Docker:**

```
osrm.base.url=http://10.0.2.2:5000
nominatim.base.url=http://10.0.2.2:8080
```

`10.0.2.2` is the host loopback from the Android emulator. `localhost` inside the emulator is the emulator itself.

**Physical phone on the same Wi-Fi:** use the PC’s LAN IP, and allow the phone through the Windows firewall for ports 5000 and 8080:

```
osrm.base.url=http://192.168.1.10:5000
nominatim.base.url=http://192.168.1.10:8080
```

**Cleartext HTTP:** Debug builds can talk to `http://` on the LAN. **Release / Play** should use **HTTPS** (Caddy on a VPS). Cleartext to a random IP will fail on a minified release unless you add a network-security exception — do not do that; put TLS in front instead. See `Caddyfile.example`.

Optional: keep the public demos as fallback while you soak-test:

```
osrm.fallback.url=https://router.project-osrm.org
nominatim.fallback.url=https://nominatim.openstreetmap.org
```

Remove those fallbacks once the self-hosted pair is boringly reliable (Nominatim usage policy still applies to the public demo).

## 6. Production VPS

1. Run the same compose on a VPS in the EU (GDPR: GPS goes to *your* routing host).
2. DNS: `route.yourdomain` → OSRM, `search.yourdomain` → Nominatim (or one box, two hostnames).
3. TLS: copy `Caddyfile.example`, replace the hostnames, run Caddy.
4. Set `osrm.base.url` / `nominatim.base.url` to those `https://` URLs, rebuild the **release** AAB.

Do not expose Nominatim to the whole internet without a rate limit (Caddy `rate_limit`, or a reverse proxy). The app already waits ~1.1 s between Nominatim calls.

## Updates

Geofabrik ships a new PBF regularly. Re-run `prepare-osrm.ps1` / `.sh` (it skips the download if the file exists — delete `data/romania-latest.osm.pbf` first to refresh), then `docker compose up -d osrm --force-recreate`.

Nominatim uses `REPLICATION_URL` for diffs after the first import.

## Attribution

Map data © OpenStreetMap contributors. OSRM and Nominatim are used under their licenses. Keep the Play listing and in-app About credit; see [`PLAY_LISTING.md`](PLAY_LISTING.md) and [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md).
