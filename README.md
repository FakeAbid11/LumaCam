# LumaCam

AI-assisted camera app for low/mid-end Android devices (minSdk 24). See [`PRD.md`](PRD.md) for the full product requirements.

> **This project is built, tested, and run entirely in GitHub Actions.** There is no local build/run step. The only way to obtain and try the app is to download the APK artifact produced by a CI run and install it on a physical phone.

## Tech stack
- Kotlin + Jetpack Compose (Material 3, dark-mode aware)
- MVVM with Hilt dependency injection
- Compose Navigation
- Room (local data, for future capture records)
- DataStore (settings / preferences)
- Multi-module Gradle: `:app`, `:core:common`, `:core:ui`, `:core:camera`, `:feature:ai`, `:feature:gallery`

## How to build (CI only)

You never run Gradle locally. A workflow (`.github/workflows/ci.yml`) runs automatically on:
- **every push** to `main` / `develop`
- **every pull request** to `main` / `develop`

The workflow does, in order:
1. **Lint** — `./gradlew lintDebug`
2. **Unit tests** — `./gradlew testDebugUnitTest` (JVM only, no device)
3. **Build debug APK** — `./gradlew assembleDebug`
4. **Upload artifact** — the debug APK is uploaded as `lumaCam-debug-apk` (kept for 14 days)

## How to download and run the APK

1. Open the repository on GitHub and click the **Actions** tab.
2. Click the workflow run for your push or PR (top of the list).
3. Scroll to the **Artifacts** section and click **`lumaCam-debug-apk`** to download the zip.
4. Unzip it; you'll get `app-debug.apk`.
5. Transfer `app-debug.apk` to your Android phone and install it (allow "Install from unknown sources" if prompted).
6. Open **LumaCam**. It launches to a placeholder Camera screen with a placeholder Settings screen — proof the full toolchain works end-to-end.

Manual QA is always done on this downloaded APK, on a real device. There is no emulator or local device testing.

## Project layout

See `PRD.md` §6 for the module boundaries. The current code is an empty, launchable shell that wires up the foundation (theme, navigation, Hilt, Room, DataStore) so subsequent feature work lands in the correct modules.
