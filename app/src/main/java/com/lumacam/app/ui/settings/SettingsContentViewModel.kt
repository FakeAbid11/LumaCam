package com.lumacam.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumacam.app.data.DeviceBenchmarkStore
import com.lumacam.app.data.SettingsRepository
import com.lumacam.feature.ai.benchmark.DeviceTier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Owns the "Visual effects" preference and its tier-based default. On LIMITED /
 * BRUTAL_TRUTH devices effects default to OFF (calmer UI) but the user can enable
 * them — the same "off by default, user can enable" pattern as film preview
 * filtering. Exposed here so the top-level Settings screen can toggle it directly.
 */
@HiltViewModel
class SettingsContentViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val benchmarkStore: DeviceBenchmarkStore
) : ViewModel() {

    val visualEffectsEnabled: Flow<Boolean?> = settingsRepository.visualEffectsEnabled

    fun defaultEnabled(): Boolean {
        val tier = benchmarkStore.load()?.tier ?: return true
        return tier != DeviceTier.LIMITED && tier != DeviceTier.BRUTAL_TRUTH
    }

    fun setVisualEffects(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setVisualEffects(enabled) }
    }
}
