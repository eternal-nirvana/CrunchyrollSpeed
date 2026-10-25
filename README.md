# CR Speed - Crunchyroll Video Playback Speed Controller

**CR Speed** is a dedicated Android app built with Kotlin that provides real-time, customizable video playback speed control for Crunchyroll's web player. It features an interactive floating pill UI, background foreground service controls, HTML5 fullscreen support, and complete one-tap shutdown.

---

## Features

- **Automated JavaScript Speed Enforcement**:
  - Automatically identifies HTML5 `<video>` elements using a `MutationObserver` (accommodating asynchronous player loading).
  - Enforces playback speed and re-applies automatically upon ad breaks, episode transitions, and site rate changes (`ratechange`, `play`, `loadedmetadata`, `timeupdate`).
  - Supports preset speeds (`1.0x`, `1.5x`, `1.75x`, `2.0x`, `2.25x`, `2.5x`) and custom speeds from `0.25x` to `4.0x` in `0.05` increments.

- **Floating Speed Pill UI**:
  - Compact, semi-transparent badge (~52x28dp) floating above the player showing the active speed.
  - Draggable anywhere on screen, remembering its coordinates across orientations and app restarts.
  - Tap to expand a quick-control panel with presets, fine-tuning buttons (`-` / `+` 0.05), custom entry, and collapse.
  - Retains visibility and interaction on top of fullscreen video playback.

- **Hide & Restore Gestures**:
  - **Hide Pill**: Temporarily removes the pill from view while maintaining active playback speed.
  - **Restore Hotspot**: Long-press the top-left screen corner (56x56dp hotspot) to restore the pill anytime.
  - **Notification Restore**: Tap "Show speed pill" in the persistent status bar notification.

- **Persistent Foreground Service**:
  - Low-priority, persistent notification showing active speed.
  - Quick action buttons directly from the notification shade: **"Show speed pill"** and **"Quit"**.

- **Clean Background Shutdown**:
  - Complete shutdown via the pill panel or notification: stops foreground service, cancels notifications, purges and destroys the WebView (`about:blank`), clears tasks (`finishAndRemoveTask()`), and terminates the process (`Process.killProcess()`) to guarantee zero residual CPU or battery consumption.

- **Restricted Secure Navigation**:
  - Whitelisted navigation strictly to Crunchyroll, auth providers, and video delivery CDNs. External links (social, ads) safely open in external browsers.

---

## Architecture & Code Structure

```
CR-Speed/
├── app/src/main/
│   ├── java/com/example/
│   │   ├── MainActivity.kt         # WebView hosting, fullscreen WebChromeClient, and pill touch handling
│   │   ├── SpeedViewModel.kt       # Reactive StateFlows, input validation, and preset speed logic
│   │   ├── SpeedPrefs.kt           # SharedPreferences manager for speeds, positions, and visibility
│   │   ├── SpeedService.kt         # Foreground Service & persistent notification handling
│   │   └── PlayerJs.kt             # JavaScript injection scripts with MutationObserver & event hooks
│   └── res/
│       ├── layout/activity_main.xml# Root FrameLayout with WebView, Fullscreen container & Speed Pill
│       ├── drawable/               # Custom themed drawables & adaptive app icons
│       └── values/                 # Strings and theme configurations
```

---

## Building & Installing the APK

### 1. Build via Gradle Command Line
To assemble the debug APK:
```bash
gradle :app:assembleDebug
```
The generated APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

To assemble the release APK:
```bash
gradle :app:assembleRelease
```

### 2. Install onto Device via ADB
Connect your Android phone with USB debugging enabled, then run:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3. Open in Android Studio
1. Launch Android Studio.
2. Select **File > Open...** and navigate to this project folder.
3. Allow Gradle to sync.
4. Click **Run (▶)** targeting your connected device or emulator (Android 8.0+ / API 26+).

---

## Desktop Version for Windows Laptops

A standalone desktop version is included under `/desktop`:
- **File**: `desktop/cr_speed.py`
- **Features**: Native Windows WebView player with the exact same floating speed pill, preset speeds, custom inputs, and keyboard shortcuts (`[` and `]` to step speed, `\` to reset to 1.0x).
- **Compiling to EXE**:
  Run `desktop\build_exe.bat` on any Windows PC or let GitHub Actions build it automatically.

---

## Publishing to GitHub Releases (APK + EXE)

### Option 1: Automatic Release via GitHub Actions (Recommended)
1. Push this repository to GitHub.
2. Go to the **Releases** section on your repository (visible in your screenshot) and click **"Create a new release"**.
3. Choose or type a tag name like `v1.0.0`, give it a title (e.g. `CR Speed v1.0.0`), and click **"Publish release"**.
4. The included GitHub Actions workflow (`.github/workflows/release.yml`) will automatically trigger:
   - Builds `CR-Speed.apk` for Android on Ubuntu.
   - Builds `CR-Speed.exe` for Windows Laptops on Windows.
   - Automatically attaches both `CR-Speed.apk` and `CR-Speed.exe` to that release!

### Option 2: Manual Direct Upload to GitHub Releases
1. On your GitHub repository page (from your screenshot), click **"Create a new release"**.
2. Type tag: `v1.0.0` and title: `CR Speed Initial Release`.
3. Scroll down to the box: **"Attach binaries by dropping them here or selecting them"**.
4. Drag and drop:
   - `CR-Speed.apk` (located at root of project / `app/build/outputs/apk/debug/app-debug.apk`)
   - `CR-Speed.exe` (from the desktop build or GitHub Action)
5. Click **"Publish release"**. Both binaries will now be permanently available for download in the Releases section.

---

## Exporting & Pushing to GitHub

In Google AI Studio Build:
1. Click the **Export / Settings** menu in the top right header.
2. Select **Push to GitHub** or **Export to GitHub**.
3. Authenticate with your GitHub account, choose your repository name, and submit.
4. Alternatively, select **Download ZIP** to download the complete codebase locally, initialize Git, and push with:
   ```bash
   git init
   git add .
   git commit -m "CR Speed: Android APK & Windows Laptop EXE with GitHub Release workflow"
   git branch -M main
   git remote add origin https://github.com/<your-username>/<repo-name>.git
   git push -u origin main
   ```

---

## Testing Checklist

- [x] **Preset Speeds**: Tap presets `1.5x`, `1.75x`, `2.0x`, `2.25x`, `2.5x` and verify immediate video rate change.
- [x] **Custom Speeds**: Test values between `0.25x` and `4.0x` via manual entry or step buttons (`+`/`-`).
- [x] **Pill Drag & Memory**: Drag pill to new coordinates, rotate device, and verify position persists.
- [x] **Hide/Show Pill**: Hide pill via panel; restore via top-left corner long-press or notification action.
- [x] **Fullscreen Playback**: Enter fullscreen video; confirm video rotates to landscape and pill floats on top.
- [x] **Clean Termination**: Tap "Quit" in panel or notification; verify notification is removed and app process is terminated.

---

## Known Limitations

- **DRM Video Restrictions**: Certain high-definition streams protected by Widevine DRM may limit hardware decoder clocks above `2.0x`.
- **Site Layout Changes**: The injection relies on standard HTML5 `<video>` tags. If Crunchyroll updates player wrappers or nests players in isolated cross-origin frames, script selectors may need updating.
