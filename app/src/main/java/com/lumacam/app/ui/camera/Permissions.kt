package com.lumacam.app.ui.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

private fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    startActivity(intent)
}

@Composable
fun CameraPermissionGate(
    needsAudio: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    val permissions = remember(needsAudio) {
        if (needsAudio) arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        else arrayOf(Manifest.permission.CAMERA)
    }

    var granted by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        granted = result.values.all { it }
        showSettings = !granted &&
            permissions.any { p -> activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, p) }
    }

    LaunchedEffect(Unit) {
        granted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    if (granted) {
        content()
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("LumaCam needs camera access to take photos and videos.")
            if (showSettings) {
                Text(
                    "Permission was denied. Enable it in Settings to continue.",
                    modifier = Modifier.padding(top = 8.dp)
                )
                Button(
                    onClick = { context.openAppSettings() },
                    modifier = Modifier.padding(top = 12.dp)
                ) { Text("Open Settings") }
            } else {
                Button(
                    onClick = { launcher.launch(permissions) },
                    modifier = Modifier.padding(top = 12.dp)
                ) { Text("Grant Permission") }
            }
        }
    }
}
