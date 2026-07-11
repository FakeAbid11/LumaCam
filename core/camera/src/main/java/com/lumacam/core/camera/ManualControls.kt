package com.lumacam.core.camera

import android.hardware.camera2.CameraMetadata

/** White-balance presets mapped to Camera2 CONTROL_AWB_MODE constants. */
enum class WhiteBalanceMode(val label: String, val awbMode: Int) {
    AUTO("Auto", CameraMetadata.CONTROL_AWB_MODE_AUTO),
    DAYLIGHT("Daylight", CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT),
    CLOUDY("Cloudy", CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT),
    TUNGSTEN("Tungsten", CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT),
    FLUORESCENT("Fluorescent", CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT);

    companion object {
        fun fromAwbMode(awbMode: Int): WhiteBalanceMode? = entries.firstOrNull { it.awbMode == awbMode }
    }
}

/** Physical lens types classified from focal length (see [classifyLenses]). */
enum class LensType(val label: String) {
    ULTRA_WIDE("Ultra-wide"),
    WIDE("Wide"),
    TELEPHOTO("Tele")
}

/** A selectable back camera exposed by the device at runtime. */
data class LensInfo(
    val id: String,
    val type: LensType,
    val minFocalLength: Float
) {
    val label: String get() = type.label
}

/**
 * The user's current manual overrides. Nulls mean "auto" for that dimension, so
 * an all-default instance behaves exactly like the auto camera from Prompt 2.
 */
data class ManualCameraState(
    val isoValue: Int? = null,
    val exposureTimeNanos: Long? = null,
    val exposureCompensation: Int = 0,
    val whiteBalance: WhiteBalanceMode = WhiteBalanceMode.AUTO,
    val focusDistance: Float? = null,
    val focusLocked: Boolean = false,
    val exposureLocked: Boolean = false,
    val hdrEnabled: Boolean = false,
    val selectedLensId: String? = null
) {
    /** Manual exposure engaged when ISO or shutter is overridden (forces AE off). */
    val isManualExposure: Boolean get() = isoValue != null || exposureTimeNanos != null

    val isManualFocus: Boolean get() = focusDistance != null
}
