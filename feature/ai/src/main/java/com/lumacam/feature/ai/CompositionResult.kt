package com.lumacam.feature.ai

import kotlin.math.abs

/**
 * The single, presentation-agnostic model that drives every AI guidance HUD
 * overlay (horizon, ghost crop, directional arrow, score ring, assistant sheet).
 *
 * Prompt 5 feeds this from [MockCompositionAnalyzer]. Prompt 6/7 will produce the
 * exact same type from the real on-device / Gemini pipeline, so the HUD never
 * needs to change.
 *
 * @param tiltAngle horizon tilt in degrees; negative = rotated counter-clockwise.
 * @param compositionScore overall composition quality, 0..100.
 * @param suggestedDirection which way the user should nudge the framing.
 * @param sceneType detected scene classification.
 * @param lighting a short human-readable lighting assessment.
 * @param suggestions ordered coaching tips shown in the assistant sheet.
 * @param targetCrop optional recommended crop, in normalized [0,1] coordinates.
 */
data class CompositionResult(
    val tiltAngle: Float,
    val compositionScore: Int,
    val suggestedDirection: MoveDirection,
    val sceneType: SceneType,
    val lighting: LightingAssessment,
    val suggestions: List<String>,
    val targetCrop: CropBounds? = null
) {
    /** True when the frame is level enough to hide the tilt warning. */
    val isLevel: Boolean get() = abs(tiltAngle) <= LEVEL_THRESHOLD_DEGREES

    companion object {
        const val LEVEL_THRESHOLD_DEGREES = 1.5f
    }
}

/** Direction the user should move the camera to improve framing. */
enum class MoveDirection { UP, DOWN, LEFT, RIGHT, NONE }

/** Coarse scene classification surfaced to the user. */
enum class SceneType(val displayName: String) {
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape"),
    FOOD("Food"),
    NIGHT("Night"),
    MACRO("Macro"),
    ARCHITECTURE("Architecture"),
    UNKNOWN("Scene")
}

/** Short lighting assessment: a label plus a one-line description. */
data class LightingAssessment(
    val label: String,
    val description: String
)

/** Recommended crop rectangle in normalized [0,1] coordinates. */
data class CropBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

/** Ordered stages reported while the analyzer works, ending at [READY]. */
enum class AnalysisStage(val label: String) {
    DETECTING_SCENE("Detecting scene"),
    FINDING_SUBJECT("Finding subject"),
    BUILDING_COMPOSITION("Building composition"),
    READY("Ready")
}

/** Streamed analyzer output consumed by the HUD view model. */
sealed interface AnalysisState {
    /** No analysis running. */
    data object Idle : AnalysisState

    /** Analysis in progress at [stage]. */
    data class InProgress(val stage: AnalysisStage) : AnalysisState

    /** Analysis complete; overlays can render from [result]. */
    data class Ready(val result: CompositionResult) : AnalysisState
}
