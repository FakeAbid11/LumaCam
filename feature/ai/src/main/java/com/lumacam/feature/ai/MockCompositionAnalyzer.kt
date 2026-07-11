package com.lumacam.feature.ai

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Hardcoded stand-in for the real AI pipeline (Prompt 6/7). Walks through the
 * analysis stages with realistic delays, then emits a fixed [CompositionResult]
 * chosen to demonstrate every HUD overlay — including a perfect 100 score so the
 * shutter glow + haptic fire.
 */
class MockCompositionAnalyzer : CompositionAnalyzer {

    override fun analyze(): Flow<AnalysisState> = flow {
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
            targetCrop = CropBounds(left = 0.32f, top = 0.14f, right = 0.94f, bottom = 0.92f)
        )
    }
}
