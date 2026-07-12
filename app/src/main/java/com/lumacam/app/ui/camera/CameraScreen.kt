package com.lumacam.app.ui.camera

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.lumacam.app.navigation.Routes
import com.lumacam.app.ui.camera.hud.AiAssistantSheetContent
import com.lumacam.app.ui.camera.hud.AnalyzingOverlay
import com.lumacam.app.ui.camera.hud.CompositionGridOverlay
import com.lumacam.app.ui.camera.hud.DirectionalArrowOverlay
import com.lumacam.app.ui.camera.hud.GhostCropOverlay
import com.lumacam.app.ui.camera.hud.HorizonOverlay
import com.lumacam.core.camera.FlashMode
import com.lumacam.core.ui.components.LumaBottomSheet
import com.lumacam.core.ui.theme.LumaAccent
import java.io.File
import kotlinx.coroutines.delay

private const val CONTROLS_HIDE_DELAY_MS = 4000L

@Composable
fun CameraScreen(
    navController: NavHostController? = null,
    viewModel: CameraViewModel = hiltViewModel(),
    hudViewModel: AiHudViewModel = hiltViewModel()
) {
    CameraPermissionGate(needsAudio = true) {
        CameraContent(navController, viewModel, hudViewModel)
    }
}

@Composable
private fun CameraContent(
    navController: NavHostController?,
    viewModel: CameraViewModel,
    hudViewModel: AiHudViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current

    val flashMode by viewModel.flashMode.collectAsState()
    val zoom by viewModel.zoomState.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val lastMedia by viewModel.lastMedia.collectAsState()
    val error by viewModel.bindingError.collectAsState()
    val capabilities by viewModel.capabilities.collectAsState()
    val manualState by viewModel.manualState.collectAsState()
    val lenses by viewModel.availableLenses.collectAsState()
    val filmPreset by viewModel.filmPreset.collectAsState()
    val previewFilterEnabled by viewModel.previewFilterEnabled.collectAsState()
    val hudState by hudViewModel.state.collectAsState()

    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var fullScreenMedia by remember { mutableStateOf<File?>(null) }
    var showPro by remember { mutableStateOf(false) }
    var gridEnabled by remember { mutableStateOf(false) }
    var showAiSheet by remember { mutableStateOf(false) }
    var captureMode by remember { mutableStateOf(CaptureMode.PHOTO) }
    var captureKey by remember { mutableIntStateOf(0) }
    var controlsVisible by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableIntStateOf(0) }
    val previewViewState = remember { mutableStateOf<PreviewView?>(null) }
    val previewView = previewViewState.value

    val captureFlash = remember { Animatable(0f) }

    LaunchedEffect(focusPoint) {
        if (focusPoint != null) {
            delay(1000)
            focusPoint = null
        }
    }

    LaunchedEffect(interactionTick) {
        controlsVisible = true
        delay(CONTROLS_HIDE_DELAY_MS)
        controlsVisible = false
    }

    LaunchedEffect(captureKey) {
        if (captureKey > 0) {
            captureFlash.snapTo(0.85f)
            captureFlash.animateTo(0f, animationSpec = androidx.compose.animation.core.tween(320))
        }
    }

    // Auto-open the AI Assistant sheet + fire the "perfect frame" haptic once the
    // analysis is ready. Gated on the result so both mock (now) and real data
    // (Prompt 6/7) trigger identically.
    LaunchedEffect(hudState.result) {
        val result = hudState.result
        if (result != null) {
            showPro = false
            showAiSheet = true
            if (result.compositionScore >= 100) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    DisposableEffect(previewView, lifecycleOwner) {
        val pv = previewView ?: return@DisposableEffect onDispose {}
        viewModel.bind(pv, lifecycleOwner)

        val scaleDetector = ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val z = viewModel.zoomState.value ?: return true
                    viewModel.setZoomRatio(z.zoomRatio * detector.scaleFactor)
                    interactionTick++
                    return true
                }
            }
        )
        val gestureDetector = GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    focusPoint = Offset(e.x, e.y)
                    viewModel.tapToFocus(e.x, e.y, pv)
                    interactionTick++
                    return true
                }

                override fun onLongPress(e: MotionEvent) {
                    viewModel.setFocusLocked(!viewModel.manualState.value.focusLocked)
                }
            }
        )
        pv.setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)
            true
        }
        onDispose {
            pv.setOnTouchListener(null)
            viewModel.unbind()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { previewViewState.value = it }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (captureFlash.value > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = captureFlash.value))
            )
        }

        focusPoint?.let { pt ->
            Box(
                Modifier
                    .offset { IntOffset((pt.x - 40).toInt(), (pt.y - 40).toInt()) }
                    .size(80.dp)
                    .border(1.5.dp, Color.White, CircleShape)
            )
        }

        // AI guidance HUD (all driven by the single CompositionResult).
        if (gridEnabled) {
            CompositionGridOverlay()
        }
        hudState.result?.let { result ->
            HorizonOverlay(tiltAngle = result.tiltAngle, isLevel = result.isLevel)
            result.targetCrop?.let { GhostCropOverlay(bounds = it) }
            DirectionalArrowOverlay(direction = result.suggestedDirection)
        }
        hudState.stage?.let { stage ->
            AnimatedVisibility(
                visible = hudState.isAnalyzing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                AnalyzingOverlay(current = stage)
            }
        }

        // Top chrome — fades with relevance.
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopBar(
                flashMode = flashMode,
                showProAvailable = capabilities?.supportsAnyManualControl == true,
                proActive = showPro,
                gridEnabled = gridEnabled,
                onFlash = { cycleFlash(viewModel, flashMode); interactionTick++ },
                onSwitchLens = { viewModel.toggleLens(); interactionTick++ },
                onPro = { showPro = true; interactionTick++ },
                onToggleGrid = { gridEnabled = !gridEnabled; interactionTick++ },
                onDemo = { hudViewModel.startAnalysis(); interactionTick++ },
                onSettings = { navController?.navigate(Routes.SETTINGS) }
            )
        }

        // Recording indicator + AE/AF lock badges.
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isRecording) RecordingPill()
            if (manualState.exposureLocked || manualState.focusLocked) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (manualState.exposureLocked) LockBadge("AE-L")
                    if (manualState.focusLocked) LockBadge("AF-L")
                }
            }
            error?.let {
                Text(it, color = Color(0xFFFF6B6B), fontSize = 13.sp)
            }
        }

        // Bottom controls.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(visible = controlsVisible, enter = fadeIn(), exit = fadeOut()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    zoom?.let { z ->
                        if (z.maxZoomRatio > z.minZoomRatio) {
                            Slider(
                                value = z.zoomRatio,
                                onValueChange = { viewModel.setZoomRatio(it); interactionTick++ },
                                valueRange = z.minZoomRatio..z.maxZoomRatio,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color(0x66FFFFFF)
                                ),
                                modifier = Modifier.width(280.dp)
                            )
                        }
                    }
                    Spacer(Modifier.size(8.dp))
                    FilmPresetStrip(
                        presets = viewModel.filmPresets,
                        selected = filmPreset,
                        onSelect = { viewModel.setFilmPreset(it); interactionTick++ },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!filmPreset.isIdentity) {
                        Spacer(Modifier.size(8.dp))
                        PreviewFilterToggle(
                            enabled = previewFilterEnabled,
                            onToggle = { viewModel.setPreviewFilterEnabled(it); interactionTick++ }
                        )
                    }
                    Spacer(Modifier.size(12.dp))
                    ModeSwitcher(
                        mode = captureMode,
                        onModeChange = { if (!isRecording) { captureMode = it; interactionTick++ } },
                        enabled = !isRecording
                    )
                    Spacer(Modifier.size(16.dp))
                }
            }

            Box(Modifier.fillMaxWidth().padding(horizontal = 28.dp)) {
                GalleryThumbnail(
                    file = lastMedia,
                    onClick = { lastMedia?.let { fullScreenMedia = it } },
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                ShutterButton(
                    mode = captureMode,
                    isRecording = isRecording,
                    captureKey = captureKey,
                    scoreProgress = hudState.result?.let { it.compositionScore / 100f },
                    glow = (hudState.result?.compositionScore ?: 0) >= 100,
                    onClick = {
                        interactionTick++
                        when (captureMode) {
                            CaptureMode.PHOTO -> {
                                captureKey++
                                viewModel.capturePhoto { }
                            }
                            CaptureMode.VIDEO -> {
                                if (isRecording) viewModel.stopRecording()
                                else viewModel.startRecording(true)
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    val caps = capabilities
    if (showPro && caps != null && caps.supportsAnyManualControl) {
        LumaBottomSheet(
            onDismiss = { showPro = false },
            title = "Pro controls"
        ) {
            ProControlsContent(
                capabilities = caps,
                manualState = manualState,
                lenses = lenses,
                onIso = viewModel::setIso,
                onExposureTime = viewModel::setExposureTime,
                onExposureCompensation = viewModel::setExposureCompensation,
                onWhiteBalance = viewModel::setWhiteBalance,
                onFocusDistance = viewModel::setManualFocusDistance,
                onExposureLock = viewModel::setExposureLocked,
                onFocusLock = viewModel::setFocusLocked,
                onHdr = viewModel::setHdrEnabled,
                onSelectLens = viewModel::selectLens
            )
        }
    }

    val aiResult = hudState.result
    if (showAiSheet && aiResult != null) {
        LumaBottomSheet(
            onDismiss = { showAiSheet = false },
            title = "AI Assistant"
        ) {
            AiAssistantSheetContent(result = aiResult)
        }
    }

    fullScreenMedia?.let { file ->
        MediaViewer(file = file, onDismiss = { fullScreenMedia = null })
    }
}

@Composable
private fun TopBar(
    flashMode: Int,
    showProAvailable: Boolean,
    proActive: Boolean,
    gridEnabled: Boolean,
    onFlash: () -> Unit,
    onSwitchLens: () -> Unit,
    onPro: () -> Unit,
    onToggleGrid: () -> Unit,
    onDemo: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AiModeIndicator()
        Spacer(Modifier.weight(1f))
        // Demo trigger for the AI HUD (Prompt 5). Real analysis wires in Prompt 6/7.
        IconButton(onClick = onDemo) {
            Icon(Icons.Filled.AutoAwesome, "Analyze scene", tint = LumaAccent)
        }
        IconButton(onClick = onToggleGrid) {
            Icon(
                if (gridEnabled) Icons.Filled.GridOn else Icons.Filled.GridOff,
                "Grid",
                tint = Color.White
            )
        }
        IconButton(onClick = onFlash) {
            Icon(flashIcon(flashMode), "Flash mode", tint = Color.White)
        }
        IconButton(onClick = onSwitchLens) {
            Icon(Icons.Filled.Cameraswitch, "Switch camera", tint = Color.White)
        }
        if (showProAvailable) {
            IconButton(onClick = onPro) {
                Icon(
                    Icons.Filled.Tune,
                    "Pro controls",
                    tint = if (proActive) LumaAccent else Color.White
                )
            }
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Filled.Settings, "Settings", tint = Color.White)
        }
    }
}

/** Non-functional AI mode indicator ("⚡ Smart ▾"); dropdown wired in Prompt 10. */
@Composable
private fun AiModeIndicator() {
    Row(
        modifier = Modifier
            .padding(start = 8.dp)
            .background(Color(0x33000000), RoundedCornerShape(50))
            .clickable { /* AI mode selector — Prompt 10 */ }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Bolt,
            contentDescription = null,
            tint = LumaAccent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.size(4.dp))
        Text("Smart", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Icon(
            Icons.Filled.ArrowDropDown,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** Compact toggle for live-preview filtering; capture is always full-quality. */
@Composable
private fun PreviewFilterToggle(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .background(Color(0x66000000), RoundedCornerShape(50))
            .clickable { onToggle(!enabled) }
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (enabled) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
            contentDescription = null,
            tint = if (enabled) LumaAccent else Color.White,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.size(6.dp))
        Text(
            if (enabled) "Live filter on" else "Live filter off",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RecordingPill() {
    Row(
        modifier = Modifier
            .background(Color(0x66000000), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).background(Color(0xFFFF3B30), CircleShape))
        Spacer(Modifier.size(6.dp))
        Text("REC", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LockBadge(text: String) {
    Text(
        text,
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .background(Color(0x66000000), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun GalleryThumbnail(file: File?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(enabled = file != null, onClick = onClick)
            .background(Color(0x33FFFFFF), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        val thumb = remember(file) { file?.let { loadThumbnail(it) } }
        thumb?.let {
            Image(
                bitmap = it,
                contentDescription = "Last capture",
                modifier = Modifier.fillMaxSize().padding(1.dp)
            )
        }
    }
}

private fun flashIcon(mode: Int): ImageVector = when (mode) {
    FlashMode.ON -> Icons.Filled.FlashOn
    FlashMode.AUTO -> Icons.Filled.FlashAuto
    else -> Icons.Filled.FlashOff
}

private fun cycleFlash(viewModel: CameraViewModel, current: Int) {
    val seq = CameraViewModel.FLASH_SEQUENCE
    val idx = seq.indexOf(current).coerceAtLeast(0)
    viewModel.setFlash(seq[(idx + 1) % seq.size])
}

private fun loadThumbnail(file: File): ImageBitmap? {
    return if (file.extension.equals("mp4", ignoreCase = true)) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            retriever.frameAtTime?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    } else {
        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
    }
}
