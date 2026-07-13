# Make Cloud AI & Local AI actually usable

## Context
The "Smart" top-bar chip is now a working mode selector (Smart / Luma Vision / Cloud AI / Local AI / Off).
Cloud AI is fully functional (real `GeminiProvider`/`OpenAiCompatProvider` HTTP + JSON) and works once a
real API key is added in Settings. Local AI's provider orchestration (`DefaultLocalAiProvider`) is complete,
but its native inference engine is `PlaceholderLocalInferenceEngine` (throws `RUNTIME_UNAVAILABLE`).

Both Cloud AI and Local AI menu items were being greyed out (`CameraScreen.kt:737-743`) because they were
disabled unless prerequisites existed (a stored key / a downloaded+selected model). With no setup yet, both
looked broken. User wants both selectable and functional.

Approved approach for Local AI: integrate **MediaPipe LLM Inference** (`com.google.mediapipe:llm.inference`)
as the real on-device engine (no NDK/CMake needed — native libs ship in the AAR). Can only be verified on a
physical device; CI stays JVM-green.

## Phase 1 — Remove grey-out, make both selectable + guided (verifiable here)
1. `app/src/main/java/com/lumacam/app/ui/camera/CameraScreen.kt` `AiModeIndicator` (~line 735):
   - Stop disabling Cloud AI / Local AI menu items. Items are always `enabled = true`.
   - Keep the informational suffix `(no key)` / `(no model)` (driven by `cloudAvailable`/`localAvailable`),
     shown in white, not greying the row.
   - Keep the `Check` icon on the current selection.
2. Guidance already works: when a mode is picked without its prerequisite, `AiHudViewModel` returns a typed
   `Failure` whose `primaryGuidance` is rendered by the HUD (`GuidanceCaption`, ~line 530):
   - Cloud AI, no key → `CloudAiError.NotConfigured`: "Cloud AI isn't fully set up yet. Add your provider,
     key, and model in Settings."
   - Local AI, no model → `LocalAiError.NoModelSelected`: "No on-device model selected. Download and select
     one in Settings to use Local AI."
   - Local AI, model present but runtime missing → `LocalAiError.RuntimeUnavailable`: "On-device inference
     isn't available in this build yet. Use Cloud AI or Luma Vision."
   No change needed to messages. `onSettings` → `Routes.SETTINGS` already wired for the deep link.
3. Cloud AI becomes usable end-to-end the moment a key is added (real provider + routing already done).
4. Verify: existing `AiHudViewModelTest` stays green; rely on CI lint/unit/build. (Optional Compose UI test
   asserting both items enabled deferred to avoid adding UI-test deps.)

## Phase 2 — Real on-device Local AI via MediaPipe LLM Inference (user-verified on device)
1. `app/build.gradle.kts`: add `com.google.mediapipe:llm.inference:<pinned>` (+ `:llm.inference-gpu` for GPU
   delegate). No NDK/CMake in our build. Bump `minSdk` only if the pinned version requires >24 (flag if so).
2. New `feature/ai/.../local/MediaPipeLocalInferenceEngine.kt` implementing `LocalInferenceEngine`:
   - `load(modelPath)` → `LlmInference.createFromOptions(ctx, LlmInferenceOptions.builder().setModelPath(modelPath)...)`;
     cache the instance + a session.
   - `analyze(image: LocalImage, prompt)` → decode `image.bytes` → `Bitmap`; build a multimodal query
     (prompt + image) per the pinned API; run `session.generateResponse(...)`; return raw text.
   - `close()` → close session + inference; map `MediaPipeException`/OOM → `LocalInferenceException`.
3. `app/src/main/java/com/lumacam/app/di/AppModule.kt:111`: swap `PlaceholderLocalInferenceEngine()` for
   `MediaPipeLocalInferenceEngine(context)`. Keep `activeModel = { repository.activeModel() }`.
4. Unit tests keep using the fake engine (MediaPipe is production-only; AAR native won't run under Robolectric).
5. Model compatibility: `LocalModelCatalog.kt` URLs are SmolVLM GGUF. Verify MediaPipe can load that GGUF; if
   not, switch catalog entries to a MediaPipe-compatible model/conversion and keep `LocalModelDownloader`.

### Known risks (verify on-device; cannot run native inference in this env)
- Multimodal (image+text) support in the pinned MediaPipe version is required for scene analysis.
- GGUF vs `.task`/converted model format compatibility.
- minSdk may need to rise above 24.

## Files
- `app/src/main/java/com/lumacam/app/ui/camera/CameraScreen.kt` (Phase 1 gate removal)
- `app/src/main/java/com/lumacam/app/di/AppModule.kt` (Phase 2 engine binding)
- `feature/ai/src/main/java/com/lumacam/feature/ai/local/MediaPipeLocalInferenceEngine.kt` (new, Phase 2)
- `feature/ai/src/main/java/com/lumacam/feature/ai/local/LocalModelCatalog.kt` (Phase 2 model URL, if needed)
- `app/build.gradle.kts` (Phase 2 dependency)

## Verification
- CI: Lint + Unit tests + Build must stay green after each phase.
- User: install debug APK; add a Cloud AI key → Cloud AI results; download a model → Local AI results
  (Phase 2) on a physical device.
