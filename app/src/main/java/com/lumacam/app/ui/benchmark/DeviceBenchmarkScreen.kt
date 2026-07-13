package com.lumacam.app.ui.benchmark

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lumacam.core.ui.components.KeyValueRow
import com.lumacam.core.ui.components.LumaSettingCard
import com.lumacam.core.ui.components.LumaSettingScaffold
import com.lumacam.core.ui.theme.LumaAccent
import com.lumacam.core.ui.theme.LumaGray500
import com.lumacam.core.ui.theme.LumaWhite
import com.lumacam.feature.ai.benchmark.BenchmarkResult
import com.lumacam.feature.ai.benchmark.DeviceCapabilities
import com.lumacam.feature.ai.local.formatBytes

@Composable
fun DeviceBenchmarkScreen(
    onBack: () -> Unit,
    viewModel: DeviceBenchmarkViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LumaSettingScaffold(
        title = "Device AI Benchmark",
        onBack = onBack
    ) {
        Text(
            "Check how well this phone can run AI models on-device. We read your " +
                "hardware specs and give you an honest verdict — no sugar-coating.",
            style = MaterialTheme.typography.bodyMedium,
            color = LumaGray500
        )

        Button(
            onClick = viewModel::runBenchmark,
            enabled = !state.isRunning,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = LumaWhite,
                    strokeWidth = 2.dp
                )
                Text("Testing…", modifier = Modifier.padding(start = 8.dp))
            } else {
                Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    if (state.result == null) "Test My Phone" else "Test Again",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        state.result?.let { ResultSection(it) }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ResultSection(result: BenchmarkResult) {
    LumaSettingCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                result.tier.displayName.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = LumaAccent,
                fontWeight = FontWeight.Bold
            )
            Text(result.tier.headline, style = MaterialTheme.typography.titleMedium, color = LumaWhite)
            Text(result.tier.message, style = MaterialTheme.typography.bodyMedium, color = LumaGray500)
        }
    }

    LumaSettingCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Analysis speed", style = MaterialTheme.typography.titleSmall, color = LumaWhite)
            val speedLabel = result.measuredMillis?.let { "${it} ms (measured)" }
                ?: "${result.tier.estimatedTimeLabel} (estimated)"
            Text(speedLabel, style = MaterialTheme.typography.bodyMedium, color = LumaGray500)
            Text(
                "Recommended mode: ${result.recommendedMode.displayName}",
                style = MaterialTheme.typography.bodyMedium,
                color = LumaWhite
            )
        }
    }

    SpecCard(result.caps)
}

@Composable
private fun SpecCard(caps: DeviceCapabilities) {
    LumaSettingCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Device specs", style = MaterialTheme.typography.titleSmall, color = LumaWhite)
            KeyValueRow("Model", caps.deviceModel)
            KeyValueRow("Chipset", caps.hardware.ifBlank { "—" })
            KeyValueRow("Total RAM", formatBytes(caps.totalRamMb * 1024L * 1024L))
            KeyValueRow("Available RAM", formatBytes(caps.availableRamMb * 1024L * 1024L))
            KeyValueRow("CPU cores", caps.cpuCores.toString())
            KeyValueRow("Vulkan GPU", if (caps.supportsVulkan) "Yes" else "No")
            KeyValueRow("Free storage", formatBytes(caps.availableStorageBytes))
            KeyValueRow("Android API", caps.apiLevel.toString())
        }
    }
}
