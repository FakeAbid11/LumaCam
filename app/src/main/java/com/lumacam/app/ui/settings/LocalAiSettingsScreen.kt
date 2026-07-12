package com.lumacam.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lumacam.app.BuildConfig
import com.lumacam.core.ui.theme.LumaAccent
import com.lumacam.core.ui.theme.LumaBlack
import com.lumacam.core.ui.theme.LumaGray500
import com.lumacam.core.ui.theme.LumaWhite
import com.lumacam.feature.ai.local.DownloadState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalAiSettingsScreen(
    onBack: () -> Unit,
    viewModel: LocalAiSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = LumaBlack,
        topBar = {
            TopAppBar(
                title = { Text("Local AI Model") },
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Run analysis fully on-device — no internet, nothing leaves your phone. " +
                    "Models are downloaded once and stored privately in this app.",
                style = MaterialTheme.typography.bodyMedium,
                color = LumaGray500
            )

            val space = state.availableSpaceLabel
            val ram = state.deviceRamLabel
            if (space.isNotBlank()) {
                Text(
                    buildString {
                        append("Free storage: $space")
                        if (ram.isNotBlank()) append("  •  Device RAM: $ram")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = LumaGray500
                )
            }

            if (!state.hasActiveModel) {
                NoActiveModelNotice()
            }

            state.models.forEach { item ->
                ModelCard(
                    item = item,
                    onDownload = { viewModel.download(item.spec) },
                    onCancel = { viewModel.cancelDownload(item.spec) },
                    onSelect = { viewModel.select(item.spec) },
                    onDelete = { viewModel.delete(item.spec) }
                )
            }

            if (BuildConfig.DEBUG) {
                LocalDebugSection(state.debugState, onRun = viewModel::runDebugAnalysis)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NoActiveModelNotice() {
    Card(colors = CardDefaults.cardColors(containerColor = LumaBlack)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = LumaAccent)
            Text(
                "No on-device model yet. Download one below to use Local AI Model mode.",
                style = MaterialTheme.typography.bodyMedium,
                color = LumaWhite,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
private fun ModelCard(
    item: LocalModelUiItem,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = LumaBlack)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.isDownloaded) {
                    RadioButton(selected = item.isActive, onClick = onSelect)
                }
                Column(Modifier.weight(1f).padding(start = if (item.isDownloaded) 4.dp else 0.dp)) {
                    Text(item.spec.name, style = MaterialTheme.typography.titleMedium, color = LumaWhite)
                    Text(
                        item.spec.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = LumaGray500
                    )
                }
                if (item.isActive) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Active", tint = LumaAccent)
                }
            }

            Text(
                "${item.spec.formattedSize}  •  ${item.spec.quantization}  •  " +
                    "min RAM ${item.spec.formattedMinRam}",
                style = MaterialTheme.typography.bodySmall,
                color = LumaGray500
            )

            if (!item.meetsRam) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = LumaAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "This device may not have enough RAM for this model.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LumaAccent,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }

            when (val d = item.download) {
                is DownloadState.CheckingSpace ->
                    ProgressRow(label = "Checking storage…", fraction = null)
                is DownloadState.Downloading ->
                    ProgressRow(
                        label = d.percent?.let { "Downloading… $it%" } ?: "Downloading…",
                        fraction = d.fraction,
                        onCancel = onCancel
                    )
                is DownloadState.Failed ->
                    Text(
                        d.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                else -> Unit
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    item.isDownloading -> Unit
                    item.isDownloaded -> {
                        OutlinedButton(onClick = onDelete) {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Delete", modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                    else -> {
                        Button(onClick = onDownload) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                if (item.download is DownloadState.Failed) "Retry download" else "Download",
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressRow(label: String, fraction: Float?, onCancel: (() -> Unit)? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = LumaWhite, modifier = Modifier.weight(1f))
            if (onCancel != null) {
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        }
        if (fraction != null) {
            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun LocalDebugSection(state: LocalDebugState, onRun: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("DEBUG", style = MaterialTheme.typography.labelMedium, color = LumaGray500)
        OutlinedButton(
            onClick = onRun,
            modifier = Modifier.fillMaxWidth(),
            enabled = state !is LocalDebugState.Running
        ) {
            Text("Run test analysis")
        }
        when (state) {
            LocalDebugState.Idle -> Unit
            LocalDebugState.Running ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    Text(
                        "Running on-device analysis…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LumaWhite,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            is LocalDebugState.Done ->
                Text(
                    state.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = LumaWhite,
                    textAlign = TextAlign.Start
                )
        }
    }
}
