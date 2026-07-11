# LumaCam — Product Requirements Document

**Status:** Draft v1.0 · **Owner:** Mobile Engineering · **Last updated:** 2026-07-11

> **⚠️ Hard engineering constraint (applies to the entire doc):** The developer has **no local Android build/run environment**. There is no local Gradle, no local emulator, no local device. **Every build, test, and run step happens in GitHub Actions CI**, and the *only* way to manually run the app is to **download the APK artifact from a CI run and install it on a physical phone**. No instruction anywhere below may assume a local build, local emulator, or local device test.

---

## 1. Product Overview & Vision

**One-sentence pitch:** LumaCam is an AI-assisted camera app that helps people take better photos by detecting scenes, subjects, and conditions in real time and applying smart, on-device and cloud-powered enhancements.

**Target users:**
- Casual smartphone photographers who want "better photos without effort."
- Users on **low/mid-end Android devices (Android 7.0 / API 24 and up)** who can't run heavy GPU-dependent camera apps.
- Privacy-conscious users who prefer on-device processing by default, with cloud as an opt-in upgrade.

**North-star metric:** % of captures where at least one AI suggestion/enhancement was accepted.

---

## 2. Feature List (by priority)

Priority legend: **P0** = must ship in v1 · **P1** = strong v1 candidate / fast-follow · **P2** = post-v1.

### P0 — Core
1. **Camera capture (photo)** — full-resolution still capture via CameraX `ImageCapture`.
2. **Live preview with AI overlay** — real-time labels/scene hints drawn over the CameraX `PreviewView`.
3. **Smart auto mode (heuristics)** — rule-based exposure/scene presets (no ML).
4. **Luma Vision (on-device ML)** — object/label detection + face detection via ML Kit.
5. **Gallery / capture review** — view, zoom, delete, share captured media.
6. **Settings** — toggle AI tiers, manage Gemini API key, privacy mode.

### P1 — Enhanced
7. **Local AI Model (TFLite)** — offline style transfer / background segmentation.
8. **Cloud AI captions (Gemini)** — per-photo auto caption + smart framing suggestion.
9. **Auto-enhance** — one-tap tonal/color correction from detected scene.
10. **HDR-ish merge** — bracketed capture + lightweight fusion (on-device).

### P2 — Future (see §11)
11. Video capture with live AI effects. 12. Cloud backup/sync. 13. Editing suite. 14. Share-to-social with AI hashtags.

---

## 3. Screens & Layouts

All screens are **single-Activity, Compose (`androidx.compose`)** driven by a `NavHost`. Each screen is a `@Composable` backed by a `ViewModel` (MVVM, §7).

| # | Screen | Layout description |
|---|--------|--------------------|
| S1 | **Splash / Permission** | Full-screen brand mark; sequential runtime permission prompts (camera, storage). Blocks entry until camera granted. |
| S2 | **Camera (home)** | `AndroidView` hosting `PreviewView` as full-bleed background. Top bar: AI-tier indicator + flash toggle. Bottom bar: shutter (center), gallery thumbnail (left), mode switch (right). Floating AI hint chip overlaying preview. |
| S3 | **AI Overlay/HUD** | Non-interactive `Canvas`/`Box` layer above preview drawing bounding boxes + labels from `ImageAnalysis`. |
| S4 | **Review** | Full-screen image with pinch-zoom (`Modifier.graphicsLayer`/horizontal pager for burst). Bottom actions: delete, share, enhance, back-to-camera. |
| S5 | **Gallery** | `LazyVerticalGrid` (2–4 cols by density) of thumbnails from MediaStore; tap → Review. |
| S6 | **Settings** | `LazyColumn` of groups: *AI tiers* (Smart/Luma Vision/Local/Cloud toggles), *Gemini API key* (secured field), *Privacy mode* (force offline), *About*. |
| S7 | **Cloud Result Sheet** | Modal bottom sheet showing Gemini caption + framing suggestion with "Apply" / "Dismiss". |

---

## 4. AI Architecture

Four layered tiers. The app **progressively escalates** capability based on the task, device capability, network, and user privacy settings. All tiers expose a common interface `AiEngine { suspend fun analyze(frame): AiResult }`.

| Tier | Name | Tech | Runs | Needs |
|------|------|------|------|-------|
| 1 | **Smart** | Rule-based heuristics (histogram, brightness, rule presets) | On-device, CPU | Nothing |
| 2 | **Luma Vision** | Google **ML Kit** (Label/Object/Face detection) bundled on-device | On-device, CPU/NNAPI | Nothing (offline) |
| 3 | **Local AI Model** | **TensorFlow Lite** bundled model (style transfer / segmentation) | On-device, CPU/GPU delegate | Model file in `assets/` |
| 4 | **Cloud AI** | **Google Gemini** (vision-capable model) via REST/SDK | Remote | Network + API key |

### Fallback logic
```
request(task):
  if PrivacyMode -> only tiers 1-3
  run Smart (always, instant)            -> baseline result
  if task needs labels/faces and Luma Vision enabled:
        result = LumaVision.analyze(); prefer over Smart
  if task needs style/segmentation and Local model loaded & device OK:
        result = LocalModel.analyze(); prefer if confidence higher
  if Cloud enabled AND network AND key present AND (low confidence OR task needs caption):
        result = Gemini.analyze()        -> highest authority
  return best available result (never block UI; show tier badge)
```
- **Degradation:** if a tier throws (e.g., Gemini 429/timeout), fall back to the previous tier's last good result; log to `AiFallbackMonitor`.
- **Privacy Mode** hard-disables tier 4; UI hides Cloud Result Sheet.

---

## 5. Camera Architecture (CameraX)

- Use **`androidx.camera.*`** (`core`, `lifecycle`, `view`, `extensions`).
- Single `ProcessCameraProvider` bound in a `CameraViewModel` to the `LifecycleOwner`.
- **Use cases:**
  - `Preview` → bound to `PreviewView` (Compose `AndroidView`).
  - `ImageCapture` → still photos; `takePicture(executor, callback)`; save to MediaStore.
  - `ImageAnalysis` (STRATEGY_KEEP_ONLY_LATEST, target ~640px) → feeds AI tiers at throttled rate (e.g., 4–8 fps) off the main thread.
  - `VideoCapture` (P2) → later.
- Bind/unbind on configuration changes; respect `CameraSelector` (back/front).
- All capture work on `Dispatchers.IO`/`CameraX` executors; **never** on main thread.

---

## 6. Folder Structure & Module Boundaries

Pragmatic **multi-module Gradle** project using a version catalog (`gradle/libs.versions.toml`). No local build needed; CI builds it.

```
LumaCam/
├── .github/workflows/        # ci.yml, release.yml
├── gradle/                   # libs.versions.toml, wrapper
├── app/                      # :app — single Activity, NavHost, DI root, signing
├── core/common/              # :core:common — coroutines, extensions, result types
├── core/ui/                  # :core:ui — theme, components, Previews
├── core/camera/              # :core:camera — CameraX wrapper, use-case builders
├── feature/ai/               # :feature:ai — AiEngine, tiers, fallback, Gemini client
├── feature/gallery/          # :feature:gallery — MediaStore repo, Review UI
└── build.gradle.kts (root)   # plugins, Kotlin/Compose compiler options
```
**Boundaries:** `feature/*` depends on `core/*`; `core` never depends on `feature`; `app` wires everything with Hilt. AI tier interface lives in `:feature:ai` and is injected into `:core:camera` via abstraction (no concrete cloud deps leak into camera module).

---

## 7. Coding Standards

- **Language:** 100% Kotlin; **no** XML layouts (Compose only, except `AndroidManifest`).
- **UI:** Jetpack Compose; state via `StateFlow`/`Compose State`; unidirectional data flow.
- **Architecture:** MVVM — `ViewModel` exposes `StateFlow<UiState>`; Composables are stateless where possible; repository pattern for data.
- **DI:** Hilt. **Concurrency:** Coroutines + `Flow`; `viewModelScope`/`lifecycleScope`.
- **Naming:** `PascalCase` classes, `camelCase` funcs, `SCREAMING_SNAKE` constants; Compose components suffixed `Screen`/`Item`/`Sheet`.
- **Style:** ktlint + Spotless in CI; 4-space indent; explicit `@Composable`/`suspend` markers; no magic numbers (use `dimens`/theme).
- **No local verification possible** → enforce everything via CI lint/ktlint job (§8).

---

## 8. GitHub Actions CI/CD Strategy

All jobs run on `ubuntu-latest`. Use **JDK 17** (`actions/setup-java`), Gradle cache (`gradle/gradle-build-action`), `actions/checkout@v4`.

**Triggers (`ci.yml`):**
- `push` to `main` and `develop`.
- `pull_request` to `main` / `develop`.

**Jobs:**

1. **`lint`** — `./gradlew lintDebug` + ktlint/Spotless check. Fails build on violations.
2. **`unit-test`** — `./gradlew testDebugUnitTest` (JVM-only, Robolectric for ViewModel/Compose-VM logic). **No device.**
3. **`build`** — `./gradlew assembleDebug assembleRelease`. Release signed via **GitHub Secrets** (`KEYSTORE_BASE64`, `KEY_ALIAS`, `KEY_PASSWORD`, `STORE_PASSWORD`) decoded in workflow; never committed.
4. **`instrumented-test`** (optional, matrix on `API 30`) — `ReactiveCircus/android-emulator-runner@v2` boots an AVD, runs `./gradlew connectedDebugAndroidTest`. Gated behind a workflow input / label to save minutes.
5. **`upload-artifact`** — `actions/upload-artifact@v4` uploads `app/build/outputs/apk/debug/*.apk` (+ release if built) with `retention-days: 14`. **This is the only path to run the app.**

**`release.yml`:** on tag `v*`, builds signed release AAB/APK, uploads artifact + (optional) Play Console internal track via secret.

---

## 9. Testing Strategy

### A. CI-only, no device (runs in `unit-test` job)
- ViewModel logic, AI fallback state machine, repository mapping, settings validation — pure JUnit + Turbine.
- Compose UI logic via **Robolectric** (`@RunWith(RobolectricTestRunner)` + `ComposeTestRule`) for state assertions without a device.
- Gemini client serialization/mock via `MockK` + `kotlinx.coroutines.test`.
- **Verify:** green `testDebugUnitTest` in CI log.

### B. Device/emulator-only (runs in `instrumented-test` job via emulator action — NEVER locally)
- CameraX `Preview`/`ImageCapture` bind/unbind, MediaStore write, ML Kit label detection on a real frame, TFLite model load/inference timing.
- `androidx.test` + `compose-ui-test` (`androidTest`).
- **Verify:** green `connectedDebugAndroidTest` on the CI-emulated AVD (API 30).

### C. Manual testing (ONLY on downloaded APK, on a physical phone)
- Install the `app-debug.apk` artifact from the workflow run on your own Android phone.
- Exercise: permissions flow, capture, AI overlay badges, Gallery, Settings toggles, Gemini caption (with key), Privacy Mode (cloud hidden).
- **No emulator/simulator locally** — manual QA is strictly APK-on-phone. File bugs with device model + Android version + screen recording.

---

## 10. Performance Goals & Constraints (low/mid-end, API 24+)

| Metric | Target (mid) | Target (low) |
|--------|--------------|--------------|
| Cold start → camera ready | < 2.0 s | < 3.0 s |
| Shutter-to-saved latency | < 500 ms | < 900 ms |
| AI overlay frame analysis | 4–8 fps | 2–4 fps (throttled) |
| Steady memory | < 150 MB | < 180 MB (watch for OOM on 1–2 GB devices) |
| TFLite model size | < 20 MB bundled | same |
| APK size | < 25 MB (Android App Bundle) | — |

**Constraints:**
- Use `ImageAnalysis` STRATEGY_KEEP_ONLY_LATEST; drop frames rather than queue.
- Default to CPU/NNAPI delegate for TFLite; GPU delegate only if probed successfully.
- Cap concurrent AI tiers; Smart always instant, Cloud only on explicit/user-action.
- Avoid `Bitmap` copies on hot path; reuse buffers; profile via CI `benchmark` (P2) on emulator.

---

## 11. Future Roadmap (post-v1)

- **P2 video** with live AI effects + background segmentation in real time.
- **Cloud sync/backup** of captures + captions (Gemini) to user account.
- **On-device LLM** (small Gemma/TFLite) to remove cloud dependency for captions.
- **Editing suite:** AI object erase, relight, super-resolution.
- **Accessibility:** spoken scene descriptions for low-vision users.
- **CI benchmarking** job on emulated low-end AVD to guard perf regressions.
- **Wear OS / foldable** preview adaptations.

---

### Appendix — "How to verify this works" (every path is CI/APK)
- Feature compiles → `build` job (CI).
- Logic correct → `unit-test` job (CI, Robolectric).
- Camera/ML correct → `instrumented-test` job (CI emulator).
- Looks/feels right → download **APK artifact**, install on **your phone**, manual test (§9C).
- Style compliant → `lint` job (CI).
