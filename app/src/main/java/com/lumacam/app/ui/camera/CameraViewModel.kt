package com.lumacam.app.ui.camera

import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.lumacam.core.camera.CameraCapabilities
import com.lumacam.core.camera.FlashMode
import com.lumacam.core.camera.LensFacing
import com.lumacam.core.camera.LensInfo
import com.lumacam.core.camera.LumaCameraController
import com.lumacam.core.camera.ManualCameraState
import com.lumacam.core.camera.WhiteBalanceMode
import com.lumacam.core.camera.ZoomState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val cameraController: LumaCameraController
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

    fun bind(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        cameraController.bind(previewView, lifecycleOwner)
    }

    fun unbind() = cameraController.unbind()

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
    }
}
