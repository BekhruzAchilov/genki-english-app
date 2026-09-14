# Genki English — Android app for the smart boards

A fullscreen app that opens the Genki English menu and nothing else.
Games download from your VPS the first time they are opened, then run
from the board's own storage forever after.

## Why this exists

Opening MENU.SWF directly in the Ruffle app gives a green screen, because
Android hands the player a single file rather than a folder, so
`loadMovie("ANIMAL.SWF")` has nowhere to look. This app runs a tiny web
server inside itself on `127.0.0.1`, so the menu's relative links resolve
exactly as they did on the Windows disc.

It also matches filenames case-insensitively, which fixes `CAN.swf` and
`XMAS.SWF` without renaming anything.

---

## Before building: add Ruffle

The project does not include Ruffle — download it once and drop it in.

1. Go to https://github.com/ruffle-rs/ruffle/releases
2. From the newest release, download the file ending in
   `-web-selfhosted.zip`
3. Unzip it
4. Copy its contents into `app/src/main/assets/ruffle/`
5. Delete `PUT_RUFFLE_FILES_HERE.txt`

You should end up with `app/src/main/assets/ruffle/ruffle.js` plus
several `.wasm` and `.js` files beside it.

Note this is the **web** build of Ruffle, not the Android APK you tested with.

---

## Building

1. Install Android Studio
2. Open this folder (`File > Open`, choose the folder containing
   `settings.gradle.kts`)
3. Wait for Gradle to finish downloading — the first time takes a while
4. `Build > Generate Signed App Bundle / APK` → choose **APK**
5. Create a new keystore when asked. **Keep the keystore file and its
   password.** Without it you cannot publish updates to this app — you
   would have to uninstall it from every board and start again.
6. Choose `release`, then Finish

The APK appears in `app/release/`. Put it on your website for teachers
to download.

---

## Server side

On the VPS, files live in `/var/www/genki/` and are served at
`https://abu-hafs.uz/games/` behind the `X-App-Key` header check.
Run `make_manifest.py` before uploading so `manifest.json` is included.

The app requests:

- `manifest.json` once at startup
- `MENU.SWF` when it opens
- each game the first time a teacher taps it

---

## First run on a board

The app asks for the server address and the key. Type them once. The key
is the long random string from the nginx config — teachers never need to
know it, you enter it when you set the board up.

After that the menu appears. Tapping a game downloads it (a few seconds)
and plays it. The same game opens instantly forever after.

## Settings

There is a faint gear in the bottom-right corner of the menu screen. It
shows how much has been cached and offers **Hammasini yuklash** — download
the whole library at once, for a board you want fully prepared in advance.

The Back button returns to the menu rather than closing the app.

---

## Things to know

- The menu's **Quit** button does nothing. Flash's `fscommand("quit")`
  has no meaning here. Use the board's home button.
- `SECURITY.SWF`, `clipart/horse.swf` and `clipart/shark.swf` are
  referenced by the menu but missing from the disc. Those buttons were
  already broken on Windows.
- Games are 1024x768 (4:3), so a widescreen board shows black bars at the
  sides. That is correct, not a fault.
- Uninstalling the app deletes the downloaded games too, since they live
  in the app's private storage.
