package com.lumacam.feature.ai.benchmark

/**
 * The analysis backend the benchmark recommends for this device (PRD §4). This is a
 * lightweight signal produced by the benchmark and persisted for reuse; the Smart
 * mode engine (a later step) reads it as one input among several — e.g. a
 * [DeviceTier.BRUTAL_TRUTH] device biases Smart mode away from [LOCAL_AI] unless the
 * user has manually pinned that mode.
 */
enum class RecommendedMode(val displayName: String) {
    LUMA_VISION("Luma Vision (offline)"),
    LOCAL_AI("Local AI Model"),
    CLOUD_AI("Cloud AI")
}
