package com.lumacam.core.camera.film

import android.graphics.SurfaceTexture
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import com.lumacam.core.common.film.FilmPreset
import com.lumacam.core.common.film.FilmPresetCatalog
import java.util.concurrent.Executor

/**
 * A CameraX [SurfaceProcessor] that applies the selected [FilmPreset] to streaming
 * frames (live preview and video recording) via a single OpenGL ES 2.0 pass. All GL
 * work runs on a dedicated thread; CameraX callbacks are dispatched onto that same
 * thread through [glExecutor] so no cross-thread posting is needed.
 *
 * Changing the preset only swaps a volatile reference (read per frame) — no rebind
 * or shader recompile. Photos are handled separately by [FilmPhotoBaker].
 */
internal class FilmSurfaceProcessor : SurfaceProcessor {

    @Volatile
    var currentPreset: FilmPreset = FilmPresetCatalog.default

    private val glThread = HandlerThread("FilmGLThread").apply { start() }
    private val glHandler = Handler(glThread.looper)

    /** Executor to hand to the [androidx.camera.core.CameraEffect]. */
    val glExecutor: Executor = Executor { command -> glHandler.post(command) }

    private var eglCore: FilmEglCore? = null
    private var program: FilmGlProgram? = null
    private var setupSurface: EGLSurface? = null
    private var oesTextureId = 0

    private var inputSurfaceTexture: SurfaceTexture? = null
    private var inputSurface: Surface? = null

    private val outputs = mutableMapOf<SurfaceOutput, EGLSurface>()
    private val outputSurfaces = mutableMapOf<SurfaceOutput, Surface>()

    private val stMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val startNanos = System.nanoTime()

    private var released = false

    override fun onInputSurface(request: SurfaceRequest) {
        if (released) {
            request.willNotProvideSurface()
            return
        }
        ensureGl()
        // Replace any previous input.
        releaseInput()

        val texture = SurfaceTexture(oesTextureId).apply {
            setDefaultBufferSize(request.resolution.width, request.resolution.height)
            setOnFrameAvailableListener({ drawFrame() }, glHandler)
        }
        val surface = Surface(texture)
        inputSurfaceTexture = texture
        inputSurface = surface

        request.provideSurface(surface, glExecutor) {
            texture.setOnFrameAvailableListener(null)
            texture.release()
            surface.release()
            if (inputSurfaceTexture === texture) {
                inputSurfaceTexture = null
                inputSurface = null
            }
        }
    }

    override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
        if (released) {
            surfaceOutput.close()
            return
        }
        ensureGl()
        val core = eglCore ?: return
        val surface = surfaceOutput.getSurface(glExecutor) { event ->
            val eglSurface = outputs.remove(event.surfaceOutput)
            outputSurfaces.remove(event.surfaceOutput)
            if (eglSurface != null) core.destroySurface(eglSurface)
            event.surfaceOutput.close()
        }
        try {
            val eglSurface = core.createWindowSurface(surface)
            outputs[surfaceOutput] = eglSurface
            outputSurfaces[surfaceOutput] = surface
        } catch (e: Exception) {
            Log.e(TAG, "createWindowSurface failed", e)
            surfaceOutput.close()
        }
    }

    private fun ensureGl() {
        if (eglCore != null) return
        val core = FilmEglCore()
        eglCore = core
        val setup = core.createPbufferSurface(1, 1)
        setupSurface = setup
        core.makeCurrent(setup)

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        oesTextureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE
        )

        program = FilmGlProgram(FilmShaders.FRAGMENT, oesInput = true)
    }

    private fun drawFrame() {
        if (released) return
        val core = eglCore ?: return
        val texture = inputSurfaceTexture ?: return
        val prog = program ?: return

        try {
            texture.updateTexImage()
            texture.getTransformMatrix(stMatrix)
        } catch (e: Exception) {
            Log.e(TAG, "updateTexImage failed", e)
            return
        }
        if (outputs.isEmpty()) return

        val preset = currentPreset
        val timeSec = ((System.nanoTime() - startNanos) / 1_000_000_000.0).toFloat()
        val timestamp = texture.timestamp

        val iterator = outputs.entries.iterator()
        while (iterator.hasNext()) {
            val (output, eglSurface) = iterator.next()
            try {
                core.makeCurrent(eglSurface)
                output.updateTransformMatrix(mvpMatrix, stMatrix)
                val size = output.size
                GLES20.glViewport(0, 0, size.width, size.height)
                prog.draw(oesTextureId, mvpMatrix, preset, size.width, size.height, timeSec)
                core.setPresentationTime(eglSurface, timestamp)
                core.swapBuffers(eglSurface)
            } catch (e: Exception) {
                Log.e(TAG, "draw to output failed", e)
            }
        }
    }

    private fun releaseInput() {
        inputSurfaceTexture?.setOnFrameAvailableListener(null)
        inputSurfaceTexture?.release()
        inputSurface?.release()
        inputSurfaceTexture = null
        inputSurface = null
    }

    fun release() {
        if (released) return
        released = true
        glHandler.post {
            releaseInput()
            val core = eglCore
            outputs.values.forEach { core?.destroySurface(it) }
            outputs.clear()
            outputSurfaces.keys.forEach { it.close() }
            outputSurfaces.clear()
            program?.release()
            program = null
            if (oesTextureId != 0) {
                GLES20.glDeleteTextures(1, intArrayOf(oesTextureId), 0)
                oesTextureId = 0
            }
            setupSurface?.let { core?.destroySurface(it) }
            setupSurface = null
            core?.makeNothingCurrent()
            core?.release()
            eglCore = null
            glThread.quitSafely()
        }
    }

    private companion object {
        const val TAG = "FilmSurfaceProcessor"
    }
}
