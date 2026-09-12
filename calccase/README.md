# Calc Case — Android app

A full-screen WebView wrapping the calculator-case rig. Built so the rig runs on the
phone with no address bar, no navigation away, and the screen kept awake — the three
things a browser tab can't give you.

- **Immersive sticky fullscreen**, so the viewport never changes size and your
  calibration stays valid.
- **Screen stays on** while the app is open.
- **Portrait locked**, back button needs a double-press.
- **Edge gesture exclusion** on the bottom 200 dp of each side, so a press on the outer
  key columns isn't read as a Back swipe.
- **Fully offline.** The page is bundled in `app/src/main/assets/`; note images go to
  IndexedDB on the device and calibration to localStorage.

## Getting the APK without installing anything

You don't need Android Studio or an SDK. GitHub's build machines already have both.

1. Create a new **private** repository on github.com — any name.
2. Upload the contents of this folder to it. On the web UI: *Add file → Upload files*,
   drag everything in, commit to `main`. (Or `git push` if you'd rather.)
3. Go to the **Actions** tab. The build starts on its own; it takes about three minutes.
4. When it's green, open **Releases** in the right-hand sidebar. There's an
   `app-debug.apk` attached.
5. **On the phone**, open that release page and tap the `.apk`. Android will ask once
   whether to allow installs from your browser — allow it, then install.

Every later push rebuilds and publishes a new release, so updating means replacing
`app/src/main/assets/index.html` and committing.

## Building it locally instead

With Android Studio installed, open this folder and press Run. From a terminal with the
SDK on `ANDROID_HOME`:

```
./gradlew assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
```

## Updating the bundled page

`app/src/main/assets/index.html` is generated, not hand-written — the Artifact host adds
a `<!doctype>`, `<head>` and a small CSS reset at publish time, and the WebView needs
those baked in. Regenerate it from the artifact source:

```
./tools/build-asset.sh ../case-rig.html app/src/main/assets/index.html
```

## Why the page is served from https://calcase.local/

`MainActivity` intercepts that host and serves the bytes out of `assets/`. Loading
`file:///android_asset/index.html` directly would work, but a `file://` page gets an
opaque origin in WebView, where `localStorage` is unreliable and IndexedDB is refused —
and the rig keeps calibration in the first and note images in the second. A real https
origin makes both behave exactly as they do in a browser. Nothing leaves the device;
the host doesn't resolve and is never contacted.

## Signing

The debug key is used deliberately, so the APK installs straight off a GitHub Release
with no keystore to manage. It's fine for a personal tool. If you ever want a release
key, generate a keystore, add it as repository secrets, and point `signingConfigs` at it.
