package com.lumacam.feature.ai.vision

import com.lumacam.feature.ai.LightingAssessment
import com.lumacam.feature.ai.RecommendedAction
import com.lumacam.feature.ai.SceneType

/**
 * Pure, JVM-testable builder that turns the scored result into a single friendly
 * line of narrated reasoning for the HUD. Kept separate from [CompositionScorer]
 * so the wording can be tuned and verified without touching scoring weights.
 */
object GuidanceTemplates {

    fun build(
        sceneType: SceneType,
        placement: Int,
        action: RecommendedAction,
        lighting: LightingAssessment
    ): String {
        val scenePhrase = when (sceneType) {
            SceneType.PORTRAIT -> "Portrait subject"
            SceneType.LANDSCAPE -> "Landscape scene"
            SceneType.FOOD -> "Food shot"
            SceneType.NIGHT -> "Low-light scene"
            SceneType.MACRO -> "Close-up subject"
            SceneType.ARCHITECTURE -> "Architectural scene"
            SceneType.UNKNOWN -> "This scene"
        }
        val actionPhrase = when (action) {
            RecommendedAction.REPOSITION ->
                if (placement < 70) {
                    "nudge your subject onto a rule-of-thirds line for a stronger frame"
                } else {
                    "re-aim so your subject sits clear of the edges"
                }
            RecommendedAction.ZOOM_IN -> "zoom in to isolate your subject"
            RecommendedAction.ZOOM_OUT -> "zoom out a touch to give the subject room"
            RecommendedAction.HOLD_AND_SHOOT -> "hold steady — the framing looks great"
            RecommendedAction.NONE -> "hold steady and shoot"
        }
        val lightingPhrase = lighting.description
            .takeIf { it.isNotBlank() && it != "No specific lighting notes." }
            ?.let { " ${it.replaceFirstChar { c -> c.lowercase() }}" }
            .orEmpty()
        return "$scenePhrase — $actionPhrase.$lightingPhrase".trim()
    }
}
