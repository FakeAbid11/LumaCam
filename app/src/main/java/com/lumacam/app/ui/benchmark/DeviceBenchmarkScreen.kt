package com.lumacam.app.ui.benchmark

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lumacam.core.ui.theme.LumaAccent
import com.lumacam.core.ui.theme.LumaBlack
import com.lumacam.core.ui.theme.LumaGray500
import com.lumacam.core.ui.theme.LumaWhite
import com.lumacam.feature.ai.benchmark.BenchmarkResult
import com.lumacam.feature.ai.benchmark.DeviceCapabilities
import com.lumacam.feature.ai.local.formatBytes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceBenchmarkScreen(
    onBack: () -> Unit,
    viewModel: DeviceBenchmarkViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = LumaBlack,
        topBar = {
            TopAppBar(
                title = { Text("Device AI Benchmark") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .widthIn(max = 600.dp)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))
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
                        color = LumaBlack,
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

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ResultSection(result: BenchmarkResult) {
    Card(colors = CardDefaults.cardColors(containerColor = LumaBlack)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

    Card(colors = CardDefaults.cardColors(containerColor = LumaBlack)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    Card(colors = CardDefaults.cardColors(containerColor = LumaBlack)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Device specs", style = MaterialTheme.typography.titleSmall, color = LumaWhite)
            SpecRow("Model", caps.deviceModel)
            SpecRow("Chipset", caps.hardware.ifBlank { "—" })
            SpecRow("Total RAM", formatBytes(caps.totalRamMb * 1024L * 1024L))
            SpecRow("Available RAM", formatBytes(caps.availableRamMb * 1024L * 1024L))
            SpecRow("CPU cores", caps.cpuCores.toString())
            SpecRow("Vulkan GPU", if (caps.supportsVulkan) "Yes" else "No")
            SpecRow("Free storage", formatBytes(caps.availableStorageBytes))
            SpecRow("Android API", caps.apiLevel.toString())
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = LumaGray500, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = LumaWhite)
    }
}
