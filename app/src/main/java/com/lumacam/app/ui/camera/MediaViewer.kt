package com.lumacam.app.ui.camera

import android.graphics.BitmapFactory
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import java.io.File

@Composable
fun MediaViewer(file: File, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val isVideo = file.extension.equals("mp4", ignoreCase = true)

    Dialog(onDismissRequest = onDismiss) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (isVideo) {
                AndroidView(
                    factory = {
                        VideoView(it).apply {
                            setVideoPath(file.absolutePath)
                            setMediaController(MediaController(it).apply { setAnchorView(this@apply) })
                            start()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val bitmap = remember(file) { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }
                bitmap?.let {
                    Image(bitmap = it, contentDescription = null, modifier = Modifier.fillMaxSize())
                }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}
