package com.lumacam.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.lumacam.core.ui.theme.LumaAccent
import com.lumacam.core.ui.theme.LumaBlack
import com.lumacam.core.ui.theme.LumaGray500
import com.lumacam.core.ui.theme.LumaWhite

/**
 * Settings placeholder (PRD S6). Real content — AI tiers, Gemini key, privacy —
 * is built out in later prompts. This provides the structure and navigation.
 * The "Visual effects" row is the one live, persisted setting (see
 * [SettingsContentViewModel]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenCloudAi: () -> Unit = {},
    onOpenLocalAi: () -> Unit = {},
    onOpenDeviceBenchmark: () -> Unit = {},
    contentViewModel: SettingsContentViewModel = hiltViewModel()
) {
    val effectsEnabled by contentViewModel.visualEffectsEnabled.collectAsState(
        initialValue = null
    )
    val effectsOn = effectsEnabled ?: contentViewModel.defaultEnabled()
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
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(inner)
        ) {
            // Center the content and cap its width so settings don't stretch
            // awkwardly across tablets / wide screens.
            val listModifier = if (maxWidth > 600.dp) {
                Modifier.widthIn(max = 600.dp).fillMaxWidth()
            } else {
                Modifier.fillMaxWidth()
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LazyColumn(
                    modifier = listModifier,
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
                        SettingsSwitchRow(
                            icon = Icons.Filled.AutoAwesome,
                            title = "Visual effects",
                            subtitle = if (effectsOn) "On" else "Off (calmer on low-end devices)",
                            checked = effectsOn,
                            onCheckedChange = { contentViewModel.setVisualEffects(it) }
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

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = LumaWhite)
        Column(
            Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = LumaWhite)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = LumaGray500)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = LumaAccent,
                checkedTrackColor = LumaAccent.copy(alpha = 0.5f)
            )
        )
    }
}
