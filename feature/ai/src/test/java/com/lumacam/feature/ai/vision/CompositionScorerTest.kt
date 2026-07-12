package com.lumacam.feature.ai.vision

import com.lumacam.feature.ai.MoveDirection
import com.lumacam.feature.ai.SceneType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositionScorerTest {

    private fun subjectAt(cx: Float, cy: Float, half: Float = 0.05f) = DetectionOutput(
        subjects = listOf(
            DetectedSubject(
                type = SubjectType.OBJECT,
                box = NormalizedBox(cx - half, cy - half, cx + half, cy + half),
                confidence = 0.9f
            )
        )
    )

    @Test
    fun noSubjectFallsBackToTiltAndBalance() {
        val result = CompositionScorer.score(DetectionOutput(), tiltDegrees = 0f, brightness = 0.5f)
        assertEquals(84, result.compositionScore)
        assertEquals(MoveDirection.NONE, result.suggestedDirection)
        assertNull(result.targetCrop)
        assertEquals(SceneType.UNKNOWN, result.sceneType)
        assertTrue(result.suggestions.any { it.contains("Point at your subject") })
    }

    @Test
    fun subjectOnIntersectionScoresPerfect() {
        val result = CompositionScorer.score(
            subjectAt(1f / 3f, 1f / 3f), tiltDegrees = 0f, brightness = 0.5f
        )
        assertEquals(100, result.compositionScore)
        assertEquals(MoveDirection.NONE, result.suggestedDirection)
        assertNull(result.targetCrop)
    }

    @Test
    fun centeredSubjectIsScoredAndCropped() {
        val result = CompositionScorer.score(
            subjectAt(0.5f, 0.5f), tiltDegrees = 0f, brightness = 0.5f
        )
        assertEquals(75, result.compositionScore)
        assertEquals(MoveDirection.LEFT, result.suggestedDirection)
        assertNotNull(result.targetCrop)
    }

    @Test
    fun tiltProducesStraightenTipFirst() {
        val result = CompositionScorer.score(
            subjectAt(1f / 3f, 1f / 3f), tiltDegrees = 8f, brightness = 0.5f
        )
        assertEquals(8f, result.tiltAngle, 0f)
        assertTrue(result.suggestions.first().startsWith("Straighten"))
    }

    @Test
    fun suggestionsAreCappedAtThree() {
        val tips = CompositionScorer.buildSuggestions(
            primary = null, placement = 0, tiltDegrees = 8f, brightness = 0.05f
        )
        assertEquals(3, tips.size)
    }
}
