package com.lumacam.core.common.film

/**
 * A film-simulation preset (LumaCam Film Camera Engine). Pure data with no Android
 * or GL dependency so it is fully JVM-unit-testable; the GL renderer in
 * `:core:camera` reads these normalized parameters as shader uniforms, and the
 * preset-picker UI reuses [colorMatrix] as an Android `ColorMatrix` for thumbnails.
 *
 * All intensity parameters are normalized to `0f..1f` (0 = off, 1 = full strength)
 * so the shader and the UI can treat them uniformly and mixing is predictable.
 *
 * The color grade is a 4x5 [colorMatrix] in row-major order — the same layout as
 * Android's `android.graphics.ColorMatrix` (`4 rows x 5 columns`: R,G,B,A output
 * each as a linear combination of R,G,B,A,1). This avoids shipping large 3D-LUT
 * image assets (PRD — "avoid requiring large LUT image assets if a matrix
 * approximation looks acceptably close"). The last column (index 4, 9, 14, 19) is a
 * constant offset expressed on a 0..255 scale, matching `ColorMatrix` conventions;
 * the GL layer divides it by 255.
 *
 * @param id stable identifier used for persistence/selection.
 * @param name human-readable label shown in the picker.
 * @param description one-line summary of the look.
 * @param colorMatrix 20-element (4x5) row-major color transform.
 * @param grainIntensity procedural film-grain strength.
 * @param halationIntensity warm glow bleeding from bright/red areas.
 * @param bloomIntensity general bright-area glow.
 * @param vignetteIntensity darkening toward the frame edges.
 * @param chromaticAberration per-channel edge color fringing (VHS/lo-fi look).
 * @param scanlineIntensity horizontal scanline darkening (VHS/CRT look).
 * @param softness focus softening / blur (MiniDV/VHS look).
 * @param temperature warm/cool cast, `0.5f` = neutral, `>0.5` warmer, `<0.5` cooler.
 */
data class FilmPreset(
    val id: String,
    val name: String,
    val description: String,
    val colorMatrix: FloatArray,
    val grainIntensity: Float,
    val halationIntensity: Float,
    val bloomIntensity: Float,
    val vignetteIntensity: Float,
    val chromaticAberration: Float,
    val scanlineIntensity: Float,
    val softness: Float,
    val temperature: Float
) {
    init {
        require(colorMatrix.size == COLOR_MATRIX_SIZE) {
            "colorMatrix must have $COLOR_MATRIX_SIZE elements (4x5), was ${colorMatrix.size}"
        }
    }

    /** True when this preset applies no visible change (identity passthrough). */
    val isIdentity: Boolean
        get() = id == ORIGINAL_ID

    // data class equals/hashCode don't handle FloatArray by value; override so
    // presets compare structurally (useful for tests and state de-duplication).
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FilmPreset) return false
        return id == other.id &&
            name == other.name &&
            description == other.description &&
            colorMatrix.contentEquals(other.colorMatrix) &&
            grainIntensity == other.grainIntensity &&
            halationIntensity == other.halationIntensity &&
            bloomIntensity == other.bloomIntensity &&
            vignetteIntensity == other.vignetteIntensity &&
            chromaticAberration == other.chromaticAberration &&
            scanlineIntensity == other.scanlineIntensity &&
            softness == other.softness &&
            temperature == other.temperature
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + colorMatrix.contentHashCode()
        result = 31 * result + grainIntensity.hashCode()
        result = 31 * result + halationIntensity.hashCode()
        result = 31 * result + bloomIntensity.hashCode()
        result = 31 * result + vignetteIntensity.hashCode()
        result = 31 * result + chromaticAberration.hashCode()
        result = 31 * result + scanlineIntensity.hashCode()
        result = 31 * result + softness.hashCode()
        result = 31 * result + temperature.hashCode()
        return result
    }

    companion object {
        const val COLOR_MATRIX_SIZE = 20
        const val ORIGINAL_ID = "original"

        /** The 4x5 identity color matrix (no color change). */
        fun identityMatrix(): FloatArray = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }
}
