package com.lumacam.core.common.film

/**
 * Pure 4x5 color-matrix math used to build [FilmPreset] color grades (LumaCam Film
 * Camera Engine). Layout matches `android.graphics.ColorMatrix` (row-major, 4 rows
 * of 5; column 4 is a constant offset on a 0..255 scale) so the same arrays drive
 * both the GL shader and the Compose thumbnail `ColorFilter`. No Android/GL
 * dependency, so it is fully JVM-unit-testable.
 */
object FilmColorMatrix {

    // Perceptual luminance weights (Rec. 709-ish) used for saturation.
    private const val LR = 0.213f
    private const val LG = 0.715f
    private const val LB = 0.072f

    /** A saturation matrix. `1f` = unchanged, `0f` = grayscale, `>1f` = boosted. */
    fun saturation(s: Float): FloatArray {
        val inv = 1f - s
        return floatArrayOf(
            inv * LR + s, inv * LG, inv * LB, 0f, 0f,
            inv * LR, inv * LG + s, inv * LB, 0f, 0f,
            inv * LR, inv * LG, inv * LB + s, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }

    /** A contrast matrix about mid-gray. `1f` = unchanged, `>1f` = punchier. */
    fun contrast(c: Float): FloatArray {
        val t = (0.5f - 0.5f * c) * 255f
        return floatArrayOf(
            c, 0f, 0f, 0f, t,
            0f, c, 0f, 0f, t,
            0f, 0f, c, 0f, t,
            0f, 0f, 0f, 1f, 0f
        )
    }

    /** Per-channel gain + offset (offset on 0..255 scale). */
    fun channelScale(
        rGain: Float = 1f,
        gGain: Float = 1f,
        bGain: Float = 1f,
        rOffset: Float = 0f,
        gOffset: Float = 0f,
        bOffset: Float = 0f
    ): FloatArray = floatArrayOf(
        rGain, 0f, 0f, 0f, rOffset,
        0f, gGain, 0f, 0f, gOffset,
        0f, 0f, bGain, 0f, bOffset,
        0f, 0f, 0f, 1f, 0f
    )

    /**
     * Composes two color matrices: returns a matrix equivalent to applying [b]
     * first, then [a] (i.e. `a ∘ b`), matching `ColorMatrix.setConcat(a, b)`.
     */
    fun concat(a: FloatArray, b: FloatArray): FloatArray {
        require(a.size == FilmPreset.COLOR_MATRIX_SIZE && b.size == FilmPreset.COLOR_MATRIX_SIZE) {
            "both matrices must be 4x5 (${FilmPreset.COLOR_MATRIX_SIZE} elements)"
        }
        val out = FloatArray(FilmPreset.COLOR_MATRIX_SIZE)
        for (i in 0 until 4) {
            for (j in 0 until 5) {
                var sum = 0f
                for (k in 0 until 4) {
                    sum += a[i * 5 + k] * b[k * 5 + j]
                }
                if (j == 4) sum += a[i * 5 + 4]
                out[i * 5 + j] = sum
            }
        }
        return out
    }

    /** Composes an ordered list of matrices, applying the first in the list first. */
    fun compose(vararg matrices: FloatArray): FloatArray {
        require(matrices.isNotEmpty()) { "compose requires at least one matrix" }
        // Apply left-to-right: result = last ∘ ... ∘ first.
        var acc = matrices[0]
        for (idx in 1 until matrices.size) {
            acc = concat(matrices[idx], acc)
        }
        return acc
    }
}
