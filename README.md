# CR Speed - Crunchyroll Video Playback Speed Controller

[![License: MIT](https://img.shields.io/github/license/eternal-nirvana/CrunchyrollSpeed)](https://github.com/eternal-nirvana/CrunchyrollSpeed/blob/main/LICENSE)
[![Latest release](https://img.shields.io/github/v/release/eternal-nirvana/CrunchyrollSpeed)](https://github.com/eternal-nirvana/CrunchyrollSpeed/releases/latest)

**A lightweight playback-speed controller for Crunchyroll, for Android and Windows.**

CR Speed opens Crunchyroll's web player and adds a small floating speed pill on top of it. Switch between presets like 1.5×, 2× or 2.5×, or dial in any custom speed from 0.25× to 4.0×. When you're done, one tap shuts everything down.

> **Disclaimer:** CR Speed is an unofficial, independent project. It is not affiliated with, endorsed by, or connected to Crunchyroll. You need your own Crunchyroll account to watch content.

---

## Download

Get the latest APK (Android) and EXE (Windows) from the [**Releases page**](https://github.com/eternal-nirvana/CrunchyrollSpeed/releases/latest).

**Android:** download `CR-Speed.apk`, open it, and allow "Install from unknown sources" if your phone asks. Requires Android 8.0 (API 26) or newer.

**Windows:** download `CR-Speed.exe` and run it.

---

## Features

- **Automatic speed enforcement**
  - Detects the HTML5 `<video>` player, even when it loads late.
  - Re-applies your speed after ad breaks, episode changes, and site-side rate resets.
  - Presets: `1.0x`, `1.5x`, `1.75x`, `2.0x`, `2.25x`, `2.5x`.
  - Custom speeds from `0.25x` to `4.0x` in `0.05` steps.
- **Floating speed pill**
  - Small, semi-transparent badge showing the current speed.
  - Draggable anywhere; remembers its position across rotations and restarts.
  - Tap it to open a panel with presets, `-` / `+` fine-tuning, and custom entry.
  - Stays visible on top of fullscreen video.
- **Hide and restore**
  - **Hide** the pill while keeping your chosen speed active.
  - **Restore** by long-pressing the top-left corner of the screen, or by tapping "Show speed pill" in the notification.
- **Foreground service notification** (Android)
  - Shows the active speed, with **Show speed pill** and **Quit** actions.
- **Clean shutdown**
  - Quit from the panel or the notification to stop the service, clear the notification, destroy the WebView, and end the process, so nothing keeps using CPU or battery.
- **Restricted navigation**
  - Only Crunchyroll, its login providers, and video CDNs load inside the app. Other links open in your external browser.

---

## How to use

1. Open the app and sign in to Crunchyroll.
2. Start any episode.
3. Tap the speed pill and pick a preset, or use `-` / `+` or custom entry.
4. Tap **Hide** if the pill is in the way. Long-press the top-left corner to bring it back.
5. Tap **Quit** when finished.

### Windows keyboard shortcuts

| Key | Action |
|-----|--------|
| `]` | Increase speed |
| `[` | Decrease speed |
| `\` | Reset to 1.0x |

---

## Build from source

### Android

```bash
git clone https://github.com/eternal-nirvana/CrunchyrollSpeed.git
cd CrunchyrollSpeed

# Debug APK
./gradlew :app:assembleDebug

# Release APK
./gradlew :app:assembleRelease
```

The debug APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

Install it on a connected phone (USB debugging on):

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or open the project in **Android Studio**, let Gradle sync, and press **Run**.

### Windows

The desktop version lives in [`desktop/`](desktop/) (`cr_speed.py`). To build the EXE on a Windows PC, run:

```bat
desktop\build_exe.bat
```

---

## Project structure

```
CrunchyrollSpeed/
├── app/src/main/
│   ├── java/com/example/
│   │   ├── MainActivity.kt    # WebView host, fullscreen handling, pill touch events
│   │   ├── SpeedViewModel.kt  # State, input validation, preset logic
│   │   ├── SpeedPrefs.kt      # Saved speed, pill position, visibility
│   │   ├── SpeedService.kt    # Foreground service and notification
│   │   └── PlayerJs.kt        # Injected JavaScript (MutationObserver + event hooks)
│   └── res/                   # Layouts, drawables, strings, themes
├── desktop/                   # Windows version
├── .github/workflows/         # Automated release builds
├── LICENSE                    # MIT License
└── README.md
```

---

## Releasing (for maintainers)

Publishing a release triggers the GitHub Actions workflow in `.github/workflows/release.yml`, which builds both files and attaches them to the release.

1. Go to **Releases → Draft a new release**.
2. Create a tag such as `v1.0.0` and add a title.
3. Click **Publish release**.
4. The workflow builds `CR-Speed.apk` (Ubuntu) and `CR-Speed.exe` (Windows) and uploads them automatically.

---

## Testing checklist

- Presets (`1.5x` to `2.5x`) change the video speed immediately.
- Custom values between `0.25x` and `4.0x` work via entry and `-` / `+`.
- The pill keeps its position after dragging and rotating.
- Hide and restore work via the corner long-press and the notification.
- Fullscreen rotates to landscape and the pill stays on top.
- **Quit** removes the notification and ends the app process.

---

## Known limitations

- **DRM:** Some Widevine-protected streams may limit decoding above `2.0x`.
- **Site changes:** The script relies on a standard HTML5 `<video>` element. If Crunchyroll changes its player or moves it into an isolated cross-origin frame, the selectors may need updating.
- **Unofficial:** Crunchyroll can change its site at any time, which may temporarily break the app.

---

## Contributing

Issues and pull requests are welcome. If something breaks after a Crunchyroll update, please open an issue with your device or OS version and what you saw.

---

## License

This project is licensed under the [MIT License](https://github.com/eternal-nirvana/CrunchyrollSpeed/blob/main/LICENSE). See the `LICENSE` file for details.
