package com.lumacam.app.data

/**
 * User-selected AI analysis backend, shown in the camera top-bar "Smart" chip.
 *
 * - [SMART]: automatic — picks the best available backend at analysis time.
 * - [LUMA_VISION]: on-device ML Kit pipeline (offline, always available).
 * - [CLOUD_AI]: a configured Cloud AI provider (requires an API key).
 * - [LOCAL_AI]: an on-device downloaded model (requires a selected + downloaded model).
 * - [OFF]: AI guidance disabled entirely.
 */
enum class AiMode(val displayName: String) {
    SMART("Smart"),
    LUMA_VISION("Luma Vision"),
    CLOUD_AI("Cloud AI"),
    LOCAL_AI("Local AI"),
    OFF("Off")
}
