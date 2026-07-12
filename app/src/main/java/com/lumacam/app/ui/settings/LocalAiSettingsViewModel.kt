package com.lumacam.app.ui.settings

import android.app.ActivityManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumacam.app.data.LocalModelDownloader
import com.lumacam.app.data.LocalModelRepository
import com.lumacam.feature.ai.local.DownloadState
import com.lumacam.feature.ai.local.LocalAiOutcome
import com.lumacam.feature.ai.local.LocalAiProvider
import com.lumacam.feature.ai.local.LocalImage
import com.lumacam.feature.ai.local.LocalModelSpec
import com.lumacam.feature.ai.local.formatBytes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Per-model row state for the Local AI settings list. */
data class LocalModelUiItem(
    val spec: LocalModelSpec,
    val isDownloaded: Boolean,
    val isActive: Boolean,
    val meetsRam: Boolean,
    val download: DownloadState = DownloadState.Idle
) {
    val isDownloading: Boolean get() = download is DownloadState.CheckingSpace ||
        download is DownloadState.Downloading
}

/** Debug-only "Run test analysis" lifecycle for the on-device provider path. */
sealed interface LocalDebugState {
    data object Idle : LocalDebugState
    data object Running : LocalDebugState
    data class Done(val message: String) : LocalDebugState
}

data class LocalAiUiState(
    val models: List<LocalModelUiItem> = emptyList(),
    val availableSpaceLabel: String = "",
    val deviceRamLabel: String = "",
    val hasActiveModel: Boolean = false,
    val debugState: LocalDebugState = LocalDebugState.Idle
)

@HiltViewModel
class LocalAiSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: LocalModelRepository,
    private val downloader: LocalModelDownloader,
    private val localAiProvider: LocalAiProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocalAiUiState())
    val uiState: StateFlow<LocalAiUiState> = _uiState.asStateFlow()

    private val downloadJobs = mutableMapOf<String, Job>()
    private val deviceRamMb: Int = readDeviceRamMb()

    init {
        refresh()
    }

    fun refresh() {
        val activeId = repository.activeModelId
        val items = repository.catalog.map { spec ->
            val existing = _uiState.value.models.firstOrNull { it.spec.id == spec.id }
            LocalModelUiItem(
                spec = spec,
                isDownloaded = repository.isDownloaded(spec),
                isActive = spec.id == activeId && repository.isDownloaded(spec),
                meetsRam = deviceRamMb == 0 || deviceRamMb >= spec.minRamMb,
                download = existing?.download ?: DownloadState.Idle
            )
        }
        _uiState.update {
            it.copy(
                models = items,
                availableSpaceLabel = formatBytes(repository.availableBytes()),
                deviceRamLabel = if (deviceRamMb > 0) formatBytes(deviceRamMb * 1024L * 1024L) else "",
                hasActiveModel = items.any { m -> m.isActive }
            )
        }
    }

    fun download(spec: LocalModelSpec) {
        if (downloadJobs[spec.id]?.isActive == true) return
        downloadJobs[spec.id] = viewModelScope.launch {
            downloader.download(spec).collect { state ->
                updateDownload(spec.id, state)
                if (state is DownloadState.Completed) {
                    // First successfully-downloaded model becomes active automatically.
                    if (repository.activeModelId == null) repository.select(spec)
                    refresh()
                }
            }
        }
    }

    fun cancelDownload(spec: LocalModelSpec) {
        downloadJobs.remove(spec.id)?.cancel()
        updateDownload(spec.id, DownloadState.Idle)
    }

    fun select(spec: LocalModelSpec) {
        if (!repository.isDownloaded(spec)) return
        repository.select(spec)
        refresh()
    }

    fun delete(spec: LocalModelSpec) {
        cancelDownload(spec)
        repository.delete(spec)
        updateDownload(spec.id, DownloadState.Idle)
        refresh()
    }

    /** Debug-only: exercise the on-device provider to prove it fails gracefully. */
    fun runDebugAnalysis() {
        _uiState.update { it.copy(debugState = LocalDebugState.Running) }
        viewModelScope.launch {
            val outcome = localAiProvider.analyze(sampleImage(), context = "Test frame.")
            val message = when (outcome) {
                is LocalAiOutcome.Success -> {
                    val r = outcome.result
                    "OK — ${r.sceneType.displayName}, score ${r.compositionScore}/100"
                }
                is LocalAiOutcome.Failure -> outcome.error.message
            }
            _uiState.update { it.copy(debugState = LocalDebugState.Done(message)) }
        }
    }

    private fun updateDownload(id: String, state: DownloadState) {
        _uiState.update { ui ->
            ui.copy(models = ui.models.map { if (it.spec.id == id) it.copy(download = state) else it })
        }
    }

    private fun sampleImage(): LocalImage = LocalImage(ByteArray(0), "image/jpeg")

    private fun readDeviceRamMb(): Int = runCatching {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        (info.totalMem / (1024L * 1024L)).toInt()
    }.getOrDefault(0)
}
