package com.lumacam.feature.ai.vision

/**
 * Supplies the current device roll (horizon tilt) in degrees. Abstracted so the
 * analyzer can be fed a real accelerometer ([SensorTiltProvider]) on-device or a
 * fake value in unit tests.
 */
fun interface TiltProvider {
    fun currentTiltDegrees(): Float
}
