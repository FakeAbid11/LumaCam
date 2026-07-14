package com.lumacam.app.ui.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumacam.app.data.AiMode
import com.lumacam.app.data.CloudAiCredentials
import com.lumacam.app.data.LocalModelRepository
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
import com.lumacam.feature.ai.local.LocalAiOutcome
import com.lumacam.feature.ai.local.LocalAiProvider
import com.lumacam.feature.ai.local.LocalImage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives the AI guidance HUD. Routes each analysis pass to the backend selected in
 * [SettingsRepository] — the on-device Luma Vision pipeline, a Cloud AI provider,
 * or a Local AI model — and streams the staged [AnalysisState] the HUD renders.
 * All backends normalize to the shared [CompositionResult], so the presentation
 * layer never changes.
 */
@HiltViewModel
class AiHudViewModel @Inject constructor(
    private val analyzer: CompositionAnalyzer,
    private val cloudAiProvider: CloudAiProvider,
    private val localAiProvider: LocalAiProvider,
    private val settingsRepository: SettingsRepository,
    private val cloudAiCredentials: CloudAiCredentials,
    private val localModelRepository: LocalModelRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AiHudState())
    val state: StateFlow<AiHudState> = _state.asStateFlow()

    private var job: Job? = null

    /**
     * Direct mode override. When set, it takes precedence over the persisted
     * selection in [SettingsRepository] for the next pass — used by the UI's
     * mode menu and by tests. Null means "use the persisted preference".
     */
    private val modeOverride = MutableStateFlow<AiMode?>(null)

    fun setAiMode(mode: AiMode) {
        modeOverride.value = mode
    }

    /**
     * Live "Cloud AI enabled" preference, captured as a [StateFlow] with a
     * deterministic default so mode resolution never blocks on a DataStore read.
     */
    private val cloudAiEnabledState =
        settingsRepository.cloudAiEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * Starts a fresh analysis pass over [frame] (with [rotationDegrees]), streaming
     * stages then the final result. Cancels any in-flight pass first.
     */
    fun startAnalysis(frame: Bitmap, rotationDegrees: Int = 0) {
        job?.cancel()
        _state.value = AiHudState(active = true, stage = AnalysisStage.DETECTING_SCENE)
        job = viewModelScope.launch {
            analysisFlow(frame, rotationDegrees).collect { s ->
                _state.value = when (s) {
                    is AnalysisState.Idle -> AiHudState()
                    is AnalysisState.InProgress ->
                        _state.value.copy(active = true, stage = s.stage, result = null)
                    is AnalysisState.Ready ->
                        _state.value.copy(
                            active = true,
                            stage = AnalysisStage.READY,
                            result = s.result,
                            rawResponse = s.rawResponse
                        )
                }
            }
        }
    }

    /** Picks the backend for this pass and returns its analysis as a streamed flow. */
    private suspend fun analysisFlow(frame: Bitmap, rotationDegrees: Int): Flow<AnalysisState> {
        return when (resolveMode()) {
            AiMode.OFF -> emptyFlow()
            AiMode.LUMA_VISION, AiMode.SMART -> analyzer.analyze(frame, rotationDegrees)
            AiMode.CLOUD_AI -> cloudFlow(frame, rotationDegrees)
            AiMode.LOCAL_AI -> localFlow(frame, rotationDegrees)
        }
    }

    /**
     * Resolves the effective backend. Explicit choices are honored directly;
     * [AiMode.SMART] auto-selects Cloud AI (when enabled + keyed), then Local AI
     * (when a model is present), falling back to the offline Luma Vision pipeline.
     */
    private suspend fun resolveMode(): AiMode {
        val chosen = modeOverride.value ?: settingsRepository.aiMode.first()
        if (chosen != AiMode.SMART) return chosen
        val cloudEnabled = cloudAiEnabledState.value
        if (cloudEnabled && cloudAiCredentials.hasApiKey(cloudAiCredentials.selectedProvider)) {
            return AiMode.CLOUD_AI
        }
        if (localModelRepository.activeModel() != null) return AiMode.LOCAL_AI
        return AiMode.LUMA_VISION
    }

    private fun cloudFlow(frame: Bitmap, rotationDegrees: Int): Flow<AnalysisState> = flow {
        emit(AnalysisState.InProgress(AnalysisStage.DETECTING_SCENE))
        emit(AnalysisState.InProgress(AnalysisStage.BUILDING_COMPOSITION))
        val image = CloudImage(frame.toJpegBytes(rotationDegrees))
        when (val outcome = cloudAiProvider.analyze(image)) {
            is CloudAiOutcome.Success -> emit(AnalysisState.Ready(outcome.result))
            is CloudAiOutcome.Failure -> emit(AnalysisState.Ready(errorResult(outcome.error.message)))
        }
    }

    private fun localFlow(frame: Bitmap, rotationDegrees: Int): Flow<AnalysisState> = flow {
        emit(AnalysisState.InProgress(AnalysisStage.DETECTING_SCENE))
        emit(AnalysisState.InProgress(AnalysisStage.BUILDING_COMPOSITION))
        val image = LocalImage(frame.toJpegBytes(rotationDegrees))
        when (val outcome = localAiProvider.analyze(image)) {
            is LocalAiOutcome.Success ->
                emit(AnalysisState.Ready(outcome.result, rawResponse = outcome.rawResponse))
            is LocalAiOutcome.Failure -> emit(AnalysisState.Ready(errorResult(outcome.error.message)))
        }
    }

    /** Surfaces a provider failure through the HUD's existing caption path. */
    private fun errorResult(message: String): CompositionResult = CompositionResult(
        tiltAngle = 0f,
        compositionScore = 0,
        suggestedDirection = MoveDirection.NONE,
        sceneType = SceneType.UNKNOWN,
        lighting = LightingAssessment("", ""),
        suggestions = emptyList(),
        primaryGuidance = message
    )

    private fun Bitmap.toJpegBytes(rotationDegrees: Int): ByteArray {
        val source = if (rotationDegrees == 0) this else rotated(rotationDegrees)
        val out = ByteArrayOutputStream()
        source.compress(Bitmap.CompressFormat.JPEG, 90, out)
        if (source != this) source.recycle()
        return out.toByteArray()
    }

    private fun Bitmap.rotated(degrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    /** Clears the HUD and stops any in-flight analysis. */
    fun dismiss() {
        job?.cancel()
        job = null
        _state.value = AiHudState()
    }
}

/**
 * UI state for the HUD.
 *
 * @param active whether the HUD is engaged (analyzing or showing a result).
 * @param stage current analysis stage; null when idle.
 * @param result the completed guidance, present once [stage] reaches READY.
 */
data class AiHudState(
    val active: Boolean = false,
    val stage: AnalysisStage? = null,
    val result: CompositionResult? = null,
    /** Raw Local-AI model text, surfaced in the HUD for on-device debugging. */
    val rawResponse: String? = null
) {
    val isAnalyzing: Boolean
        get() = active && result == null && stage != null && stage != AnalysisStage.READY
}
