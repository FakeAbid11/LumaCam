package com.lumacam.feature.ai.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import com.lumacam.feature.ai.CompositionResult

/**
 * Luma Vision — LumaCam's lightweight, offline composition-analysis engine
 * (PRD §4 Tier 2). Combines ML Kit subject/scene detection with sensor-based tilt
 * and a pure-Kotlin scoring core to produce a real [CompositionResult] from a
 * single camera frame, targeting well under one second on mid-range hardware.
 *
 * This is the engine only. Wiring it into the "✨ Analyze" flow happens in a later
 * step; here it is exposed via a clean [analyze] entry point plus tested scoring.
 */
class LumaVisionAnalyzer(
    private val detector: SubjectDetector,
    private val tiltProvider: TiltProvider
) {

    /**
     * Analyzes one [frame]. [rotationDegrees] is the clockwise rotation needed to
     * make the frame upright (as reported by CameraX); it is applied before
     * detection. Never throws — detection failures degrade gracefully.
     */
    suspend fun analyze(frame: Bitmap, rotationDegrees: Int = 0): CompositionResult {
        // Intermediate bitmaps are recycled as soon as they are no longer needed so
        // we never leak native (ashmem) allocations across many analysis frames.
        val upright = rotate(frame, rotationDegrees)
        val scaled = downscale(upright, MAX_DETECT_EDGE)
        val detection = detector.detect(scaled)
        val pixels = samplePixels(upright)
        val brightness = Luminance.average(pixels)
        val tilt = tiltProvider.currentTiltDegrees()
        if (upright !== frame) upright.recycle()
        if (scaled !== upright) scaled.recycle()
        return CompositionScorer.score(detection, tilt, brightness)
    }

    /** Starts the tilt sensor if one is attached. */
    fun start() {
        (tiltProvider as? SensorTiltProvider)?.start()
    }

    /** Stops the tilt sensor and releases detector resources. */
    fun stop() {
        (tiltProvider as? SensorTiltProvider)?.stop()
    }

    fun close() {
        stop()
        detector.close()
    }

    // NOTE (hardening audit, Prompt 12 follow-up): this analyzer, its ML Kit
    // detector and the tilt sensor are never started/closed by an owner today
    // (the real analysis pipeline from Prompt 7 is not yet wired to a screen).
    // When it is connected, the screen/ViewModel MUST call start() on attach and
    // close() on detach/onCleared — otherwise the accelerometer listener and ML
    // Kit clients leak. Tracked for the Prompt 9 real-runtime follow-up.

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        val normalized = ((degrees % 360) + 360) % 360
        if (normalized == 0) return bitmap
        val matrix = Matrix().apply { postRotate(normalized.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun downscale(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxEdge) return bitmap
        val scale = maxEdge.toFloat() / longest
        val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    private fun samplePixels(bitmap: Bitmap): IntArray {
        val sample = Bitmap.createScaledBitmap(bitmap, BRIGHTNESS_EDGE, BRIGHTNESS_EDGE, true)
        val pixels = IntArray(BRIGHTNESS_EDGE * BRIGHTNESS_EDGE)
        sample.getPixels(pixels, 0, BRIGHTNESS_EDGE, 0, 0, BRIGHTNESS_EDGE, BRIGHTNESS_EDGE)
        return pixels
    }

    companion object {
        private const val MAX_DETECT_EDGE = 640
        private const val BRIGHTNESS_EDGE = 32

        /** Convenience factory wiring the on-device ML Kit + accelerometer stack. */
        fun create(context: Context): LumaVisionAnalyzer =
            LumaVisionAnalyzer(
                detector = MlKitSubjectDetector(),
                tiltProvider = SensorTiltProvider(context.applicationContext)
            )
    }
}
