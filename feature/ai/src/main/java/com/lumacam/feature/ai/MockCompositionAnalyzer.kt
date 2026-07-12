package com.lumacam.feature.ai

import android.graphics.Bitmap
import com.lumacam.feature.ai.NormalizedPoint
import com.lumacam.feature.ai.RecommendedAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Hardcoded stand-in for the real AI pipeline. Kept (unbound) for Compose Previews
 * and fast UI iteration without running real ML Kit inference. Walks through the
 * analysis stages with realistic delays, then emits a fixed [CompositionResult]
 * chosen to demonstrate every HUD overlay — including a perfect 100 score so the
 * shutter glow + haptic fire. The [frame]/[rotationDegrees] arguments are ignored.
 */
class MockCompositionAnalyzer : CompositionAnalyzer {

    override fun analyze(frame: Bitmap, rotationDegrees: Int): Flow<AnalysisState> = flow {
        emit(AnalysisState.InProgress(AnalysisStage.DETECTING_SCENE))
        delay(STAGE_DELAY_MS)
        emit(AnalysisState.InProgress(AnalysisStage.FINDING_SUBJECT))
        delay(STAGE_DELAY_MS)
        emit(AnalysisState.InProgress(AnalysisStage.BUILDING_COMPOSITION))
        delay(STAGE_DELAY_MS)
        emit(AnalysisState.Ready(DEMO_RESULT))
    }

    private companion object {
        const val STAGE_DELAY_MS = 700L

        val DEMO_RESULT = CompositionResult(
            tiltAngle = 0.4f,
            compositionScore = 100,
            suggestedDirection = MoveDirection.LEFT,
            sceneType = SceneType.PORTRAIT,
            lighting = LightingAssessment(
                label = "Soft side light",
                description = "Even, flattering light. Keep the subject facing the window."
            ),
            suggestions = listOf(
                "Move slightly left to place the subject on the right third.",
                "Lower the camera a touch for a stronger eye line.",
                "Great separation from the background — hold steady."
            ),
            targetCrop = CropBounds(left = 0.32f, top = 0.14f, right = 0.94f, bottom = 0.92f),
            subjectPoint = NormalizedPoint(x = 0.6f, y = 0.4f),
            recommendedAction = RecommendedAction.REPOSITION,
            primaryGuidance = "Portrait subject — nudge your subject onto a rule-of-thirds line for a stronger frame. Even, flattering light."
        )
    }
}
