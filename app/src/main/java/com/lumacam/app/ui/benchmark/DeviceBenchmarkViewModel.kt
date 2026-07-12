package com.lumacam.app.ui.benchmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumacam.app.data.DeviceBenchmarkStore
import com.lumacam.app.data.DeviceCapabilityProbe
import com.lumacam.app.data.LocalModelRepository
import com.lumacam.feature.ai.benchmark.BenchmarkResult
import com.lumacam.feature.ai.benchmark.DeviceTierClassifier
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DeviceBenchmarkUiState(
    val isRunning: Boolean = false,
    val result: BenchmarkResult? = null
)

/**
 * "Test My Phone" — runs the Device AI Compatibility Benchmark (PRD §4). Probes real
 * device specs, classifies the tier, derives a recommended mode and persists the
 * result for reuse by the Local AI Model manager and (later) the Smart mode engine.
 *
 * This is an *estimated* benchmark: it reads specs only and never runs a model, so
 * [BenchmarkResult.measuredMillis] is always null for now. A measured pass will be
 * added when a real native runtime is bundled.
 */
@HiltViewModel
class DeviceBenchmarkViewModel @Inject constructor(
    private val probe: DeviceCapabilityProbe,
    private val store: DeviceBenchmarkStore,
    private val localModelRepository: LocalModelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceBenchmarkUiState(result = store.load()))
    val uiState: StateFlow<DeviceBenchmarkUiState> = _uiState.asStateFlow()

    fun runBenchmark() {
        if (_uiState.value.isRunning) return
        _uiState.update { it.copy(isRunning = true) }
        viewModelScope.launch {
            val start = System.currentTimeMillis()
            val result = withContext(Dispatchers.Default) {
                val caps = probe.probe()
                val tier = DeviceTierClassifier.classify(caps)
                val hasModel = localModelRepository.activeModel() != null
                BenchmarkResult(
                    caps = caps,
                    tier = tier,
                    recommendedMode = DeviceTierClassifier.recommendedMode(tier, hasModel),
                    measuredMillis = null,
                    elapsedMillis = System.currentTimeMillis() - start,
                    timestamp = System.currentTimeMillis()
                )
            }
            // Brief minimum so the "testing…" state is perceivable to the user.
            delay(600)
            store.save(result)
            _uiState.update { it.copy(isRunning = false, result = result) }
        }
    }
}
