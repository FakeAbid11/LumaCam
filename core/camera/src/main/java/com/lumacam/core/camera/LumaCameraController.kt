package com.lumacam.core.camera

import android.content.Context
import android.util.Log
import androidx.annotation.SuppressLint
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * CameraX wrapper (PRD §5): a single ProcessCameraProvider bound to the
 * LifecycleOwner, with explicit Preview / ImageCapture / VideoCapture /
 * ImageAnalysis use cases. UI state is exposed as StateFlows; the host
 * ViewModel drives it and the Compose screen renders it.
 */
@SuppressLint("MissingPermission")
class LumaCameraController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaSaver: MediaSaver
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var boundPreviewView: PreviewView? = null
    private var boundLifecycleOwner: LifecycleOwner? = null
    private var recording: Recording? = null

    private val _lensFacing = MutableStateFlow(LensFacing.BACK)
    val lensFacing: StateFlow<LensFacing> = _lensFacing.asStateFlow()

    private val _flashMode = MutableStateFlow(FlashMode.OFF)
    val flashMode: StateFlow<Int> = _flashMode.asStateFlow()

    private val _zoomState = MutableStateFlow<ZoomState?>(null)
    val zoomState: StateFlow<ZoomState?> = _zoomState.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _lastMedia = MutableStateFlow<File?>(null)
    val lastMedia: StateFlow<File?> = _lastMedia.asStateFlow()

    private val _bindingError = MutableStateFlow<String?>(null)
    val bindingError: StateFlow<String?> = _bindingError.asStateFlow()

    @SuppressLint("MissingPermission")
    fun bind(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        boundPreviewView = previewView
        boundLifecycleOwner = lifecycleOwner
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                cameraProvider = future.get()
                bindUseCases()
            } catch (e: Exception) {
                _bindingError.value = e.message
                Log.e(TAG, "bind failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @SuppressLint("MissingPermission")
    private fun bindUseCases() {
        val provider = cameraProvider ?: return
        val previewView = boundPreviewView ?: return
        val lifecycleOwner = boundLifecycleOwner ?: return
        provider.unbindAll()

        val previewUC = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageCaptureUC = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(_flashMode.value)
            .build()

        val recorder = Recorder.Builder().build()
        val videoCaptureUC = VideoCapture.Builder(recorder).build()

        // No-op analyzer kept bound for the future AI tier (PRD §5).
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { proxy -> proxy.close() }

        val selector = CameraSelector.Builder()
            .requireLensFacing(
                if (_lensFacing.value == LensFacing.BACK)
                    CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
            )
            .build()

        try {
            camera = provider.bindToLifecycle(
                lifecycleOwner, selector, previewUC, imageCaptureUC, videoCaptureUC, imageAnalysis
            )
            preview = previewUC
            imageCapture = imageCaptureUC
            videoCapture = videoCaptureUC
            syncZoomFromCamera()
        } catch (e: Exception) {
            _bindingError.value = e.message
            Log.e(TAG, "use case binding failed", e)
        }
    }

    private fun syncZoomFromCamera() {
        val z = camera?.cameraInfo?.zoomState?.value ?: return
        _zoomState.value = ZoomState(z.zoomRatio, z.minZoomRatio, z.maxZoomRatio)
    }

    @SuppressLint("MissingPermission")
    fun capturePhoto(onResult: (File?) -> Unit) {
        val imageCapture = imageCapture ?: return onResult(null)
        val file = mediaSaver.newPhotoFile()
        val output = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(
            output,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    _lastMedia.value = file
                    onResult(file)
                }

                override fun onError(exception: ImageCaptureException) {
                    _bindingError.value = exception.message
                    Log.e(TAG, "capture failed", exception)
                    onResult(null)
                }
            }
        )
    }

    @SuppressLint("MissingPermission")
    fun startRecording(useAudio: Boolean): Boolean {
        val videoCapture = videoCapture ?: return false
        if (_isRecording.value) return false
        val file = mediaSaver.newVideoFile()
        val outputOptions = FileOutputOptions.Builder(file).build()
        val pending = videoCapture.output.prepareRecording(context, outputOptions)
        if (useAudio) pending.withAudioEnabled()
        recording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
            if (event is VideoRecordEvent.Finalize) {
                if (event.hasError()) {
                    _bindingError.value = "Video error: ${event.error}"
                    _lastMedia.value = null
                } else {
                    _lastMedia.value = file
                }
                _isRecording.value = false
            }
        }
        _isRecording.value = true
        return true
    }

    fun stopRecording() {
        recording?.stop()
        recording = null
    }

    fun setZoomRatio(ratio: Float) {
        val cam = camera ?: return
        val z = cam.cameraInfo.zoomState.value ?: return
        val clamped = ratio.coerceIn(z.minZoomRatio, z.maxZoomRatio)
        cam.cameraControl.setZoomRatio(clamped)
        _zoomState.value = ZoomState(clamped, z.minZoomRatio, z.maxZoomRatio)
    }

    fun setFlash(mode: Int) {
        _flashMode.value = mode
        imageCapture?.flashMode = mode
    }

    /** Toggle front/back; returns the new facing. Triggers a rebind. */
    fun toggleLens(): LensFacing {
        val next = if (_lensFacing.value == LensFacing.BACK) LensFacing.FRONT else LensFacing.BACK
        _lensFacing.value = next
        bindUseCases()
        return next
    }

    /** Tap-to-focus at view-local coordinates. */
    @SuppressLint("MissingPermission")
    fun tapToFocus(x: Float, y: Float, previewView: PreviewView): Boolean {
        val cam = camera ?: return false
        val point = previewView.meteringPointFactory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point).build()
        cam.cameraControl.startFocusAndMetering(action)
        return true
    }

    fun unbind() {
        recording?.stop()
        recording = null
        cameraProvider?.unbindAll()
        camera = null
    }

    companion object {
        private const val TAG = "LumaCameraController"
    }
}
