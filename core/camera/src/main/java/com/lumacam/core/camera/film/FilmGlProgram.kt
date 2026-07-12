package com.lumacam.core.camera.film

import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.lumacam.core.common.film.FilmPreset
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * A compiled GLSL program for the Film Camera Engine plus the vertex data and
 * uniform plumbing to render one full-screen film-graded quad. Shared by the live
 * [FilmSurfaceProcessor] (external-OES input) and the offscreen [FilmPhotoBaker]
 * (2D input), so both paths apply an identical look.
 *
 * Not thread-safe: create and use on a single GL thread with a current context.
 */
internal class FilmGlProgram(fragmentSource: String, private val oesInput: Boolean) {

    private val programId: Int
    private val aPosition: Int
    private val aTextureCoord: Int
    private val uTexMatrix: Int
    private val uResolution: Int
    private val uTime: Int
    private val uColorMatrix: Int
    private val uColorOffset: Int
    private val uGrain: Int
    private val uHalation: Int
    private val uBloom: Int
    private val uVignette: Int
    private val uChroma: Int
    private val uScanline: Int
    private val uSoftness: Int
    private val uTemperature: Int
    private val uSampler: Int

    private val positionBuffer: FloatBuffer = floatBuffer(POSITIONS)
    private val texCoordBuffer: FloatBuffer

    init {
        programId = FilmGl.buildProgram(FilmShaders.VERTEX, fragmentSource)
        aPosition = GLES20.glGetAttribLocation(programId, "aPosition")
        aTextureCoord = GLES20.glGetAttribLocation(programId, "aTextureCoord")
        uTexMatrix = GLES20.glGetUniformLocation(programId, "uTexMatrix")
        uResolution = GLES20.glGetUniformLocation(programId, "uResolution")
        uTime = GLES20.glGetUniformLocation(programId, "uTime")
        uColorMatrix = GLES20.glGetUniformLocation(programId, "uColorMatrix")
        uColorOffset = GLES20.glGetUniformLocation(programId, "uColorOffset")
        uGrain = GLES20.glGetUniformLocation(programId, "uGrain")
        uHalation = GLES20.glGetUniformLocation(programId, "uHalation")
        uBloom = GLES20.glGetUniformLocation(programId, "uBloom")
        uVignette = GLES20.glGetUniformLocation(programId, "uVignette")
        uChroma = GLES20.glGetUniformLocation(programId, "uChroma")
        uScanline = GLES20.glGetUniformLocation(programId, "uScanline")
        uSoftness = GLES20.glGetUniformLocation(programId, "uSoftness")
        uTemperature = GLES20.glGetUniformLocation(programId, "uTemperature")
        uSampler = GLES20.glGetUniformLocation(programId, "sTexture")
        // OES path relies on the camera's texture matrix; the 2D path renders to a
        // pbuffer read back via glReadPixels, whose bottom-up read cancels GL's
        // bottom-left origin — so both paths use the same (non-flipped) coords.
        texCoordBuffer = floatBuffer(TEX_COORDS)
    }

    /**
     * Renders [textureId] through the film shader into the currently-bound
     * framebuffer/surface (the caller sets the viewport).
     *
     * @param texMatrix 4x4 texture transform (identity for the 2D path).
     * @param preset the film look to apply.
     * @param width output width in pixels (for resolution-dependent effects).
     * @param height output height in pixels.
     * @param timeSeconds animation clock for grain.
     */
    fun draw(
        textureId: Int,
        texMatrix: FloatArray,
        preset: FilmPreset,
        width: Int,
        height: Int,
        timeSeconds: Float
    ) {
        GLES20.glUseProgram(programId)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        val target = if (oesInput) GLES11Ext.GL_TEXTURE_EXTERNAL_OES else GLES20.GL_TEXTURE_2D
        GLES20.glBindTexture(target, textureId)
        GLES20.glUniform1i(uSampler, 0)

        GLES20.glUniformMatrix4fv(uTexMatrix, 1, false, texMatrix, 0)
        GLES20.glUniform2f(uResolution, width.toFloat(), height.toFloat())
        GLES20.glUniform1f(uTime, timeSeconds)

        GLES20.glUniformMatrix4fv(uColorMatrix, 1, false, colorMatrixLinear(preset.colorMatrix), 0)
        val off = colorMatrixOffset(preset.colorMatrix)
        GLES20.glUniform4f(uColorOffset, off[0], off[1], off[2], off[3])

        GLES20.glUniform1f(uGrain, preset.grainIntensity)
        GLES20.glUniform1f(uHalation, preset.halationIntensity)
        GLES20.glUniform1f(uBloom, preset.bloomIntensity)
        GLES20.glUniform1f(uVignette, preset.vignetteIntensity)
        GLES20.glUniform1f(uChroma, preset.chromaticAberration)
        GLES20.glUniform1f(uScanline, preset.scanlineIntensity)
        GLES20.glUniform1f(uSoftness, preset.softness)
        GLES20.glUniform1f(uTemperature, preset.temperature)

        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_FLOAT, false, 0, positionBuffer)
        GLES20.glEnableVertexAttribArray(aTextureCoord)
        GLES20.glVertexAttribPointer(aTextureCoord, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPosition)
        GLES20.glDisableVertexAttribArray(aTextureCoord)
        GLES20.glBindTexture(target, 0)
    }

    fun release() {
        GLES20.glDeleteProgram(programId)
    }

    private companion object {
        val POSITIONS = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
        val TEX_COORDS = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f)

        fun floatBuffer(data: FloatArray): FloatBuffer =
            ByteBuffer.allocateDirect(data.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply { put(data); position(0) }

        /**
         * Converts a 4x5 row-major ColorMatrix into the column-major 4x4 linear
         * part expected by a GLSL `mat4` (GLES2 forbids transpose on upload).
         */
        fun colorMatrixLinear(m: FloatArray): FloatArray {
            val out = FloatArray(16)
            for (row in 0 until 4) {
                for (col in 0 until 4) {
                    // GLSL column-major: out[col*4 + row] = m[row][col]
                    out[col * 4 + row] = m[row * 5 + col]
                }
            }
            return out
        }

        /** Extracts the 4-channel offset column, normalized from 0..255 to 0..1. */
        fun colorMatrixOffset(m: FloatArray): FloatArray = floatArrayOf(
            m[4] / 255f, m[9] / 255f, m[14] / 255f, m[19] / 255f
        )
    }
}
