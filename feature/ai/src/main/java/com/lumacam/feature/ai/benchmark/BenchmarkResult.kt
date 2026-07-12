package com.lumacam.feature.ai.benchmark

/**
 * The outcome of running "Test My Phone" (PRD — Device AI Compatibility Benchmark):
 * the gathered [caps], the classified [tier], the derived [recommendedMode], and a
 * timestamp.
 *
 * [measuredMillis] is null when the analysis time is an *estimate* (the current
 * case: the benchmark reads specs only). When a local model is downloaded AND a real
 * native inference runtime is bundled (a later step), a single timed inference pass
 * can populate this with a *measured* value, which the UI then labels accordingly.
 *
 * @param elapsedMillis how long the spec-gathering itself took (near-instant).
 */
data class BenchmarkResult(
    val caps: DeviceCapabilities,
    val tier: DeviceTier,
    val recommendedMode: RecommendedMode,
    val measuredMillis: Long? = null,
    val elapsedMillis: Long = 0L,
    val timestamp: Long = 0L
) {
    /** True when the analysis-time figure is a real measurement, not an estimate. */
    val isMeasured: Boolean get() = measuredMillis != null
}
