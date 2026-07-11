package com.lumacam.core.camera

/**
 * Snapshot of what a bound camera can actually do, probed from Camera2
 * CameraCharacteristics at bind time. Every field is nullable / boolean so the
 * UI can hide controls the current lens does not support (graceful degradation,
 * Prompt 3). Auto-only devices simply get a mostly-empty instance.
 */
data class CameraCapabilities(
    val isoRange: IntRange? = null,
    val exposureTimeRange: LongRange? = null,
    val minFocusDistance: Float? = null,
    val exposureCompensationRange: IntRange = 0..0,
    val exposureCompensationStep: Float = 0f,
    val supportedWhiteBalance: List<WhiteBalanceMode> = listOf(WhiteBalanceMode.AUTO),
    val supportsManualSensor: Boolean = false,
    val supportsManualFocus: Boolean = false,
    val supportsExposureLock: Boolean = false,
    val supportsExposureCompensation: Boolean = false,
    val hdrSupported: Boolean = false,
    val hasMultipleLenses: Boolean = false,
) {
    val supportsAnyManualControl: Boolean
        get() = supportsManualSensor || supportsManualFocus || supportsExposureLock ||
            supportsExposureCompensation || hdrSupported ||
            supportedWhiteBalance.size > 1 || hasMultipleLenses
}

/** Default target exposure time (~1/60s), clamped to the sensor's range. */
const val DEFAULT_EXPOSURE_TIME_NANOS = 16_666_666L

/** Default ISO used when engaging manual exposure without an explicit value. */
const val DEFAULT_ISO = 100

fun coerceIso(value: Int, range: IntRange?): Int =
    if (range == null) value else value.coerceIn(range.first, range.last)

fun coerceExposureTimeNanos(value: Long, range: LongRange?): Long =
    if (range == null) value else value.coerceIn(range.first, range.last)

fun coerceExposureCompensation(value: Int, range: IntRange): Int =
    value.coerceIn(range.first, range.last)

/** LENS_FOCUS_DISTANCE is in diopters: 0 = infinity, [minFocusDistance] = closest. */
fun coerceFocusDistance(value: Float, minFocusDistance: Float): Float =
    value.coerceIn(0f, if (minFocusDistance > 0f) minFocusDistance else 0f)

fun defaultExposureTimeNanos(range: LongRange?): Long =
    coerceExposureTimeNanos(DEFAULT_EXPOSURE_TIME_NANOS, range)

fun defaultIso(range: IntRange?): Int =
    coerceIso(DEFAULT_ISO, range)

/**
 * Classify back-camera lenses by focal length relative to the median (treated as
 * the "main" wide lens). Deterministic and unit-tested; nothing is hardcoded to
 * a device. A single lens is always [LensType.WIDE].
 */
fun classifyLenses(focalLengths: List<Float>): List<LensType> {
    if (focalLengths.isEmpty()) return emptyList()
    if (focalLengths.size == 1) return listOf(LensType.WIDE)
    val ref = medianOf(focalLengths)
    if (ref <= 0f) return focalLengths.map { LensType.WIDE }
    return focalLengths.map { focal ->
        val ratio = focal / ref
        when {
            ratio <= 0.7f -> LensType.ULTRA_WIDE
            ratio >= 1.5f -> LensType.TELEPHOTO
            else -> LensType.WIDE
        }
    }
}

fun medianOf(values: List<Float>): Float {
    if (values.isEmpty()) return 0f
    val sorted = values.sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2f
}
