package com.lumacam.feature.ai.vision

import com.lumacam.feature.ai.LightingAssessment

/**
 * Maps average frame brightness (0..1) to a human-readable lighting assessment.
 * Pure logic — thresholds are unit-tested.
 */
object LightingAnalyzer {

    const val LOW_LIGHT_MAX = 0.18f
    const val BRIGHT_MIN = 0.85f

    fun assess(brightness: Float): LightingAssessment = when {
        brightness < LOW_LIGHT_MAX -> LightingAssessment(
            label = "Low light",
            description = "It's dark — hold steady, brace the phone, or add light to reduce blur."
        )
        brightness > BRIGHT_MIN -> LightingAssessment(
            label = "Very bright",
            description = "Highlights may clip. Lower exposure or avoid pointing at the light source."
        )
        else -> LightingAssessment(
            label = "Good light",
            description = "Even, workable light. Keep the light behind you for the cleanest result."
        )
    }

    /** True when brightness is low enough to hint at a night/low-light scene. */
    fun isLowLight(brightness: Float): Boolean = brightness < LOW_LIGHT_MAX
}
