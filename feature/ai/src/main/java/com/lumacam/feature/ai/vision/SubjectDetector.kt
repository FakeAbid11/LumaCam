package com.lumacam.feature.ai.vision

import android.graphics.Bitmap

/**
 * Detects subjects and coarse scene labels in a single frame. Abstracted away
 * from the concrete ML backend ([MlKitSubjectDetector]) so the analyzer stays
 * testable and the PRD's tier boundary (§4/§6) is preserved.
 */
interface SubjectDetector {
    /**
     * Runs one detection pass on [bitmap] (already rotated upright). Implementations
     * must not throw; on failure they return an empty [DetectionOutput].
     */
    suspend fun detect(bitmap: Bitmap): DetectionOutput

    /** Releases any native resources held by the detector. */
    fun close()
}
