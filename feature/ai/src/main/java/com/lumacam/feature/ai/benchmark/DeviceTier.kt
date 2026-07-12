package com.lumacam.feature.ai.benchmark

/**
 * The four honest capability tiers (PRD — Device AI Compatibility Benchmark). The
 * messaging is deliberately direct about the *device's* capability, never about the
 * user ("Your phone is not suitable…", not "your phone is bad").
 *
 * @param displayName short label shown in the results screen.
 * @param headline one-line verdict.
 * @param message honest explanation and guidance.
 * @param estimatedTimeLabel rough per-analysis time range, always labeled as an
 *   *estimate* in the UI (a real measured time requires a downloaded model + a
 *   bundled native runtime, which is a later step).
 */
enum class DeviceTier(
    val displayName: String,
    val headline: String,
    val message: String,
    val estimatedTimeLabel: String
) {
    EXCELLENT(
        displayName = "Excellent",
        headline = "Your phone can comfortably run on-device AI.",
        message = "This device has the memory and horsepower to run local vision " +
            "models smoothly. Local AI Model is a great fit.",
        estimatedTimeLabel = "~1–3 s per analysis"
    ),
    GOOD(
        displayName = "Good",
        headline = "Your phone handles on-device AI well.",
        message = "This device can run local vision models at a reasonable speed. " +
            "Larger models will be slower but usable.",
        estimatedTimeLabel = "~3–8 s per analysis"
    ),
    LIMITED(
        displayName = "Limited",
        headline = "Your phone can run smaller models, with patience.",
        message = "This device can run the smallest local models, but analysis will " +
            "be slow and battery use higher. Luma Vision or Cloud AI will feel faster.",
        estimatedTimeLabel = "~8–20 s per analysis"
    ),
    BRUTAL_TRUTH(
        displayName = "Brutal Truth",
        headline = "Your phone is not suitable for running these models locally.",
        message = "This device doesn't have the memory or compute for on-device AI " +
            "models to run acceptably. Luma Vision (offline) or Cloud AI will work " +
            "far better here.",
        estimatedTimeLabel = "20 s+ per analysis (not recommended)"
    )
}
