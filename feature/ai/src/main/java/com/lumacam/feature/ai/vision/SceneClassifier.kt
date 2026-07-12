package com.lumacam.feature.ai.vision

import com.lumacam.feature.ai.SceneType

/**
 * Heuristic scene classification from cheap signals: image-labeling labels, face
 * count, primary-subject size, and brightness. No dedicated model — pure logic,
 * so the mapping is unit-testable and runs instantly.
 */
object SceneClassifier {

    private val FOOD = setOf("food", "dish", "meal", "dessert", "cuisine", "plate", "fruit", "drink")
    private val ARCHITECTURE = setOf("building", "architecture", "skyscraper", "house", "tower", "bridge")
    private val LANDSCAPE = setOf("sky", "mountain", "tree", "landscape", "beach", "field", "cloud", "water", "sunset")

    /** Faces this large (fraction of frame) strongly imply a portrait. */
    const val PORTRAIT_FACE_AREA = 0.05f

    /** A subject filling this much of the frame implies a close-up / macro. */
    const val MACRO_AREA = 0.55f

    fun classify(
        labels: List<String>,
        faceCount: Int,
        primarySubjectArea: Float,
        brightness: Float
    ): SceneType {
        val lower = labels.map { it.lowercase() }
        return when {
            LightingAnalyzer.isLowLight(brightness) -> SceneType.NIGHT
            faceCount > 0 && primarySubjectArea >= PORTRAIT_FACE_AREA -> SceneType.PORTRAIT
            lower.any { l -> FOOD.any { l.contains(it) } } -> SceneType.FOOD
            lower.any { l -> ARCHITECTURE.any { l.contains(it) } } -> SceneType.ARCHITECTURE
            primarySubjectArea >= MACRO_AREA -> SceneType.MACRO
            lower.any { l -> LANDSCAPE.any { l.contains(it) } } -> SceneType.LANDSCAPE
            else -> SceneType.UNKNOWN
        }
    }
}
