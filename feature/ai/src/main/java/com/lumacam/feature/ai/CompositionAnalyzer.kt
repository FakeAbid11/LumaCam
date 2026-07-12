package com.lumacam.feature.ai

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow

/**
 * Source of AI composition guidance. Prompt 5 shipped [MockCompositionAnalyzer];
 * the real on-device Luma Vision pipeline ([LumaVisionCompositionAnalyzer]) is now
 * bound via Hilt and drives the same [AnalysisState] stream, so the HUD
 * presentation layer never changed.
 */
interface CompositionAnalyzer {
    /**
     * Runs one analysis pass over a single [frame] (already grabbed from the live
     * preview, [rotationDegrees] being the clockwise rotation needed to make it
     * upright). Emits [AnalysisState.InProgress] for each stage and finishes with a
     * single [AnalysisState.Ready] carrying the genuine [CompositionResult].
     */
    fun analyze(frame: Bitmap, rotationDegrees: Int = 0): Flow<AnalysisState>
}
