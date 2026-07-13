package com.lumacam.app.ui.camera

import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ARGB_8888
import com.lumacam.app.data.AiMode
import com.lumacam.app.data.CloudAiCredentials
import com.lumacam.app.data.LocalModelRepository
import com.lumacam.app.data.LocalModelStorage
import com.lumacam.app.data.SettingsRepository
import com.lumacam.feature.ai.AnalysisStage
import com.lumacam.feature.ai.AnalysisState
import com.lumacam.feature.ai.CompositionAnalyzer
import com.lumacam.feature.ai.CompositionResult
import com.lumacam.feature.ai.LightingAssessment
import com.lumacam.feature.ai.MoveDirection
import com.lumacam.feature.ai.SceneType
import com.lumacam.feature.ai.cloud.CloudAiOutcome
import com.lumacam.feature.ai.cloud.CloudAiProvider
import com.lumacam.feature.ai.cloud.CloudImage
import com.lumacam.feature.ai.cloud.CloudProviderType
import com.lumacam.feature.ai.cloud.ConnectionTestResult
import com.lumacam.feature.ai.local.LocalAiOutcome
import com.lumacam.feature.ai.local.LocalAiProvider
import com.lumacam.feature.ai.local.LocalImage
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
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Verifies the analyze-trigger state machine (BUG 1.6): idle -> analyzing
 * (DETECTING_SCENE, no result yet) -> result (READY), and that the chosen AI
 * backend is actually routed to (Luma Vision / Cloud AI / Local AI / SMART
 * fallback). Uses fakes + a real SettingsRepository so the ViewModel wiring is
 * exercised without ML Kit or network.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AiHudViewModelTest {

    private val context = RuntimeEnvironment.getApplication()

    @Test
    fun idleThenAnalyzingThenResult() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher]!!
        Dispatchers.setMain(dispatcher)

        val settings = SettingsRepository(context)
        val vm = AiHudViewModel(
            analyzer = FakeCompositionAnalyzer(),
            cloudAiProvider = FakeCloudAiProvider(CloudAiOutcome.Success(FAKE_RESULT)),
            localAiProvider = FakeLocalAiProvider(LocalAiOutcome.Success(FAKE_RESULT)),
            settingsRepository = settings,
            cloudAiCredentials = FakeCloudAiCredentials(),
            localModelRepository = LocalModelRepository(context, LocalModelStorage(context))
        )
        vm.setAiMode(AiMode.LUMA_VISION)

        // idle
        assertFalse(vm.state.value.active)
        assertNull(vm.state.value.result)

        vm.startAnalysis(Bitmap.createBitmap(1, 1, ARGB_8888), 0)

        assertTrue(vm.state.value.active)
        assertEquals(AnalysisStage.DETECTING_SCENE, vm.state.value.stage)
        assertNull(vm.state.value.result)

        advanceUntilIdle()

        assertTrue(vm.state.value.active)
        assertEquals(AnalysisStage.READY, vm.state.value.stage)
        assertNotNull(vm.state.value.result)
    }

    @Test
    fun cloudModeRoutesToCloudProvider() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher]!!
        Dispatchers.setMain(dispatcher)

        val settings = SettingsRepository(context)
        val cloud = FakeCloudAiProvider(CloudAiOutcome.Success(CLOUD_RESULT))
        val vm = AiHudViewModel(
            analyzer = FakeCompositionAnalyzer(),
            cloudAiProvider = cloud,
            localAiProvider = FakeLocalAiProvider(LocalAiOutcome.Success(FAKE_RESULT)),
            settingsRepository = settings,
            cloudAiCredentials = FakeCloudAiCredentials(),
            localModelRepository = LocalModelRepository(context, LocalModelStorage(context))
        )
        vm.setAiMode(AiMode.CLOUD_AI)

        vm.startAnalysis(Bitmap.createBitmap(1, 1, ARGB_8888), 0)
        advanceUntilIdle()

        assertTrue(cloud.called)
        assertEquals(AnalysisStage.READY, vm.state.value.stage)
        assertEquals(CLOUD_RESULT, vm.state.value.result)
    }

    @Test
    fun cloudFailureShowsProviderMessage() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher]!!
        Dispatchers.setMain(dispatcher)

        val settings = SettingsRepository(context)
        val vm = AiHudViewModel(
            analyzer = FakeCompositionAnalyzer(),
            cloudAiProvider = FakeCloudAiProvider(CloudAiOutcome.Failure(CloudAiErrorInvalidKey)),
            localAiProvider = FakeLocalAiProvider(LocalAiOutcome.Success(FAKE_RESULT)),
            settingsRepository = settings,
            cloudAiCredentials = FakeCloudAiCredentials(),
            localModelRepository = LocalModelRepository(context, LocalModelStorage(context))
        )
        vm.setAiMode(AiMode.CLOUD_AI)

        vm.startAnalysis(Bitmap.createBitmap(1, 1, ARGB_8888), 0)
        advanceUntilIdle()

        assertEquals(AnalysisStage.READY, vm.state.value.stage)
        assertEquals(CloudAiErrorInvalidKey.message, vm.state.value.result?.primaryGuidance)
    }

    @Test
    fun localModeRoutesToLocalProvider() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher]!!
        Dispatchers.setMain(dispatcher)

        val settings = SettingsRepository(context)
        val local = FakeLocalAiProvider(LocalAiOutcome.Success(LOCAL_RESULT))
        val vm = AiHudViewModel(
            analyzer = FakeCompositionAnalyzer(),
            cloudAiProvider = FakeCloudAiProvider(CloudAiOutcome.Success(FAKE_RESULT)),
            localAiProvider = local,
            settingsRepository = settings,
            cloudAiCredentials = FakeCloudAiCredentials(),
            localModelRepository = LocalModelRepository(context, LocalModelStorage(context))
        )
        vm.setAiMode(AiMode.LOCAL_AI)

        vm.startAnalysis(Bitmap.createBitmap(1, 1, ARGB_8888), 0)
        advanceUntilIdle()

        assertTrue(local.called)
        assertEquals(AnalysisStage.READY, vm.state.value.stage)
        assertEquals(LOCAL_RESULT, vm.state.value.result)
    }

    @Test
    fun smartFallsBackToLumaVisionWhenNothingAvailable() = runTest {
        val dispatcher = coroutineContext[CoroutineDispatcher]!!
        Dispatchers.setMain(dispatcher)

        val settings = SettingsRepository(context)
        val cloud = FakeCloudAiProvider(CloudAiOutcome.Success(CLOUD_RESULT))
        val local = FakeLocalAiProvider(LocalAiOutcome.Success(LOCAL_RESULT))
        val vm = AiHudViewModel(
            analyzer = FakeCompositionAnalyzer(),
            cloudAiProvider = cloud,
            localAiProvider = local,
            settingsRepository = settings,
            // No key, cloud disabled by default, no model -> falls back to Luma Vision.
            cloudAiCredentials = FakeCloudAiCredentials(keyPresent = false),
            localModelRepository = LocalModelRepository(context, LocalModelStorage(context))
        )
        vm.setAiMode(AiMode.SMART)

        vm.startAnalysis(Bitmap.createBitmap(1, 1, ARGB_8888), 0)
        advanceUntilIdle()

        assertFalse(cloud.called)
        assertFalse(local.called)
        assertEquals(FAKE_RESULT, vm.state.value.result)
    }

    private class FakeCompositionAnalyzer : CompositionAnalyzer {
        override fun analyze(frame: Bitmap, rotationDegrees: Int): Flow<AnalysisState> = flow {
            emit(AnalysisState.InProgress(AnalysisStage.DETECTING_SCENE))
            emit(AnalysisState.InProgress(AnalysisStage.FINDING_SUBJECT))
            emit(AnalysisState.InProgress(AnalysisStage.BUILDING_COMPOSITION))
            emit(AnalysisState.Ready(FAKE_RESULT))
        }
    }

    private class FakeCloudAiProvider(var outcome: CloudAiOutcome) : CloudAiProvider {
        var called = false
        override val type = CloudProviderType.GEMINI
        override suspend fun analyze(image: CloudImage, context: String?): CloudAiOutcome {
            called = true
            return outcome
        }

        override suspend fun testConnection(): ConnectionTestResult = ConnectionTestResult.Success
    }

    private class FakeLocalAiProvider(var outcome: LocalAiOutcome) : LocalAiProvider {
        var called = false
        override suspend fun analyze(image: LocalImage, context: String?): LocalAiOutcome {
            called = true
            return outcome
        }
    }

    private class FakeCloudAiCredentials(var keyPresent: Boolean = false) : CloudAiCredentials {
        override val selectedProvider = CloudProviderType.GEMINI
        override fun hasApiKey(type: CloudProviderType) = keyPresent
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

        val CLOUD_RESULT = FAKE_RESULT.copy(compositionScore = 91, primaryGuidance = "from cloud")
        val LOCAL_RESULT = FAKE_RESULT.copy(compositionScore = 87, primaryGuidance = "from local")

        val CloudAiErrorInvalidKey =
            com.lumacam.feature.ai.cloud.CloudAiError.InvalidKey
    }
}
