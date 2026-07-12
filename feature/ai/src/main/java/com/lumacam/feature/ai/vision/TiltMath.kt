package com.lumacam.feature.ai.vision

import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.abs

/**
 * Pure device-tilt math. Converts an accelerometer gravity vector into a roll
 * angle (how far the horizon is tilted) and grades how level the frame is. No
 * Android dependency, so it is fully unit-testable.
 */
object TiltMath {

    /** Beyond this many degrees the frame is treated as maximally un-level. */
    const val MAX_TILT_DEGREES = 10f

    /**
     * Roll angle in degrees from the accelerometer gravity components.
     *
     * With the phone held upright in portrait, gravity points down the screen
     * (y negative in the Android sensor frame): [gx] = 0 → 0°. Rotating the
     * phone clockwise makes [gx] positive and returns a positive angle. The
     * result is in (-180, 180].
     */
    fun rollDegrees(gx: Float, gy: Float): Float {
        if (gx == 0f && gy == 0f) return 0f
        return Math.toDegrees(atan2(gx.toDouble(), -gy.toDouble())).toFloat()
    }

    /**
     * How level the frame is, 0..100, where 100 = perfectly level and 0 =
     * tilted by at least [MAX_TILT_DEGREES].
     */
    fun levelScore(tiltDegrees: Float): Int {
        val capped = min(abs(tiltDegrees), MAX_TILT_DEGREES)
        return ((1f - capped / MAX_TILT_DEGREES) * 100f).toInt()
    }
}
