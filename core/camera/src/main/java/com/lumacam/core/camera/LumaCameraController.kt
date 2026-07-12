package com.lumacam.core.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.util.Log
import android.annotation.SuppressLint
import androidx.camera.core.ImageProxy
import androidx.camera.core.toBitmap
import java.util.concurrent.atomic.AtomicReference
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import com.lumacam.core.camera.film.FilmCameraEffect
import com.lumacam.core.camera.film.FilmPhotoBaker
import com.lumacam.core.camera.film.FilmSurfaceProcessor
import com.lumacam.core.common.film.FilmPreset
import com.lumacam.core.common.film.FilmPresetCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * CameraX wrapper (PRD §5): a single ProcessCameraProvider bound to the
 * LifecycleOwner, with explicit Preview / ImageCapture / VideoCapture /
 * ImageAnalysis use cases. UI state is exposed as StateFlows; the host
 * ViewModel drives it and the Compose screen renders it.
 */
@SuppressLint("MissingPermission")
@OptIn(markerClass = [ExperimentalCamera2Interop::class])
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

    /**
     * One-shot callback for an analysis-only frame grab. Set when "✨ Analyze" is
     * tapped; consumed by the next [ImageAnalysis] frame (which converts it to a
     * Bitmap and hands it back with its rotation). Cleared after a single use so we
     * never convert frames on the hot path except when explicitly requested.
     */
    private val pendingFrame = AtomicReference<((Bitmap?, Int) -> Unit)?>(null)

    private val _lensFacing = MutableStateFlow(LensFacing.BACK)
    val lensFacing: StateFlow<LensFacing> = _lensFacing.asStateFlow()

    private val _flashMode = MutableStateFlow(FlashMode.OFF)
    val flashMode: StateFlow<Int> = _flashMode.asStateFlow()

    private val _zoomState = MutableStateFlow<ZoomState?>(null)
    val zoomState: StateFlow<ZoomState?> = _zoomState.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _lastMedia = MutableStateFlow<Uri?>(null)
    val lastMedia: StateFlow<Uri?> = _lastMedia.asStateFlow()

    private val _bindingError = MutableStateFlow<String?>(null)
    val bindingError: StateFlow<String?> = _bindingError.asStateFlow()

    private val _capabilities = MutableStateFlow<CameraCapabilities?>(null)
    val capabilities: StateFlow<CameraCapabilities?> = _capabilities.asStateFlow()

    private val _manualState = MutableStateFlow(ManualCameraState())
    val manualState: StateFlow<ManualCameraState> = _manualState.asStateFlow()

    private val _availableLenses = MutableStateFlow<List<LensInfo>>(emptyList())
    val availableLenses: StateFlow<List<LensInfo>> = _availableLenses.asStateFlow()

    // ---- Film Camera Engine ----------------------------------------------------
    private val _filmPreset = MutableStateFlow(FilmPresetCatalog.default)
    val filmPreset: StateFlow<FilmPreset> = _filmPreset.asStateFlow()

    private val _previewFilterEnabled = MutableStateFlow(true)
    val previewFilterEnabled: StateFlow<Boolean> = _previewFilterEnabled.asStateFlow()

    private var filmProcessor: FilmSurfaceProcessor? = null
    private val photoBaker = FilmPhotoBaker()
    // Lazily (re)created so shutdown() can terminate them when backgrounded and a
    // later bind() still has a live executor.
    private var bakeExecutor: ExecutorService? =
        Executors.newSingleThreadExecutor()
    private var cameraExecutor: ExecutorService? =
        Executors.newSingleThreadExecutor()

    private fun bakeExecutor(): ExecutorService =
        bakeExecutor ?: Executors.newSingleThreadExecutor().also { bakeExecutor = it }

    private fun cameraExecutor(): ExecutorService =
        cameraExecutor ?: Executors.newSingleThreadExecutor().also { cameraExecutor = it }

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

        // Analysis-only frame grab (PRD §4/§5). The analyzer is always bound but is
        // a no-op unless a one-shot grab was requested via [captureAnalysisFrame];
        // only then does it convert the current frame to a Bitmap and hand it back.
        // The frame is always closed (try/finally + camera executor) so a thrown
        // analyzer never leaks an ImageProxy and stalls the camera pipeline.
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageAnalysis.setAnalyzer(cameraExecutor()) { proxy: ImageProxy ->
            try {
                pendingFrame.getAndSet(null)?.let { cb ->
                    val frame = proxy.toBitmap()
                    cb(frame, proxy.imageInfo.rotationDegrees)
                }
            } finally {
                proxy.close()
            }
        }

        enumerateLenses(provider)
        val selector = buildSelector()

        try {
            camera = if (_filmPreset.value.isIdentity) {
                filmProcessor?.release()
                filmProcessor = null
                provider.bindToLifecycle(
                    lifecycleOwner, selector, previewUC, imageCaptureUC, videoCaptureUC, imageAnalysis
                )
            } else {
                val group = UseCaseGroup.Builder()
                    .addUseCase(previewUC)
                    .addUseCase(imageCaptureUC)
                    .addUseCase(videoCaptureUC)
                    .addUseCase(imageAnalysis)
                    .addEffect(buildFilmEffect())
                    .build()
                provider.bindToLifecycle(lifecycleOwner, selector, group)
            }
            preview = previewUC
            imageCapture = imageCaptureUC
            videoCapture = videoCaptureUC
            syncZoomFromCamera()
            probeCapabilities()
            applyManualState()
        } catch (e: Exception) {
            _bindingError.value = e.message
            Log.e(TAG, "use case binding failed", e)
        }
    }

    /**
     * Wrap the shared [FilmSurfaceProcessor] in a [CameraEffect]. VIDEO_CAPTURE is
     * always filtered (baked video); PREVIEW is filtered only when live filtering is
     * enabled (off by default on low-tier devices). Still photos are baked separately
     * by [FilmPhotoBaker], so IMAGE_CAPTURE is never targeted.
     */
    private fun buildFilmEffect(): FilmCameraEffect {
        val processor = ensureFilmProcessor()
        processor.currentPreset = _filmPreset.value
        var targets = CameraEffect.VIDEO_CAPTURE
        if (_previewFilterEnabled.value) targets = targets or CameraEffect.PREVIEW
        val errorListener = Consumer<Throwable> { t ->
            Log.e(TAG, "film effect error", t)
            _bindingError.value = t.message
        }
        return FilmCameraEffect(processor, targets, errorListener)
    }

    private fun ensureFilmProcessor(): FilmSurfaceProcessor =
        filmProcessor ?: FilmSurfaceProcessor().also {
            it.currentPreset = _filmPreset.value
            filmProcessor = it
        }

    /**
     * Select the active film preset. Updates the live GL processor immediately;
     * rebinds only when crossing the identity boundary (attaching/detaching the
     * effect from the CameraX pipeline).
     */
    fun setFilmPreset(preset: FilmPreset) {
        val previous = _filmPreset.value
        _filmPreset.value = preset
        filmProcessor?.currentPreset = preset
        if (previous.isIdentity != preset.isIdentity) bindUseCases()
    }

    /** Toggle live-preview filtering (capture is always full-quality). Rebinds. */
    fun setPreviewFilterEnabled(enabled: Boolean) {
        if (_previewFilterEnabled.value == enabled) return
        _previewFilterEnabled.value = enabled
        if (!_filmPreset.value.isIdentity) bindUseCases()
    }

    /** Build a selector for the current facing, honouring a chosen back lens id. */
    private fun buildSelector(): CameraSelector {
        val facing = if (_lensFacing.value == LensFacing.BACK)
            CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
        val builder = CameraSelector.Builder().requireLensFacing(facing)
        val id = _manualState.value.selectedLensId
        if (facing == CameraSelector.LENS_FACING_BACK && id != null) {
            builder.addCameraFilter { infos ->
                infos.filter { safeCameraId(it) == id }.ifEmpty { infos }
            }
        }
        return builder.build()
    }

    private fun enumerateLenses(provider: ProcessCameraProvider) {
        val facing = if (_lensFacing.value == LensFacing.BACK)
            CameraMetadata.LENS_FACING_BACK else CameraMetadata.LENS_FACING_FRONT
        val infos = try { provider.availableCameraInfos } catch (e: Exception) { emptyList<CameraInfo>() }
        val matching = infos.filter { lensFacingOf(it) == facing }
        val focals = matching.map { minFocalOf(it) ?: 0f }
        val types = classifyLenses(focals)
        _availableLenses.value = matching.mapIndexed { i, info ->
            LensInfo(
                id = safeCameraId(info) ?: i.toString(),
                type = types.getOrElse(i) { LensType.WIDE },
                minFocalLength = focals[i]
            )
        }
    }

    private fun probeCapabilities() {
        val cam = camera ?: return
        val c2 = try { Camera2CameraInfo.from(cam.cameraInfo) } catch (e: Exception) { null } ?: return
        fun <T> ch(key: CameraCharacteristics.Key<T>): T? =
            try { c2.getCameraCharacteristic(key) } catch (e: Exception) { null }

        val isoRange = ch(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)?.let { it.lower..it.upper }
        val expRange = ch(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)?.let { it.lower..it.upper }
        val minFocus = ch(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
        val requestCaps = ch(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.toList() ?: emptyList()
        val manualSensor = requestCaps.contains(
            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR
        )
        val awbModes = ch(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)?.toList() ?: emptyList()
        val supportedWb = WhiteBalanceMode.entries
            .filter { awbModes.contains(it.awbMode) }
            .ifEmpty { listOf(WhiteBalanceMode.AUTO) }
        val aeLock = ch(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE) ?: false
        val sceneModes = ch(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES)?.toList() ?: emptyList()
        val hdr = sceneModes.contains(CameraMetadata.CONTROL_SCENE_MODE_HDR)

        val exposureState = cam.cameraInfo.exposureState
        val evSupported = exposureState.isExposureCompensationSupported
        val evRange = if (evSupported) {
            exposureState.exposureCompensationRange.let { it.lower..it.upper }
        } else {
            0..0
        }
        val evStep = if (evSupported) exposureState.exposureCompensationStep.toFloat() else 0f

        _capabilities.value = CameraCapabilities(
            isoRange = if (manualSensor) isoRange else null,
            exposureTimeRange = if (manualSensor) expRange else null,
            minFocusDistance = minFocus,
            exposureCompensationRange = evRange,
            exposureCompensationStep = evStep,
            supportedWhiteBalance = supportedWb,
            supportsManualSensor = manualSensor && isoRange != null && expRange != null,
            supportsManualFocus = (minFocus ?: 0f) > 0f,
            supportsExposureLock = aeLock,
            supportsExposureCompensation = evSupported,
            hdrSupported = hdr,
            hasMultipleLenses = _availableLenses.value.size > 1
        )
    }

    private fun lensFacingOf(info: CameraInfo): Int? = try {
        Camera2CameraInfo.from(info).getCameraCharacteristic(CameraCharacteristics.LENS_FACING)
    } catch (e: Exception) {
        null
    }

    private fun minFocalOf(info: CameraInfo): Float? = try {
        Camera2CameraInfo.from(info)
            .getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            ?.minOrNull()
    } catch (e: Exception) {
        null
    }

    private fun safeCameraId(info: CameraInfo): String? = try {
        Camera2CameraInfo.from(info).cameraId
    } catch (e: Exception) {
        null
    }

    private fun syncZoomFromCamera() {
        val z = camera?.cameraInfo?.zoomState?.value ?: return
        _zoomState.value = ZoomState(z.zoomRatio, z.minZoomRatio, z.maxZoomRatio)
    }

    @SuppressLint("MissingPermission")
    fun capturePhoto(onResult: (Uri?) -> Unit) {
        val imageCapture = imageCapture ?: return onResult(null)
        // Write the JPEG into a private temp file first (so FilmPhotoBaker can bake
        // in place), then publish it into the MediaStore Uri the Gallery indexes.
        val uri = mediaSaver.createPhotoUri()
        val tempFile = mediaSaver.newPhotoTempFile()
        val output = ImageCapture.OutputFileOptions.Builder(tempFile).build()
        imageCapture.takePicture(
            output,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    val preset = _filmPreset.value
                    if (preset.isIdentity) {
                        mediaSaver.publishPhoto(uri, tempFile)
                        _lastMedia.value = uri
                        onResult(uri)
                        return
                    }
                    // Bake the same film look into the full-res JPEG off the main thread.
                    bakeExecutor().execute {
                        try {
                            photoBaker.bake(tempFile, preset)
                        } catch (e: Exception) {
                            Log.e(TAG, "photo bake failed", e)
                        }
                        ContextCompat.getMainExecutor(context).execute {
                            mediaSaver.publishPhoto(uri, tempFile)
                            _lastMedia.value = uri
                            onResult(uri)
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    mediaSaver.discard(uri)
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
        // Video is written directly into its MediaStore Uri (the live GL/CameraEffect
        // pipeline already bakes any film preset during capture — no post-pass needed,
        // unlike photos). The row stays pending until finalize().
        val (uri, values) = mediaSaver.createVideoUri()
        val outputOptions = MediaStoreOutputOptions.Builder(context.contentResolver, uri)
            .setContentValues(values)
            .build()
        val pending = videoCapture.output.prepareRecording(context, outputOptions)
        if (useAudio) pending.withAudioEnabled()
        recording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
            if (event is VideoRecordEvent.Finalize) {
                if (event.hasError()) {
                    mediaSaver.discard(uri)
                    _bindingError.value = "Video error: ${event.error}"
                    _lastMedia.value = null
                } else {
                    mediaSaver.finalize(uri)
                    _lastMedia.value = uri
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

    /**
     * Requests a single in-memory frame from the live preview for AI analysis — not
     * a full photo/video capture. The next [ImageAnalysis] frame converts itself to
     * a [Bitmap] (with its rotation in degrees) and is delivered to [onFrame]. Both
     * the front and back cameras route through this same analyzer, so the grabbed
     * frame always reflects the currently-active lens.
     */
    fun captureAnalysisFrame(onFrame: (Bitmap?, Int) -> Unit) {
        pendingFrame.set(onFrame)
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
        // A back-lens id is meaningless for the front camera; clear it.
        _manualState.value = _manualState.value.copy(selectedLensId = null)
        bindUseCases()
        return next
    }

    /** Tap-to-focus at view-local coordinates. Ignored while focus is locked. */
    @SuppressLint("MissingPermission")
    fun tapToFocus(x: Float, y: Float, previewView: PreviewView): Boolean {
        val cam = camera ?: return false
        if (_manualState.value.focusLocked || _manualState.value.isManualFocus) return false
        val point = previewView.meteringPointFactory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point).build()
        cam.cameraControl.startFocusAndMetering(action)
        return true
    }

    // ---- Manual (pro) controls -------------------------------------------------

    /** Manual ISO; null restores auto-exposure. */
    fun setIso(iso: Int?) {
        val clamped = iso?.let { coerceIso(it, _capabilities.value?.isoRange) }
        _manualState.value = _manualState.value.copy(isoValue = clamped)
        applyCaptureRequestOptions()
    }

    /** Manual shutter (exposure time, nanoseconds); null restores auto-exposure. */
    fun setExposureTime(nanos: Long?) {
        val clamped = nanos?.let { coerceExposureTimeNanos(it, _capabilities.value?.exposureTimeRange) }
        _manualState.value = _manualState.value.copy(exposureTimeNanos = clamped)
        applyCaptureRequestOptions()
    }

    /** Exposure Value compensation (index into the sensor's EV range). */
    fun setExposureCompensation(index: Int) {
        val caps = _capabilities.value ?: return
        if (!caps.supportsExposureCompensation) return
        val clamped = coerceExposureCompensation(index, caps.exposureCompensationRange)
        _manualState.value = _manualState.value.copy(exposureCompensation = clamped)
        applyExposureCompensation()
    }

    fun setWhiteBalance(mode: WhiteBalanceMode) {
        _manualState.value = _manualState.value.copy(whiteBalance = mode)
        applyCaptureRequestOptions()
    }

    /** Manual focus distance in diopters (0 = infinity); null restores auto-focus. */
    fun setManualFocusDistance(distance: Float?) {
        val clamped = distance?.let {
            coerceFocusDistance(it, _capabilities.value?.minFocusDistance ?: 0f)
        }
        _manualState.value = _manualState.value.copy(focusDistance = clamped)
        applyCaptureRequestOptions()
    }

    /** Lock/unlock autofocus (independent of exposure lock). */
    fun setFocusLocked(locked: Boolean) {
        _manualState.value = _manualState.value.copy(focusLocked = locked)
        applyFocusLock()
    }

    /** Lock/unlock auto-exposure (standard AE lock, independent of focus lock). */
    fun setExposureLocked(locked: Boolean) {
        _manualState.value = _manualState.value.copy(exposureLocked = locked)
        applyCaptureRequestOptions()
    }

    fun setHdrEnabled(enabled: Boolean) {
        _manualState.value = _manualState.value.copy(hdrEnabled = enabled)
        applyCaptureRequestOptions()
    }

    /** Switch to a specific back lens (from [availableLenses]); triggers a rebind. */
    fun selectLens(lensId: String) {
        if (_manualState.value.selectedLensId == lensId) return
        _manualState.value = _manualState.value.copy(selectedLensId = lensId)
        bindUseCases()
    }

    private fun applyManualState() {
        applyExposureCompensation()
        applyCaptureRequestOptions()
        applyFocusLock()
    }

    private fun applyExposureCompensation() {
        val cam = camera ?: return
        val caps = _capabilities.value ?: return
        if (!caps.supportsExposureCompensation) return
        try {
            cam.cameraControl.setExposureCompensationIndex(_manualState.value.exposureCompensation)
        } catch (e: Exception) {
            Log.w(TAG, "exposure compensation failed", e)
        }
    }

    private fun applyCaptureRequestOptions() {
        val cam = camera ?: return
        val caps = _capabilities.value ?: return
        val state = _manualState.value
        try {
            val builder = CaptureRequestOptions.Builder()

            if (caps.supportedWhiteBalance.contains(state.whiteBalance)) {
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AWB_MODE, state.whiteBalance.awbMode
                )
            }

            if (state.isManualExposure && caps.supportsManualSensor) {
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF
                )
                builder.setCaptureRequestOption(
                    CaptureRequest.SENSOR_SENSITIVITY,
                    state.isoValue ?: defaultIso(caps.isoRange)
                )
                builder.setCaptureRequestOption(
                    CaptureRequest.SENSOR_EXPOSURE_TIME,
                    state.exposureTimeNanos ?: defaultExposureTimeNanos(caps.exposureTimeRange)
                )
            } else {
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON
                )
                if (caps.supportsExposureLock) {
                    builder.setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_LOCK, state.exposureLocked
                    )
                }
            }

            if (state.isManualFocus && caps.supportsManualFocus) {
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF
                )
                builder.setCaptureRequestOption(
                    CaptureRequest.LENS_FOCUS_DISTANCE, state.focusDistance ?: 0f
                )
            }

            if (state.hdrEnabled && caps.hdrSupported) {
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_USE_SCENE_MODE
                )
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_SCENE_MODE, CameraMetadata.CONTROL_SCENE_MODE_HDR
                )
            }

            Camera2CameraControl.from(cam.cameraControl)
                .setCaptureRequestOptions(builder.build())
        } catch (e: Exception) {
            Log.w(TAG, "applying manual capture options failed", e)
        }
    }

    private fun applyFocusLock() {
        val cam = camera ?: return
        try {
            if (_manualState.value.focusLocked) {
                val pv = boundPreviewView
                val point = if (pv != null && pv.width > 0 && pv.height > 0) {
                    pv.meteringPointFactory.createPoint(pv.width / 2f, pv.height / 2f)
                } else {
                    SurfaceOrientedMeteringPointFactory(1f, 1f).createPoint(0.5f, 0.5f)
                }
                val action = FocusMeteringAction.Builder(point)
                    .disableAutoCancel()
                    .build()
                cam.cameraControl.startFocusAndMetering(action)
            } else {
                cam.cameraControl.cancelFocusAndMetering()
            }
        } catch (e: Exception) {
            Log.w(TAG, "focus lock failed", e)
        }
    }

    fun unbind() {
        recording?.stop()
        recording = null
        cameraProvider?.unbindAll()
        camera = null
        filmProcessor?.release()
        filmProcessor = null
    }

    /**
     * Full teardown used when the camera is backgrounded (ON_STOP) or the screen is
     * disposed. Releases the film GL thread/EGL context and shuts the single-thread
     * executors so nothing keeps running while the app is in the background
     * (battery). Safe to call repeatedly.
     */
    fun shutdown() {
        unbind()
        try {
            bakeExecutor?.shutdown()
        } catch (_: Exception) {
        }
        bakeExecutor = null
        try {
            cameraExecutor?.shutdown()
        } catch (_: Exception) {
        }
        cameraExecutor = null
    }

    companion object {
        private const val TAG = "LumaCameraController"
    }
}
