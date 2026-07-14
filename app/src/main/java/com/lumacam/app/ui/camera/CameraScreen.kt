package com.lumacam.app.ui.camera

import android.content.Context
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
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import android.net.Uri
import com.lumacam.app.navigation.Routes
import com.lumacam.app.ui.camera.hud.AiAssistantSheetContent
import com.lumacam.app.ui.camera.hud.AimPointOverlay
import com.lumacam.app.ui.camera.hud.AnalyzingOverlay
import com.lumacam.app.ui.camera.hud.CompositionGridOverlay
import com.lumacam.app.ui.camera.hud.DirectionalArrowOverlay
import com.lumacam.app.ui.camera.hud.GhostCropOverlay
import com.lumacam.app.ui.camera.hud.GuidanceCaption
import com.lumacam.app.ui.camera.hud.HorizonOverlay
import com.lumacam.app.ui.camera.hud.RecommendedActionButton
import com.lumacam.app.ui.camera.hud.SubjectLockBadge
import com.lumacam.core.camera.FlashMode
import com.lumacam.app.data.AiMode
import com.lumacam.core.ui.components.GradientIcon
import com.lumacam.core.ui.components.LumaBottomSheet
import com.lumacam.core.ui.components.LumaTopAppBar
import com.lumacam.core.ui.components.LumaPill
import com.lumacam.core.ui.theme.LumaAccent
import com.lumacam.core.ui.theme.LumaColors
import com.lumacam.core.ui.theme.LumaShapes
import com.lumacam.core.ui.theme.LumaSpacing
import com.lumacam.core.ui.theme.LumaWhite
import com.lumacam.feature.ai.RecommendedAction
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
    val lowEndMode by viewModel.lowEndMode.collectAsState()
    val hudState by hudViewModel.state.collectAsState()
    val aiMode by viewModel.aiMode.collectAsState()
    val cloudAiAvailable by viewModel.cloudAiAvailable.collectAsState()
    val localAiAvailable by viewModel.localAiAvailable.collectAsState()

    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var fullScreenMedia by remember { mutableStateOf<android.net.Uri?>(null) }
    var showPro by remember { mutableStateOf(false) }
    var gridEnabled by remember { mutableStateOf(false) }
    var showAiSheet by remember { mutableStateOf(false) }
    var captureMode by remember { mutableStateOf(CaptureMode.PHOTO) }
    // Staged-reveal flags: scan -> aim point -> subject lock -> caption -> action.
    var revealAim by remember { mutableStateOf(false) }
    var revealLock by remember { mutableStateOf(false) }
    var revealCaption by remember { mutableStateOf(false) }
    var revealAction by remember { mutableStateOf(false) }
    var captureKey by remember { mutableIntStateOf(0) }
    // MutableState so changing it only recomposes the chrome, not the whole tree.
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

    // Each interaction restarts the auto-hide timer. Using a MutableState for
    // controlsVisible means only the chrome recomposes, not the preview/HUD.
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
            // Stage the reveal so the HUD unfolds step by step: scan card (already
            // shown by AnalyzingOverlay) -> aim point -> subject lock -> caption ->
            // one-tap action.
            revealAim = true
            delay(350)
            revealLock = true
            delay(350)
            revealCaption = true
            delay(350)
            revealAction = true
        } else {
            revealAim = false
            revealLock = false
            revealCaption = false
            revealAction = false
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

    // Release camera + film GL resources fully when backgrounded (ON_STOP) so no
    // GL thread / EGL context / executor keeps running in the background (battery).
    // Re-bind on resume when the preview is available. (Fold/unfold config changes
    // also recreate the activity, so camera state is restored from Settings.)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.shutdown()
                Lifecycle.Event.ON_RESUME -> {
                    previewView?.let { viewModel.bind(it, lifecycleOwner) }
                    viewModel.refreshAiAvailability()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(Modifier.fillMaxSize().background(LumaColors.chromeBlack)) {
        // Floating preview card on solid black chrome (Doka-Cam style): the
        // camera surface is a rounded rectangle inset on black, not edge-to-edge.
        Box(
            Modifier
                .fillMaxSize()
                .padding(LumaSpacing.md)
                .clip(LumaShapes.extraLarge)
                .border(1.dp, LumaColors.chromeBorder, LumaShapes.extraLarge)
                .background(LumaColors.chromeBlack)
        ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { previewViewState.value = it }
            },
            modifier = Modifier.fillMaxSize()
        )

        CaptureFlashOverlay(captureFlash, lowEndMode)

        focusPoint?.let { pt ->
            Box(
                Modifier
                    .offset { IntOffset((pt.x - 40).toInt(), (pt.y - 40).toInt()) }
                    .size(80.dp)
                    .border(1.5.dp, LumaWhite, CircleShape)
            )
        }

        // AI guidance HUD (all driven by the single CompositionResult).
        if (gridEnabled) {
            CompositionGridOverlay()
        }
        if (aiMode != AiMode.OFF) {
            hudState.result?.let { result ->
                HorizonOverlay(tiltAngle = result.tiltAngle, isLevel = result.isLevel)
                result.targetCrop?.let { GhostCropOverlay(bounds = it) }
                DirectionalArrowOverlay(
                    direction = result.suggestedDirection,
                    reducedMotion = lowEndMode
                )
                result.subjectPoint?.let { point ->
                    AnimatedVisibility(
                        visible = revealAim,
                        enter = fadeIn(animationSpec = tween(300))
                    ) {
                        AimPointOverlay(point = point, reducedMotion = lowEndMode)
                    }
                    AnimatedVisibility(
                        visible = revealLock,
                        enter = fadeIn(animationSpec = tween(300))
                    ) {
                        SubjectLockBadge(point = point)
                    }
                }
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
        }
        }

        // Top chrome — fades with relevance. Uses the shared LumaTopAppBar so the
        // camera reads like the rest of the app (dark bar + title). Camera is the
        // home screen, so the back affordance is hidden.
        val onAnalyze: () -> Unit = {
            interactionTick++
            viewModel.captureAnalysisFrame { bitmap, rotation ->
                if (bitmap != null) hudViewModel.startAnalysis(bitmap, rotation)
            }
        }
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            LumaTopAppBar(
                title = "Camera",
                onBack = { },
                showBack = false,
                actions = {
                    CameraTopBarActions(
                        flashMode = flashMode,
                        showProAvailable = capabilities?.supportsAnyManualControl == true,
                        proActive = showPro,
                        gridEnabled = gridEnabled,
                        aiMode = aiMode,
                        onAiModeChange = { viewModel.setAiMode(it) },
                        cloudAiAvailable = cloudAiAvailable,
                        localAiAvailable = localAiAvailable,
                        onFlash = { cycleFlash(viewModel, flashMode); interactionTick++ },
                        onSwitchLens = { viewModel.toggleLens(); interactionTick++ },
                        onPro = { showPro = true; interactionTick++ },
                        onToggleGrid = { gridEnabled = !gridEnabled; interactionTick++ },
                        onSettings = { navController?.navigate(Routes.SETTINGS) }
                    )
                }
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
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
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
                            BoxWithConstraints(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                val sliderWidth = maxWidth.coerceAtMost(560.dp)
                                Slider(
                                    value = z.zoomRatio,
                                    onValueChange = { viewModel.setZoomRatio(it); interactionTick++ },
                                    valueRange = z.minZoomRatio..z.maxZoomRatio,
                                    colors = SliderDefaults.colors(
                                        thumbColor = LumaWhite,
                                        activeTrackColor = LumaWhite,
                                        inactiveTrackColor = LumaColors.sliderTrackInactive
                                    ),
                                    modifier = Modifier.width(sliderWidth)
                                )
                            }
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
                    uri = lastMedia,
                    onClick = { lastMedia?.let { fullScreenMedia = it } },
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                ShutterButton(
                    mode = captureMode,
                    isRecording = isRecording,
                    captureKey = captureKey,
                    scoreProgress = hudState.result?.let { it.compositionScore / 100f },
                    glow = (hudState.result?.compositionScore ?: 0) >= 100,
                    lowEndMode = lowEndMode,
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
                // Analyze sits beside the shutter (right). Circular, mirroring the
                // shutter's ring; dimmed/disabled when AI is OFF.
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(56.dp)
                        .clip(CircleShape)
                        .border(
                            1.5.dp,
                            if (aiMode == AiMode.OFF) LumaColors.chromeMuted else LumaWhite,
                            CircleShape
                        )
                        .clickable(enabled = aiMode != AiMode.OFF, onClick = onAnalyze)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (aiMode == AiMode.OFF) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            "Analyze scene",
                            tint = LumaColors.chromeMuted,
                            modifier = Modifier.size(26.dp)
                        )
                    } else {
                        GradientIcon(
                            Icons.Filled.AutoAwesome,
                            "Analyze scene",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
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

    // Staged-reveal caption + one-tap action, drawn on top of the assistant sheet
    // (which auto-opens) so the Doka-Cam-style guidance stays visible and tappable.
    hudState.result?.let { result ->
        AnimatedVisibility(
            visible = revealCaption && result.primaryGuidance != null,
            enter = fadeIn(animationSpec = tween(300))
        ) {
            result.primaryGuidance?.let { GuidanceCaption(text = it) }
        }
        AnimatedVisibility(
            visible = revealAction && result.recommendedAction != null,
            enter = fadeIn(animationSpec = tween(300))
        ) {
            result.recommendedAction?.let { action ->
                RecommendedActionButton(
                    action = action,
                    onClick = { handleAiAction(action, viewModel, haptic) }
                )
            }
        }
    }

    fullScreenMedia?.let { uri ->
        MediaViewer(uri = uri, onDismiss = { fullScreenMedia = null })
    }
}

/**
 * Dispatches the model's recommended [action] to the real camera affordances.
 * [haptic] gives tactile confirmation; zoom maps to [CameraViewModel.setZoomRatio],
 * hold-and-shoot triggers a capture, and reposition re-uses the directional arrow
 * already shown on the HUD.
 */
private fun handleAiAction(
    action: RecommendedAction,
    viewModel: CameraViewModel,
    haptic: HapticFeedback
) {
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    when (action) {
        RecommendedAction.ZOOM_IN -> {
            val z = viewModel.zoomState.value ?: return
            viewModel.setZoomRatio((z.zoomRatio * 1.4f).coerceAtMost(z.maxZoomRatio))
        }
        RecommendedAction.ZOOM_OUT -> {
            val z = viewModel.zoomState.value ?: return
            viewModel.setZoomRatio((z.zoomRatio / 1.4f).coerceAtLeast(z.minZoomRatio))
        }
        RecommendedAction.HOLD_AND_SHOOT -> viewModel.capturePhoto { }
        RecommendedAction.REPOSITION, RecommendedAction.NONE -> {}
    }
}

/**
 * Full-screen white capture flash. Reads [captureFlash] locally so the 320 ms flash
 * animation only recomposes this leaf, not the entire camera tree. Suppressed
 * entirely in [lowEndMode] (LIMITED/BRUTAL_TRUTH tiers) to keep the UI calm.
 */
@Composable
private fun CaptureFlashOverlay(
    captureFlash: Animatable<Float, *>,
    lowEndMode: Boolean
) {
    if (lowEndMode) return
    val alpha = captureFlash.value
    if (alpha > 0f) {
        Box(Modifier.fillMaxSize().background(LumaWhite.copy(alpha = alpha)))
    }
}

@Composable
private fun CameraTopBarActions(
    flashMode: Int,
    showProAvailable: Boolean,
    proActive: Boolean,
    gridEnabled: Boolean,
    aiMode: AiMode,
    onAiModeChange: (AiMode) -> Unit,
    cloudAiAvailable: Boolean,
    localAiAvailable: Boolean,
    onFlash: () -> Unit,
    onSwitchLens: () -> Unit,
    onPro: () -> Unit,
    onToggleGrid: () -> Unit,
    onSettings: () -> Unit
) {
    AiModeIndicator(
        current = aiMode,
        onSelect = onAiModeChange,
        cloudAvailable = cloudAiAvailable,
        localAvailable = localAiAvailable
    )
    Spacer(Modifier.width(4.dp))
    IconButton(onClick = onToggleGrid) {
        Icon(
            if (gridEnabled) Icons.Filled.GridOn else Icons.Filled.GridOff,
            "Grid",
            tint = LumaWhite
        )
    }
    IconButton(onClick = onFlash) {
        Icon(flashIcon(flashMode), "Flash mode", tint = LumaWhite)
    }
    IconButton(onClick = onSwitchLens) {
        Icon(Icons.Filled.Cameraswitch, "Switch camera", tint = LumaWhite)
    }
    if (showProAvailable) {
        IconButton(onClick = onPro) {
            if (proActive) {
                GradientIcon(Icons.Filled.Tune, "Pro controls", modifier = Modifier.size(24.dp))
            } else {
                Icon(Icons.Filled.Tune, "Pro controls", tint = LumaWhite)
            }
        }
    }
    IconButton(onClick = onSettings) {
        Icon(Icons.Filled.Settings, "Settings", tint = LumaWhite)
    }
}

/**
 * AI mode selector ("⚡ <mode> ▾"). Opens a dropdown to pick the analysis backend;
 * Cloud AI / Local AI entries are disabled when their prerequisites (API key /
 * downloaded model) aren't met. The chosen mode is persisted and shown here.
 */
@Composable
private fun AiModeIndicator(
    current: AiMode,
    onSelect: (AiMode) -> Unit,
    cloudAvailable: Boolean,
    localAvailable: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        LumaPill(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .padding(start = 8.dp)
                .clickable { expanded = true },
            soft = true
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GradientIcon(
                    Icons.Filled.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    current.displayName,
                    color = LumaWhite,
                    style = MaterialTheme.typography.labelLarge
                )
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = LumaWhite,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AiMode.entries.forEach { mode ->
                // Items are always selectable. The "(no key)" / "(no model)" suffix
                // signals a missing prerequisite without greying the row out (which
                // made the modes look broken); picking one without its setup surfaces
                // a clear "add key / download model in Settings" message via the HUD.
                val suffix = when (mode) {
                    AiMode.CLOUD_AI -> if (!cloudAvailable) " (no key)" else ""
                    AiMode.LOCAL_AI -> if (!localAvailable) " (no model)" else ""
                    else -> ""
                }
                DropdownMenuItem(
                    enabled = true,
                    onClick = {
                        onSelect(mode)
                        expanded = false
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            mode.displayName + suffix,
                            color = LumaWhite,
                            style = MaterialTheme.typography.bodyMedium
                        )
                            if (mode == current) {
                                Spacer(Modifier.size(4.dp))
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = LumaWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

/** Compact toggle for live-preview filtering; capture is always full-quality. */
@Composable
private fun PreviewFilterToggle(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    LumaPill(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable { onToggle(!enabled) }
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (enabled) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                contentDescription = null,
                tint = if (enabled) LumaAccent else LumaWhite,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.size(6.dp))
            Text(
                if (enabled) "Live filter on" else "Live filter off",
                color = LumaWhite,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun RecordingPill() {
    LumaPill {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).background(LumaColors.recIndicator, CircleShape))
            Spacer(Modifier.size(6.dp))
            Text("REC", color = LumaWhite, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun LockBadge(text: String) {
    LumaPill {
        Text(
            text,
            color = LumaWhite,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun GalleryThumbnail(uri: Uri?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(enabled = uri != null, onClick = onClick)
            .background(LumaColors.chromeScrim, LumaShapes.small)
            .border(1.dp, LumaColors.chromeScrimMedium, LumaShapes.small),
        contentAlignment = Alignment.Center
    ) {
        val context = LocalContext.current
        val thumb = remember(uri) { uri?.let { loadThumbnail(it, context) } }
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

private fun loadThumbnail(uri: Uri, context: Context): ImageBitmap? {
    val resolver = context.contentResolver
    val isVideo = resolver.getType(uri) == "video/mp4"
    return if (isVideo) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            retriever.frameAtTime?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    } else {
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
    }
}
