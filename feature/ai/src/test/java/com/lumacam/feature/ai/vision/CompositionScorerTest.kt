package com.lumacam.feature.ai.vision

import com.lumacam.feature.ai.NormalizedPoint
import com.lumacam.feature.ai.RecommendedAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the staged-reveal fields produced by [CompositionScorer]: the subject
 * point, the recommended one-tap action, and the narrated guidance line. All pure
 * JVM logic, so no Android/Robolectric needed.
 */
class CompositionScorerTest {

    private fun detectionWith(box: NormalizedBox): DetectionOutput =
        DetectionOutput(
            subjects = listOf(DetectedSubject(SubjectType.OBJECT, box, 0.9f, "cat")),
            labels = listOf("cat")
        )

    @Test
    fun noSubject_recommendsRepositionAndNoPoint() {
        val result = CompositionScorer.score(DetectionOutput(), 0f, 0.5f)
        assertNull(result.subjectPoint)
        assertEquals(RecommendedAction.REPOSITION, result.recommendedAction)
        assertTrue(result.primaryGuidance?.isNotEmpty() == true)
    }

    @Test
    fun smallSubject_recommendsZoomInWithCenterPoint() {
        // 0.04 area -> below the SMALL_SUBJECT_AREA threshold.
        val box = NormalizedBox(0.48f, 0.48f, 0.52f, 0.52f)
        val result = CompositionScorer.score(detectionWith(box), 0f, 0.5f)
        assertEquals(NormalizedPoint(0.5f, 0.5f), result.subjectPoint)
        assertEquals(RecommendedAction.ZOOM_IN, result.recommendedAction)
    }

    @Test
    fun wellPlacedLargeSubject_recommendsHoldAndShoot() {
        val half = 0.25f
        val cx = 1f / 3f
        val cy = 1f / 3f
        val box = NormalizedBox(cx - half, cy - half, cx + half, cy + half)
        val result = CompositionScorer.score(detectionWith(box), 0f, 0.5f)
        assertEquals(NormalizedPoint(cx, cy), result.subjectPoint)
        assertEquals(RecommendedAction.HOLD_AND_SHOOT, result.recommendedAction)
    }

    @Test
    fun guidanceMentionsAction() {
        val box = NormalizedBox(0.48f, 0.48f, 0.52f, 0.52f)
        val result = CompositionScorer.score(detectionWith(box), 0f, 0.5f)
        val guidance = result.primaryGuidance.orEmpty()
        assertTrue(guidance.contains("zoom in", ignoreCase = true))
    }
}
