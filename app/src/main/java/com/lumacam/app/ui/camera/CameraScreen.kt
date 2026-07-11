package com.lumacam.app.ui.camera

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.align
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.height
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.lumacam.app.navigation.Routes
import com.lumacam.core.camera.FlashMode
import java.io.File
import kotlinx.coroutines.delay

@Composable
fun CameraScreen(
    navController: NavHostController? = null,
    viewModel: CameraViewModel = hiltViewModel()
) {
    CameraPermissionGate(needsAudio = true) {
        CameraContent(navController, viewModel)
    }
}

@Composable
private fun CameraContent(navController: NavHostController?, viewModel: CameraViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val flashMode by viewModel.flashMode.collectAsState()
    val zoom by viewModel.zoomState.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val lastMedia by viewModel.lastMedia.collectAsState()
    val error by viewModel.bindingError.collectAsState()

    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var fullScreenMedia by remember { mutableStateOf<File?>(null) }
    val previewViewState = remember { mutableStateOf<PreviewView?>(null) }
    val previewView = previewViewState.value

    LaunchedEffect(focusPoint) {
        if (focusPoint != null) {
            delay(1000)
            focusPoint = null
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
                    return true
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

        focusPoint?.let { pt ->
            Box(
                Modifier
                    .offset { IntOffset((pt.x - 32).toInt(), (pt.y - 32).toInt()) }
                    .size(64.dp)
                    .border(2.dp, Color.White, CircleShape)
            )
        }

        Column(Modifier.align(Alignment.TopStart).padding(8.dp)) {
            IconButton(onClick = { viewModel.toggleLens() }) {
                Icon(Icons.Filled.Cameraswitch, "Switch camera", tint = Color.White)
            }
            IconButton(onClick = { cycleFlash(viewModel, flashMode) }) {
                Icon(flashIcon(flashMode), "Flash mode", tint = Color.White)
            }
        }

        IconButton(
            onClick = { navController?.navigate(Routes.SETTINGS) },
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        ) {
            Icon(Icons.Filled.Settings, "Settings", tint = Color.White)
        }

        error?.let {
            Text(
                it,
                color = Color.Red,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 72.dp)
            )
        }

        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            zoom?.let { z ->
                Slider(
                    value = z.zoomRatio,
                    onValueChange = { viewModel.setZoomRatio(it) },
                    valueRange = z.minZoomRatio..z.maxZoomRatio,
                    modifier = Modifier.width(300.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                lastMedia?.let { file ->
                    val thumb = remember(file) { loadThumbnail(file) }
                    Box(
                        Modifier
                            .size(48.dp)
                            .clickable { fullScreenMedia = file }
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        thumb?.let { Image(bitmap = it, contentDescription = null, Modifier.fillMaxSize()) }
                    }
                }
                IconButton(onClick = { viewModel.capturePhoto { } }) {
                    Icon(Icons.Filled.PhotoCamera, "Capture photo", tint = Color.White)
                }
                IconButton(onClick = {
                    if (isRecording) viewModel.stopRecording() else viewModel.startRecording(true)
                }) {
                    Icon(
                        if (isRecording) Icons.Filled.Stop else Icons.Filled.Videocam,
                        if (isRecording) "Stop recording" else "Record video",
                        tint = Color.White
                    )
                }
            }
        }
    }

    fullScreenMedia?.let { file ->
        MediaViewer(file = file, onDismiss = { fullScreenMedia = null })
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
