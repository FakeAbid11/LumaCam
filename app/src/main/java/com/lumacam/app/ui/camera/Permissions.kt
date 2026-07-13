package com.lumacam.app.ui.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.lumacam.core.ui.components.GradientIcon
import com.lumacam.core.ui.theme.LumaColors
import com.lumacam.core.ui.theme.LumaGray500
import com.lumacam.core.ui.theme.LumaSpacing
import com.lumacam.core.ui.theme.LumaWhite

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
        PermissionOnboarding(
            needsAudio = needsAudio,
            showSettings = showSettings,
            onPrimary = {
                if (showSettings) context.openAppSettings()
                else launcher.launch(permissions)
            }
        )
    }
}

@Composable
private fun PermissionOnboarding(
    needsAudio: Boolean,
    showSettings: Boolean,
    onPrimary: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(LumaColors.chromeBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LumaSpacing.xl, vertical = LumaSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LumaSpacing.xl)
        ) {
            // Brand
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LumaSpacing.sm)
            ) {
                Box(
                    Modifier
                        .size(72.dp)
                        .background(LumaColors.accent.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    GradientIcon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Text(
                    "LumaCam",
                    style = MaterialTheme.typography.headlineSmall,
                    color = LumaWhite,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Your camera, guided by on-device AI.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LumaGray500
                )
            }

            // Feature highlights
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(LumaSpacing.md)
            ) {
                OnboardingFeature(
                    Icons.Filled.Cameraswitch,
                    "Photos & video",
                    "Shoot in high quality with pro controls and live filters."
                )
                OnboardingFeature(
                    Icons.Filled.AutoAwesome,
                    "On-device AI",
                    "Real-time composition tips, private by design."
                )
                OnboardingFeature(
                    Icons.Filled.Lock,
                    "Private",
                    "Your media and keys never leave this device."
                )
            }

            // Call to action
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LumaSpacing.sm)
            ) {
                if (showSettings) {
                    Text(
                        "Camera access was denied. Enable it in Settings to continue.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LumaGray500,
                        textAlign = TextAlign.Center
                    )
                }
                Button(
                    onClick = onPrimary,
                    colors = ButtonDefaults.buttonColors(containerColor = LumaColors.accent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (showSettings) "Open Settings" else "Allow camera access",
                        style = MaterialTheme.typography.labelLarge,
                        color = LumaColors.onAccent
                    )
                }
                if (needsAudio) {
                    Text(
                        "Microphone is used only for video recording.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LumaGray500,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingFeature(icon: ImageVector, title: String, body: String) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LumaSpacing.md)
    ) {
        Box(
            Modifier
                .size(44.dp)
                .background(LumaColors.accent.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = LumaColors.accent, modifier = Modifier.size(22.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = LumaWhite)
            Text(body, style = MaterialTheme.typography.bodySmall, color = LumaGray500)
        }
    }
}
