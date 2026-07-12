package com.lumacam.feature.ai.vision

import android.graphics.Bitmap
import com.lumacam.feature.ai.AnalysisStage
import com.lumacam.feature.ai.AnalysisState
import com.lumacam.feature.ai.CompositionAnalyzer
import com.lumacam.feature.ai.CompositionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Real [CompositionAnalyzer] backed by Luma Vision (PRD §4 Tier 2). Wraps the
 * single-pass [LumaVisionAnalyzer] engine and adapts it to the staged
 * [AnalysisState] stream the HUD consumes, mirroring the cadence the mock used:
 * DETECTING_SCENE → FINDING_SUBJECT → BUILDING_COMPOSITION → READY.
 *
 * The genuine inference (ML Kit detection + sensor tilt + scoring) is heavy, so it
 * runs on [Dispatchers.IO]; stage emissions themselves stay on the collector's
 * dispatcher so the HUD overlay can show progress without blocking the UI.
 */
class LumaVisionCompositionAnalyzer(
    private val vision: LumaVisionAnalyzer
) : CompositionAnalyzer {

    override fun analyze(frame: Bitmap, rotationDegrees: Int): Flow<AnalysisState> = flow {
        emit(AnalysisState.InProgress(AnalysisStage.DETECTING_SCENE))
        val result: CompositionResult = withContext(Dispatchers.IO) {
            vision.analyze(frame, rotationDegrees)
        }
        emit(AnalysisState.InProgress(AnalysisStage.FINDING_SUBJECT))
        emit(AnalysisState.InProgress(AnalysisStage.BUILDING_COMPOSITION))
        emit(AnalysisState.Ready(result))
        // One-shot preview grab — release it once inference is done.
        if (!frame.isRecycled) frame.recycle()
    }
}
