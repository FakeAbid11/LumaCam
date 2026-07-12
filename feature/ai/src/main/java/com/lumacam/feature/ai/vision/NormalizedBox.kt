package com.lumacam.feature.ai.vision

/**
 * An axis-aligned rectangle in normalized image coordinates, where (0f, 0f) is
 * the top-left of the frame and (1f, 1f) is the bottom-right. Pure data — no
 * Android dependency — so the scoring logic that consumes it is JVM-unit-testable.
 */
data class NormalizedBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    val area: Float get() = width * height

    companion object {
        /**
         * Builds a normalized box from pixel coordinates, clamping to [0,1].
         * A non-positive [imageWidth]/[imageHeight] yields a zero box.
         */
        fun fromPixels(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            imageWidth: Int,
            imageHeight: Int
        ): NormalizedBox {
            if (imageWidth <= 0 || imageHeight <= 0) return NormalizedBox(0f, 0f, 0f, 0f)
            val w = imageWidth.toFloat()
            val h = imageHeight.toFloat()
            return NormalizedBox(
                left = (left / w).coerceIn(0f, 1f),
                top = (top / h).coerceIn(0f, 1f),
                right = (right / w).coerceIn(0f, 1f),
                bottom = (bottom / h).coerceIn(0f, 1f)
            )
        }
    }
}
