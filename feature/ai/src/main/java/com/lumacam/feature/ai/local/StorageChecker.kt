package com.lumacam.feature.ai.local

/**
 * Pure storage pre-flight math (PRD §4 Tier 3 — "storage-space check before
 * downloading"). Kept free of Android APIs so it is JVM-unit-testable; callers pass
 * in the free-space figure obtained from the platform.
 */
object StorageChecker {

    /**
     * Extra headroom required on top of the model size, so a download never fills
     * the device to the brim (which can wedge the OS and other apps).
     */
    const val HEADROOM_BYTES: Long = 200L * 1024 * 1024

    /** Outcome of a storage check. */
    data class Result(
        val hasEnoughSpace: Boolean,
        val requiredBytes: Long,
        val availableBytes: Long
    ) {
        /** How many more bytes are needed; 0 when there's already enough. */
        val shortfallBytes: Long get() = (requiredBytes - availableBytes).coerceAtLeast(0L)
    }

    /**
     * Checks whether [availableBytes] can hold a download of [modelSizeBytes] plus
     * [headroomBytes] of safety margin.
     */
    fun check(
        modelSizeBytes: Long,
        availableBytes: Long,
        headroomBytes: Long = HEADROOM_BYTES
    ): Result {
        val required = modelSizeBytes + headroomBytes
        return Result(
            hasEnoughSpace = availableBytes >= required,
            requiredBytes = required,
            availableBytes = availableBytes
        )
    }
}
