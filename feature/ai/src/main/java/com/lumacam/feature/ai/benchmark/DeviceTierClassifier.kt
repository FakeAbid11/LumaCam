package com.lumacam.feature.ai.benchmark

/**
 * Classifies [DeviceCapabilities] into a [DeviceTier] and derives a
 * [RecommendedMode] (PRD — Device AI Compatibility Benchmark).
 *
 * The thresholds below are intentionally centralised as named, documented `const`s
 * so they can be tuned later without touching logic. RAM is the primary gate for
 * on-device model feasibility; CPU cores, Vulkan and API level refine the top tiers.
 * A device must satisfy ALL of a tier's requirements to reach it; classification
 * walks from best to worst and returns the first tier fully satisfied.
 *
 * This is pure logic with no Android dependency, so it is fully JVM-unit-testable.
 */
object DeviceTierClassifier {

    // --- EXCELLENT thresholds (tunable) ---
    const val EXCELLENT_MIN_RAM_MB = 8 * 1024
    const val EXCELLENT_MIN_CORES = 8
    const val EXCELLENT_MIN_API = 29

    // --- GOOD thresholds (tunable) ---
    const val GOOD_MIN_RAM_MB = 6 * 1024
    const val GOOD_MIN_CORES = 6
    const val GOOD_MIN_API = 26

    // --- LIMITED thresholds (tunable). Below this floor = BRUTAL_TRUTH. ---
    const val LIMITED_MIN_RAM_MB = 4 * 1024
    const val LIMITED_MIN_API = 24

    fun classify(caps: DeviceCapabilities): DeviceTier = when {
        caps.totalRamMb >= EXCELLENT_MIN_RAM_MB &&
            caps.cpuCores >= EXCELLENT_MIN_CORES &&
            caps.apiLevel >= EXCELLENT_MIN_API &&
            caps.supportsVulkan -> DeviceTier.EXCELLENT

        caps.totalRamMb >= GOOD_MIN_RAM_MB &&
            caps.cpuCores >= GOOD_MIN_CORES &&
            caps.apiLevel >= GOOD_MIN_API -> DeviceTier.GOOD

        caps.totalRamMb >= LIMITED_MIN_RAM_MB &&
            caps.apiLevel >= LIMITED_MIN_API -> DeviceTier.LIMITED

        else -> DeviceTier.BRUTAL_TRUTH
    }

    /**
     * Recommends a default analysis mode for [tier]. On-device ([RecommendedMode.LOCAL_AI])
     * is only recommended when the device is capable AND a model is already downloaded;
     * otherwise the always-available offline [RecommendedMode.LUMA_VISION] is preferred.
     * A [DeviceTier.BRUTAL_TRUTH] device is never steered toward Local AI here.
     */
    fun recommendedMode(tier: DeviceTier, hasDownloadedModel: Boolean): RecommendedMode = when (tier) {
        DeviceTier.EXCELLENT, DeviceTier.GOOD ->
            if (hasDownloadedModel) RecommendedMode.LOCAL_AI else RecommendedMode.LUMA_VISION
        DeviceTier.LIMITED -> RecommendedMode.LUMA_VISION
        DeviceTier.BRUTAL_TRUTH -> RecommendedMode.LUMA_VISION
    }
}
