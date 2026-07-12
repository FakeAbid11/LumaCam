package com.lumacam.feature.ai.benchmark

import com.lumacam.feature.ai.local.LocalModelSpec
import com.lumacam.feature.ai.local.StorageChecker
import com.lumacam.feature.ai.local.formatBytes

/**
 * Per-model suitability verdict fed into the Model Manager (PRD — "show why, don't
 * just say no"). Unsuitable models are marked NOT_RECOMMENDED with an honest reason
 * (memory, estimated speed, battery, storage) rather than hidden or blocked — the
 * user can still choose to download them.
 */
sealed interface ModelRecommendation {
    data object Recommended : ModelRecommendation
    data class NotRecommended(val reason: String) : ModelRecommendation
}

/** Evaluates whether [spec] is a good fit for a device with [caps] at [tier]. */
object ModelSuitability {

    fun evaluate(
        caps: DeviceCapabilities,
        tier: DeviceTier,
        spec: LocalModelSpec,
        availableStorageBytes: Long = caps.availableStorageBytes
    ): ModelRecommendation {
        // Hard memory gate: not enough RAM to load the model at all.
        if (caps.totalRamMb < spec.minRamMb) {
            return ModelRecommendation.NotRecommended(
                "Needs about ${spec.formattedMinRam} of RAM; this phone has " +
                    "${formatBytes(caps.totalRamMb * 1024L * 1024L)}. Expect crashes or failures."
            )
        }

        // Storage gate: not enough free space (model size + safety headroom).
        val storage = StorageChecker.check(spec.sizeBytes, availableStorageBytes)
        if (!storage.hasEnoughSpace) {
            return ModelRecommendation.NotRecommended(
                "Needs about ${spec.formattedSize} free (plus headroom); free up storage first."
            )
        }

        // Capability gate: technically fits in memory, but the device is too slow.
        if (tier == DeviceTier.BRUTAL_TRUTH) {
            return ModelRecommendation.NotRecommended(
                "This phone can load it, but analysis will be very slow (${tier.estimatedTimeLabel}) " +
                    "and battery use high. Luma Vision or Cloud AI will work better."
            )
        }

        return ModelRecommendation.Recommended
    }
}
