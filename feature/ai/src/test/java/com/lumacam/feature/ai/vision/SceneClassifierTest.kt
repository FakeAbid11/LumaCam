package com.lumacam.feature.ai.vision

import com.lumacam.feature.ai.SceneType
import org.junit.Assert.assertEquals
import org.junit.Test

class SceneClassifierTest {

    @Test
    fun lowLightIsNight() {
        assertEquals(SceneType.NIGHT, SceneClassifier.classify(emptyList(), 0, 0f, 0.05f))
    }

    @Test
    fun largeFaceIsPortrait() {
        assertEquals(SceneType.PORTRAIT, SceneClassifier.classify(emptyList(), 1, 0.1f, 0.5f))
    }

    @Test
    fun portraitWinsOverFoodLabel() {
        assertEquals(SceneType.PORTRAIT, SceneClassifier.classify(listOf("Food"), 1, 0.1f, 0.5f))
    }

    @Test
    fun foodLabelIsFood() {
        assertEquals(SceneType.FOOD, SceneClassifier.classify(listOf("Food"), 0, 0.2f, 0.5f))
    }

    @Test
    fun buildingLabelIsArchitecture() {
        assertEquals(
            SceneType.ARCHITECTURE,
            SceneClassifier.classify(listOf("Building"), 0, 0.2f, 0.5f)
        )
    }

    @Test
    fun largeSubjectIsMacro() {
        assertEquals(SceneType.MACRO, SceneClassifier.classify(emptyList(), 0, 0.6f, 0.5f))
    }

    @Test
    fun skyLabelIsLandscape() {
        assertEquals(SceneType.LANDSCAPE, SceneClassifier.classify(listOf("Sky"), 0, 0.1f, 0.5f))
    }

    @Test
    fun nothingRecognizedIsUnknown() {
        assertEquals(SceneType.UNKNOWN, SceneClassifier.classify(emptyList(), 0, 0.1f, 0.5f))
    }
}
