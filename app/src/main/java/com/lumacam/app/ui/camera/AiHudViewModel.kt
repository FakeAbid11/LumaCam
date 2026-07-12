package com.lumacam.app.ui.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.graphics.Bitmap
import com.lumacam.feature.ai.AnalysisStage
import com.lumacam.feature.ai.AnalysisState
import com.lumacam.feature.ai.CompositionAnalyzer
import com.lumacam.feature.ai.CompositionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives the AI guidance HUD. The injected [CompositionAnalyzer] is the real Luma
 * Vision pipeline; [startAnalysis] feeds it a frame grabbed from the live preview
 * and streams the staged [AnalysisState] the HUD renders.
 */
@HiltViewModel
class AiHudViewModel @Inject constructor(
    private val analyzer: CompositionAnalyzer
) : ViewModel() {

    private val _state = MutableStateFlow(AiHudState())
    val state: StateFlow<AiHudState> = _state.asStateFlow()

    private var job: Job? = null

    /**
     * Starts a fresh analysis pass over [frame] (with [rotationDegrees]), streaming
     * stages then the final result. Cancels any in-flight pass first.
     */
    fun startAnalysis(frame: Bitmap, rotationDegrees: Int = 0) {
        job?.cancel()
        _state.value = AiHudState(active = true, stage = AnalysisStage.DETECTING_SCENE)
        job = viewModelScope.launch {
            analyzer.analyze(frame, rotationDegrees).collect { s ->
                _state.value = when (s) {
                    is AnalysisState.Idle -> AiHudState()
                    is AnalysisState.InProgress ->
                        _state.value.copy(active = true, stage = s.stage, result = null)
                    is AnalysisState.Ready ->
                        _state.value.copy(
                            active = true,
                            stage = AnalysisStage.READY,
                            result = s.result
                        )
                }
            }
        }
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
    val result: CompositionResult? = null
) {
    val isAnalyzing: Boolean
        get() = active && result == null && stage != null && stage != AnalysisStage.READY
}
