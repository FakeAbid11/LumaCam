package com.lumacam.core.camera.film

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.Log
import com.lumacam.core.common.film.FilmPreset
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Bakes a [FilmPreset] into a captured JPEG at full resolution by running the same
 * film shader offscreen (2D texture → pbuffer → glReadPixels). This guarantees the
 * saved photo matches the live preview (WYSIWYG) without relying on CameraX's
 * still-capture SurfaceProcessor routing.
 *
 * Each call uses its own short-lived EGL context so it is isolated from the live
 * preview GL thread and safe to run on a background executor. Failures (including
 * out-of-memory on very large images) leave the original file untouched rather than
 * crashing.
 */
internal class FilmPhotoBaker {

    /**
     * Applies [preset] to [file] in place. No-op for the identity preset. Returns
     * true when the file was successfully re-written with the effect baked in.
     */
    fun bake(file: File, preset: FilmPreset): Boolean {
        if (preset.isIdentity) return false
        if (!file.exists()) return false

        var eglCore: FilmEglCore? = null
        var pbuffer: EGLSurface? = null
        var program: FilmGlProgram? = null
        var source: Bitmap? = null
        var output: Bitmap? = null
        return try {
            val orientation = readOrientation(file)

            source = BitmapFactory.decodeFile(file.absolutePath)
                ?: return false
            val width = source.width
            val height = source.height

            eglCore = FilmEglCore()
            pbuffer = eglCore.createPbufferSurface(width, height)
            eglCore.makeCurrent(pbuffer)

            val textureId = uploadTexture(source)
            program = FilmGlProgram(FilmShaders.FRAGMENT_2D, oesInput = false)

            GLES20.glViewport(0, 0, width, height)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            program.draw(textureId, IDENTITY, preset, width, height, timeSeconds = 0.37f)

            output = readPixels(width, height)
            GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)

            FileOutputStream(file).use { out ->
                output.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            writeOrientation(file, orientation)
            true
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "bake OOM; leaving original photo", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "bake failed; leaving original photo", e)
            false
        } finally {
            source?.recycle()
            output?.recycle()
            program?.release()
            pbuffer?.let { eglCore?.destroySurface(it) }
            eglCore?.makeNothingCurrent()
            eglCore?.release()
        }
    }

    private fun uploadTexture(bitmap: Bitmap): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        val id = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE
        )
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        return id
    }

    private fun readPixels(width: Int, height: Int): Bitmap {
        val buffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
        GLES20.glReadPixels(
            0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer
        )
        buffer.rewind()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        return bitmap
    }

    private fun readOrientation(file: File): Int = try {
        ExifInterface(file.absolutePath)
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } catch (e: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }

    private fun writeOrientation(file: File, orientation: Int) {
        if (orientation == ExifInterface.ORIENTATION_NORMAL) return
        try {
            val exif = ExifInterface(file.absolutePath)
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            exif.saveAttributes()
        } catch (e: Exception) {
            Log.e(TAG, "failed to restore EXIF orientation", e)
        }
    }

    private companion object {
        const val TAG = "FilmPhotoBaker"
        const val JPEG_QUALITY = 95
        val IDENTITY = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        )
    }
}
