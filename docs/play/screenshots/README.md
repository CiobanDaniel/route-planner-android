# Phone screenshots for Play

Play needs **at least two** portrait phone screenshots (JPEG or 24-bit PNG). Capture a **real** build (emulator or device). Do not upload mockups.

Put files here as `01-home.png`, `02-driving.png`, `03-about.png`, … then upload them under **Main store listing → Phone screenshots**. Image files in this folder are **gitignored** so a personal map or depot does not land on GitHub. The shot list README stays in git.

Shot list and OSM-credit rule: [`../PLAY_LISTING.md`](../PLAY_LISTING.md).

## Capture

1. Run the app (debug is fine for shots; use a Romania-looking map area).
2. USB debugging or an emulator with `adb` on `PATH`.
3. From the repo:

```powershell
pwsh docs/play/capture-screenshots.ps1
```

That pulls `screencap` into this folder with the next free `NN-capture.png` name. Rename to the shot list.

Show the map attribution chip or **About** in at least one shot.

Do **not** add 7-inch / 10-inch screenshots unless the listing claims tablets.
