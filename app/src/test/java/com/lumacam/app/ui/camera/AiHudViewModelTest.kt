package com.lumacam.app.ui.camera

import android.graphics.Bitmap
import com.lumacam.feature.ai.AnalysisStage
import com.lumacam.feature.ai.AnalysisState
import com.lumacam.feature.ai.CompositionAnalyzer
import com.lumacam.feature.ai.CompositionResult
import com.lumacam.feature.ai.LightingAssessment
import com.lumacam.feature.ai.MoveDirection
import com.lumacam.feature.ai.SceneType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies the analyze-trigger state machine (BUG 1.6): idle → analyzing
 * (DETECTING_SCENE, no result yet) → result (READY). Uses a fake
 * [CompositionAnalyzer] so the ViewModel wiring is exercised without ML Kit.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AiHudViewModelTest {

    @Test
    fun idleThenAnalyzingThenResult() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher]!!
        Dispatchers.setMain(dispatcher)

        val vm = AiHudViewModel(FakeCompositionAnalyzer())

        // idle
        assertFalse(vm.state.value.active)
        assertNull(vm.state.value.result)

        // trigger analysis with a (Robolectric) frame
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        vm.startAnalysis(bitmap, 0)

        // analyzing: active, stage set, result not yet available
        assertTrue(vm.state.value.active)
        assertEquals(AnalysisStage.DETECTING_SCENE, vm.state.value.stage)
        assertNull(vm.state.value.result)

        // run the (fake) flow to completion
        advanceUntilIdle()

        assertTrue(vm.state.value.active)
        assertEquals(AnalysisStage.READY, vm.state.value.stage)
        assertNotNull(vm.state.value.result)
    }

    private class FakeCompositionAnalyzer : CompositionAnalyzer {
        override fun analyze(frame: Bitmap, rotationDegrees: Int): Flow<AnalysisState> = flow {
            emit(AnalysisState.InProgress(AnalysisStage.DETECTING_SCENE))
            emit(AnalysisState.InProgress(AnalysisStage.FINDING_SUBJECT))
            emit(AnalysisState.InProgress(AnalysisStage.BUILDING_COMPOSITION))
            emit(AnalysisState.Ready(FAKE_RESULT))
        }
    }

    private companion object {
        val FAKE_RESULT = CompositionResult(
            tiltAngle = 0f,
            compositionScore = 100,
            suggestedDirection = MoveDirection.NONE,
            sceneType = SceneType.UNKNOWN,
            lighting = LightingAssessment("label", "description"),
            suggestions = emptyList()
        )
    }
}
