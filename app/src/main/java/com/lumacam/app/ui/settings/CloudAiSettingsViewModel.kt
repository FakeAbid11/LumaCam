package com.lumacam.app.ui.settings

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumacam.app.data.CloudAiKeyStore
import com.lumacam.feature.ai.CompositionResult
import com.lumacam.feature.ai.cloud.CloudAiConfig
import com.lumacam.feature.ai.cloud.CloudAiOutcome
import com.lumacam.feature.ai.cloud.CloudAiProviderFactory
import com.lumacam.feature.ai.cloud.CloudProviderType
import com.lumacam.feature.ai.cloud.ConnectionTestResult
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.lumacam.feature.ai.cloud.CloudImage

/** Connection-test lifecycle for the Settings "Test Connection" button. */
sealed interface TestConnectionState {
    data object Idle : TestConnectionState
    data object Testing : TestConnectionState
    data object TakingLong : TestConnectionState
    data object Success : TestConnectionState
    data class Error(val message: String) : TestConnectionState
}

/** Debug-only "Run test analysis" lifecycle. */
sealed interface DebugAnalyzeState {
    data object Idle : DebugAnalyzeState
    data object Running : DebugAnalyzeState
    data class Success(val summary: String) : DebugAnalyzeState
    data class Error(val message: String) : DebugAnalyzeState
}

data class CloudAiSettingsUiState(
    val selectedProvider: CloudProviderType = CloudProviderType.GEMINI,
    val apiKey: String = "",
    val baseUrl: String = "",
    val model: String = "",
    val testState: TestConnectionState = TestConnectionState.Idle,
    val debugState: DebugAnalyzeState = DebugAnalyzeState.Idle
) {
    val effectiveModelHint: String get() = selectedProvider.defaultModel
    val effectiveBaseUrlHint: String get() = selectedProvider.defaultBaseUrl
}

@HiltViewModel
class CloudAiSettingsViewModel @Inject constructor(
    private val keyStore: CloudAiKeyStore,
    private val providerFactory: CloudAiProviderFactory
) : ViewModel() {

    private val _uiState = MutableStateFlow(CloudAiSettingsUiState())
    val uiState: StateFlow<CloudAiSettingsUiState> = _uiState.asStateFlow()

    private var slowWarningJob: Job? = null

    init {
        loadProvider(keyStore.selectedProvider)
    }

    fun selectProvider(type: CloudProviderType) {
        keyStore.selectedProvider = type
        loadProvider(type)
    }

    private fun loadProvider(type: CloudProviderType) {
        _uiState.value = CloudAiSettingsUiState(
            selectedProvider = type,
            apiKey = keyStore.getApiKey(type),
            baseUrl = keyStore.getBaseUrl(type),
            model = keyStore.getModel(type)
        )
    }

    fun updateApiKey(value: String) {
        keyStore.setApiKey(_uiState.value.selectedProvider, value)
        _uiState.update { it.copy(apiKey = value, testState = TestConnectionState.Idle) }
    }

    fun updateBaseUrl(value: String) {
        keyStore.setBaseUrl(_uiState.value.selectedProvider, value)
        _uiState.update { it.copy(baseUrl = value, testState = TestConnectionState.Idle) }
    }

    fun updateModel(value: String) {
        keyStore.setModel(_uiState.value.selectedProvider, value)
        _uiState.update { it.copy(model = value, testState = TestConnectionState.Idle) }
    }

    fun testConnection() {
        val config = currentConfig()
        if (!config.isComplete) {
            _uiState.update {
                it.copy(
                    testState = TestConnectionState.Error(
                        "Add your API key" +
                            (if (config.effectiveModel.isBlank()) " and model" else "") +
                            (if (config.effectiveBaseUrl.isBlank()) " and base URL" else "") +
                            " first."
                    )
                )
            }
            return
        }
        _uiState.update { it.copy(testState = TestConnectionState.Testing) }
        startSlowWarning()
        viewModelScope.launch {
            val provider = providerFactory.create(config)
            val result = provider.testConnection()
            slowWarningJob?.cancel()
            _uiState.update {
                it.copy(
                    testState = when (result) {
                        is ConnectionTestResult.Success -> TestConnectionState.Success
                        is ConnectionTestResult.Failure ->
                            TestConnectionState.Error(result.error.message)
                    }
                )
            }
        }
    }

    /** Debug-only: analyze a small generated image to prove the full path works. */
    fun runDebugAnalysis() {
        val config = currentConfig()
        if (!config.isComplete) {
            _uiState.update {
                it.copy(debugState = DebugAnalyzeState.Error("Configure the provider first."))
            }
            return
        }
        _uiState.update { it.copy(debugState = DebugAnalyzeState.Running) }
        viewModelScope.launch {
            val provider = providerFactory.create(config)
            val outcome = provider.analyze(sampleImage(), context = "This is a test frame.")
            _uiState.update {
                it.copy(
                    debugState = when (outcome) {
                        is CloudAiOutcome.Success -> DebugAnalyzeState.Success(summarize(outcome.result))
                        is CloudAiOutcome.Failure -> DebugAnalyzeState.Error(outcome.error.message)
                    }
                )
            }
        }
    }

    private fun currentConfig(): CloudAiConfig =
        keyStore.buildConfig(_uiState.value.selectedProvider)

    private fun startSlowWarning() {
        slowWarningJob?.cancel()
        slowWarningJob = viewModelScope.launch {
            delay(CloudAiConfig.SLOW_WARNING_MILLIS)
            _uiState.update {
                if (it.testState is TestConnectionState.Testing) {
                    it.copy(testState = TestConnectionState.TakingLong)
                } else {
                    it
                }
            }
        }
    }

    private fun summarize(r: CompositionResult): String = buildString {
        appendLine("Scene: ${r.sceneType.displayName}")
        appendLine("Score: ${r.compositionScore}/100")
        appendLine("Direction: ${r.suggestedDirection}")
        appendLine("Tilt: ${r.tiltAngle}°")
        appendLine("Lighting: ${r.lighting.label}")
        if (r.suggestions.isNotEmpty()) {
            appendLine("Tips:")
            r.suggestions.forEach { appendLine("  • $it") }
        }
    }.trim()

    private fun sampleImage(): CloudImage {
        val size = 96
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(90, 120, 150)
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        paint.color = Color.rgb(235, 225, 205)
        canvas.drawCircle(size / 3f, size / 3f, size / 5f, paint)
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return CloudImage(out.toByteArray(), "image/jpeg")
    }
}
