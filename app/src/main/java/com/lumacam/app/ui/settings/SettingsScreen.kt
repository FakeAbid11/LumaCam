package com.lumacam.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.lumacam.core.ui.theme.LumaAccent
import com.lumacam.core.ui.theme.LumaBlack
import com.lumacam.core.ui.theme.LumaGray500
import com.lumacam.core.ui.theme.LumaWhite

/**
 * Settings placeholder (PRD S6). Real content — AI tiers, Gemini key, privacy —
 * is built out in later prompts. This provides the structure and navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenCloudAi: () -> Unit = {},
    onOpenLocalAi: () -> Unit = {},
    onOpenDeviceBenchmark: () -> Unit = {}
) {
    Scaffold(
        containerColor = LumaBlack,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = LumaWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LumaBlack,
                    titleContentColor = LumaWhite,
                    navigationIconContentColor = LumaWhite
                )
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                SettingsSection("AI") {
                    SettingsRow(
                        icon = Icons.Filled.AutoAwesome,
                        title = "Cloud AI",
                        subtitle = "Providers, API key & connection test",
                        accent = true,
                        onClick = onOpenCloudAi
                    )
                    SettingsRow(
                        icon = Icons.Filled.Memory,
                        title = "Local AI Model",
                        subtitle = "Download & run on-device vision models",
                        accent = true,
                        onClick = onOpenLocalAi
                    )
                    SettingsRow(
                        icon = Icons.Filled.Speed,
                        title = "Device AI Benchmark",
                        subtitle = "Test My Phone — can it run AI on-device?",
                        accent = true,
                        onClick = onOpenDeviceBenchmark
                    )
                }
            }
            item {
                SettingsSection("General") {
                    SettingsRow(
                        icon = Icons.Filled.Tune,
                        title = "App settings",
                        subtitle = "Capture, storage & behavior"
                    )
                }
            }
            item {
                SettingsSection("About") {
                    SettingsRow(
                        icon = Icons.Filled.Info,
                        title = "LumaCam",
                        subtitle = "Version 1.0.0"
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = LumaGray500,
            modifier = Modifier.padding(start = 20.dp, bottom = 4.dp)
        )
        content()
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (accent) LumaAccent else LumaWhite
        )
        Column(Modifier.padding(start = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = LumaWhite)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = LumaGray500)
        }
    }
}
