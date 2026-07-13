package com.lumacam.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lumacam.app.BuildConfig
import com.lumacam.core.ui.components.LumaSettingCard
import com.lumacam.core.ui.components.LumaSettingScaffold
import com.lumacam.core.ui.theme.LumaAccent
import com.lumacam.core.ui.theme.LumaGray500
import com.lumacam.core.ui.theme.LumaWhite
import com.lumacam.feature.ai.cloud.CloudProviderType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudAiSettingsScreen(
    onBack: () -> Unit,
    viewModel: CloudAiSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LumaSettingScaffold(
        title = "Cloud AI",
        onBack = onBack
    ) {
        Text(
            "Cloud AI is optional. Your API key is stored encrypted on this device " +
                "and is only sent to the provider you choose.",
            style = MaterialTheme.typography.bodyMedium,
            color = LumaGray500
        )

        LumaSettingCard {
            ProviderDropdown(
                selected = state.selectedProvider,
                onSelect = viewModel::selectProvider
            )
            ApiKeyField(value = state.apiKey, onChange = viewModel::updateApiKey)
            OutlinedTextField(
                value = state.baseUrl,
                onValueChange = viewModel::updateBaseUrl,
                label = { Text("Base URL" + required(state.selectedProvider.requiresBaseUrl)) },
                placeholder = { Text(state.effectiveBaseUrlHint.ifBlank { "https://…" }) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.model,
                onValueChange = viewModel::updateModel,
                label = { Text("Model" + required(state.selectedProvider.requiresModel)) },
                placeholder = { Text(state.effectiveModelHint.ifBlank { "model id" }) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = viewModel::testConnection,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.testState !is TestConnectionState.Testing &&
                    state.testState !is TestConnectionState.TakingLong
            ) {
                Text("Test Connection")
            }
            TestStatus(state.testState)
        }

        if (BuildConfig.DEBUG) {
            LumaSettingCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "DEBUG",
                        style = MaterialTheme.typography.labelMedium,
                        color = LumaGray500
                    )
                    DebugAnalyzeSection(
                        state = state.debugState,
                        onRun = viewModel::runDebugAnalysis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderDropdown(
    selected: CloudProviderType,
    onSelect: (CloudProviderType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Provider") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            CloudProviderType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayName) },
                    onClick = {
                        onSelect(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ApiKeyField(value: String, onChange: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text("API key") },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None
        else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) "Hide key" else "Show key"
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun TestStatus(state: TestConnectionState) {
    when (state) {
        TestConnectionState.Idle -> Unit
        TestConnectionState.Testing -> StatusRow(loading = true, text = "Testing connection…")
        TestConnectionState.TakingLong ->
            StatusRow(loading = true, text = "Taking longer than expected…")
        TestConnectionState.Success ->
            StatusRow(icon = Icons.Filled.CheckCircle, tint = LumaAccent, text = "Connection OK")
        is TestConnectionState.Error ->
            StatusRow(
                icon = Icons.Filled.ErrorOutline,
                tint = MaterialTheme.colorScheme.error,
                text = state.message
            )
    }
}

@Composable
private fun DebugAnalyzeSection(state: DebugAnalyzeState, onRun: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onRun,
            modifier = Modifier.fillMaxWidth(),
            enabled = state !is DebugAnalyzeState.Running
        ) {
            Text("Run test analysis")
        }
        when (state) {
            DebugAnalyzeState.Idle -> Unit
            DebugAnalyzeState.Running -> StatusRow(loading = true, text = "Analyzing sample image…")
            is DebugAnalyzeState.Success ->
                Text(state.summary, style = MaterialTheme.typography.bodySmall, color = LumaWhite)
            is DebugAnalyzeState.Error ->
                StatusRow(
                    icon = Icons.Filled.ErrorOutline,
                    tint = MaterialTheme.colorScheme.error,
                    text = state.message
                )
        }
    }
}

@Composable
private fun StatusRow(
    loading: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    tint: androidx.compose.ui.graphics.Color = LumaWhite,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint)
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = tint,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

private fun required(flag: Boolean): String = if (flag) " *" else ""
