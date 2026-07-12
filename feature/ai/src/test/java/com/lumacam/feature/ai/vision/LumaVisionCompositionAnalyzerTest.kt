package com.lumacam.feature.ai.vision

import android.graphics.Bitmap
import com.lumacam.feature.ai.AnalysisStage
import com.lumacam.feature.ai.AnalysisState
import com.lumacam.feature.ai.CompositionResult
import com.lumacam.feature.ai.MoveDirection
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies the real analysis adapter (BUG 1): it streams the staged
 * DETECTING_SCENE → FINDING_SUBJECT → BUILDING_COMPOSITION → READY states and
 * emits a genuine [CompositionResult] produced by Luma Vision from a faked
 * detector/tilt (no real ML Kit inference).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LumaVisionCompositionAnalyzerTest {

    private fun subjectOnThird(): DetectionOutput {
        val half = 0.05f
        val cx = 1f / 3f
        val cy = 1f / 3f
        return DetectionOutput(
            subjects = listOf(
                DetectedSubject(
                    type = SubjectType.OBJECT,
                    box = NormalizedBox(cx - half, cy - half, cx + half, cy + half),
                    confidence = 0.9f,
                    label = "cat"
                )
            ),
            labels = listOf("cat")
        )
    }

    @Test
    fun emitsStagedStatesEndingInReadyWithRealResult() = runTest {
        val fakeDetector = object : SubjectDetector {
            override suspend fun detect(bitmap: Bitmap): DetectionOutput = subjectOnThird()
            override fun close() = Unit
        }
        val fakeTilt = object : TiltProvider {
            override fun currentTiltDegrees(): Float = 0f
            fun start() = Unit
            fun stop() = Unit
        }
        val analyzer = LumaVisionCompositionAnalyzer(LumaVisionAnalyzer(fakeDetector, fakeTilt))

        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val states = analyzer.analyze(bitmap, 0).toList()

        assertEquals(
            listOf(
                AnalysisStage.DETECTING_SCENE,
                AnalysisStage.FINDING_SUBJECT,
                AnalysisStage.BUILDING_COMPOSITION
            ),
            states.filterIsInstance<AnalysisState.InProgress>().map { it.stage }
        )
        val ready = states.filterIsInstance<AnalysisState.Ready>().single()
        val result: CompositionResult = ready.result
        // Subject on a rule-of-thirds intersection scores a perfect 100 (per
        // CompositionScorer) — proving the result is real, not the mock's fixed data.
        assertEquals(100, result.compositionScore)
        assertEquals(MoveDirection.NONE, result.suggestedDirection)
        assertTrue(bitmap.isRecycled)
    }

    @Test
    fun tiltedFrameSurfacesStraightenSuggestion() = runTest {
        val fakeDetector = object : SubjectDetector {
            override suspend fun detect(bitmap: Bitmap): DetectionOutput = DetectionOutput()
            override fun close() = Unit
        }
        val fakeTilt = object : TiltProvider {
            override fun currentTiltDegrees(): Float = 8f
            fun start() = Unit
            fun stop() = Unit
        }
        val analyzer = LumaVisionCompositionAnalyzer(LumaVisionAnalyzer(fakeDetector, fakeTilt))

        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val ready = analyzer.analyze(bitmap, 0).toList()
            .filterIsInstance<AnalysisState.Ready>().single()
        assertEquals(8f, ready.result.tiltAngle, 0f)
        assertTrue(ready.result.suggestions.first().startsWith("Straighten"))
    }
}
