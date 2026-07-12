package com.lumacam.app.ui.camera

import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumacam.app.data.DeviceBenchmarkStore
import com.lumacam.app.data.SettingsRepository
import com.lumacam.core.camera.CameraCapabilities
import com.lumacam.core.camera.FlashMode
import com.lumacam.core.camera.LensFacing
import com.lumacam.core.camera.LensInfo
import com.lumacam.core.camera.LumaCameraController
import com.lumacam.core.camera.ManualCameraState
import com.lumacam.core.camera.WhiteBalanceMode
import com.lumacam.core.camera.ZoomState
import com.lumacam.core.common.film.FilmPreset
import com.lumacam.core.common.film.FilmPresetCatalog
import com.lumacam.feature.ai.benchmark.DeviceTier
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val cameraController: LumaCameraController,
    private val settingsRepository: SettingsRepository,
    private val benchmarkStore: DeviceBenchmarkStore
) : ViewModel() {

    val lensFacing: StateFlow<LensFacing> = cameraController.lensFacing
    val flashMode: StateFlow<Int> = cameraController.flashMode
    val zoomState: StateFlow<ZoomState?> = cameraController.zoomState
    val isRecording: StateFlow<Boolean> = cameraController.isRecording
    val lastMedia: StateFlow<File?> = cameraController.lastMedia
    val bindingError: StateFlow<String?> = cameraController.bindingError

    val capabilities: StateFlow<CameraCapabilities?> = cameraController.capabilities
    val manualState: StateFlow<ManualCameraState> = cameraController.manualState
    val availableLenses: StateFlow<List<LensInfo>> = cameraController.availableLenses

    // ---- Film Camera Engine ----------------------------------------------------
    val filmPresets: List<FilmPreset> = FilmPresetCatalog.presets
    val filmPreset: StateFlow<FilmPreset> = cameraController.filmPreset
    val previewFilterEnabled: StateFlow<Boolean> = cameraController.previewFilterEnabled

    /**
     * When true, non-essential visual flourishes (capture flash, shutter glow/pulse,
     * continuous arrow pulse) are suppressed to keep low-end devices smooth. Defaults
     * to the device-tier recommendation (off on LIMITED/BRUTAL_TRUTH) until the user
     * preference is loaded; the user can override it via Settings.
     */
    private val _lowEndMode = MutableStateFlow(computeLowEndForTier(benchmarkStore.load()?.tier))
    val lowEndMode: StateFlow<Boolean> = _lowEndMode.asStateFlow()

    init {
        viewModelScope.launch {
            val id = settingsRepository.filmPresetId.first()
            if (id != null) cameraController.setFilmPreset(FilmPresetCatalog.byId(id))

            val stored = settingsRepository.filmPreviewFilter.first()
            val enabled = stored ?: defaultPreviewFilterForDevice()
            cameraController.setPreviewFilterEnabled(enabled)

            val effects = settingsRepository.visualEffectsEnabled.first()
            _lowEndMode.value = effects?.let { !it }
                ?: computeLowEndForTier(benchmarkStore.load()?.tier)
        }
    }

    /** Low-tier devices default live preview filtering off (capture stays full-quality). */
    private fun defaultPreviewFilterForDevice(): Boolean = !computeLowEndForTier(benchmarkStore.load()?.tier)

    fun setVisualEffects(enabled: Boolean) {
        _lowEndMode.value = !enabled
        viewModelScope.launch { settingsRepository.setVisualEffects(enabled) }
    }

    fun setFilmPreset(preset: FilmPreset) {
        cameraController.setFilmPreset(preset)
        viewModelScope.launch { settingsRepository.setFilmPreset(preset.id) }
    }

    fun setPreviewFilterEnabled(enabled: Boolean) {
        cameraController.setPreviewFilterEnabled(enabled)
        viewModelScope.launch { settingsRepository.setFilmPreviewFilter(enabled) }
    }

    fun bind(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        cameraController.bind(previewView, lifecycleOwner)
    }

    fun unbind() = cameraController.unbind()

    fun shutdown() = cameraController.shutdown()

    fun capturePhoto(onResult: (File?) -> Unit) = cameraController.capturePhoto(onResult)

    fun startRecording(useAudio: Boolean) = cameraController.startRecording(useAudio)
    fun stopRecording() = cameraController.stopRecording()

    fun setZoomRatio(ratio: Float) = cameraController.setZoomRatio(ratio)
    fun setFlash(mode: Int) = cameraController.setFlash(mode)
    fun toggleLens(): LensFacing = cameraController.toggleLens()
    fun tapToFocus(x: Float, y: Float, previewView: PreviewView) =
        cameraController.tapToFocus(x, y, previewView)

    fun setIso(iso: Int?) = cameraController.setIso(iso)
    fun setExposureTime(nanos: Long?) = cameraController.setExposureTime(nanos)
    fun setExposureCompensation(index: Int) = cameraController.setExposureCompensation(index)
    fun setWhiteBalance(mode: WhiteBalanceMode) = cameraController.setWhiteBalance(mode)
    fun setManualFocusDistance(distance: Float?) = cameraController.setManualFocusDistance(distance)
    fun setFocusLocked(locked: Boolean) = cameraController.setFocusLocked(locked)
    fun setExposureLocked(locked: Boolean) = cameraController.setExposureLocked(locked)
    fun setHdrEnabled(enabled: Boolean) = cameraController.setHdrEnabled(enabled)
    fun selectLens(lensId: String) = cameraController.selectLens(lensId)

    companion object {
        val FLASH_SEQUENCE = listOf(FlashMode.OFF, FlashMode.ON, FlashMode.AUTO)

        /**
         * Shared tier → "reduced visuals" decision used by both the film
         * preview-filter default and the general visual-effects default, so the two
         * never produce a half-reduced, inconsistent look on LIMITED/BRUTAL_TRUTH
         * devices. Visible for tests to lock the contract.
         */
        internal fun computeLowEndForTier(tier: DeviceTier?): Boolean =
            tier == DeviceTier.LIMITED || tier == DeviceTier.BRUTAL_TRUTH
    }
}
