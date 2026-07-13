package com.lumacam.app.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.lumacam.core.ui.components.LumaSettingCard
import com.lumacam.core.ui.components.LumaSettingScaffold
import com.lumacam.core.ui.components.SectionHeader
import com.lumacam.core.ui.components.SettingRow
import com.lumacam.core.ui.components.SettingSwitchRow

/**
 * Settings (PRD S6). Real content — AI tiers, AI Studio key, privacy — is built out
 * in later prompts; this provides the structure and navigation. The "Visual
 * effects" row is the one live, persisted setting (see [SettingsContentViewModel]).
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenCloudAi: () -> Unit = {},
    onOpenLocalAi: () -> Unit = {},
    onOpenDeviceBenchmark: () -> Unit = {},
    contentViewModel: SettingsContentViewModel = hiltViewModel()
) {
    val effectsEnabled by contentViewModel.visualEffectsEnabled.collectAsState(
        initial = null
    )
    val effectsOn = effectsEnabled ?: contentViewModel.defaultEnabled()

    LumaSettingScaffold(
        title = "Settings",
        onBack = onBack
    ) {
        SectionHeader("AI")
        LumaSettingCard {
            SettingRow(
                icon = Icons.Filled.AutoAwesome,
                title = "Cloud AI",
                subtitle = "Providers, API key & connection test",
                accent = true,
                onClick = onOpenCloudAi
            )
            SettingRow(
                icon = Icons.Filled.Memory,
                title = "Local AI Model",
                subtitle = "Download & run on-device vision models",
                accent = true,
                onClick = onOpenLocalAi
            )
            SettingRow(
                icon = Icons.Filled.Speed,
                title = "Device AI Benchmark",
                subtitle = "Test My Phone — can it run AI on-device?",
                accent = true,
                onClick = onOpenDeviceBenchmark
            )
        }

        SectionHeader("General")
        LumaSettingCard {
            SettingRow(
                icon = Icons.Filled.Tune,
                title = "App settings",
                subtitle = "Capture, storage & behavior"
            )
            SettingSwitchRow(
                icon = Icons.Filled.AutoAwesome,
                title = "Visual effects",
                subtitle = if (effectsOn) "On" else "Off (calmer on low-end devices)",
                checked = effectsOn,
                onCheckedChange = { contentViewModel.setVisualEffects(it) }
            )
        }

        SectionHeader("About")
        LumaSettingCard {
            SettingRow(
                icon = Icons.Filled.Info,
                title = "LumaCam",
                subtitle = "Version 1.0.0"
            )
        }
    }
}
