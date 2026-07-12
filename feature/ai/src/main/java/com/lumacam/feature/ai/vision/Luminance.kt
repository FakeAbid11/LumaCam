package com.lumacam.feature.ai.vision

/**
 * Pure luminance helpers. The Android layer extracts a (downscaled) pixel array
 * from the camera frame; the math here is testable without a device.
 */
object Luminance {

    /**
     * Average perceived brightness of ARGB [pixels], returned as 0f..1f using the
     * Rec. 601 luma weights. An empty array returns 0f.
     */
    fun average(pixels: IntArray): Float {
        if (pixels.isEmpty()) return 0f
        var sum = 0.0
        for (p in pixels) {
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            sum += 0.299 * r + 0.587 * g + 0.114 * b
        }
        return (sum / pixels.size / 255.0).toFloat()
    }
}
