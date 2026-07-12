package com.lumacam.app.data

import android.content.Context
import com.lumacam.feature.ai.benchmark.DeviceCapabilities
import com.lumacam.feature.ai.benchmark.DeviceTier
import com.lumacam.feature.ai.benchmark.RecommendedMode
import com.lumacam.feature.ai.benchmark.BenchmarkResult

/**
 * Persists the last Device AI Compatibility Benchmark result in plain
 * SharedPreferences (PRD §4 — reused by the Local AI Model manager and, later, the
 * Smart mode engine). No secrets involved, so no encryption needed.
 */
class DeviceBenchmarkStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(result: BenchmarkResult) {
        prefs.edit()
            .putString(KEY_DEVICE_MODEL, result.caps.deviceModel)
            .putString(KEY_HARDWARE, result.caps.hardware)
            .putInt(KEY_TOTAL_RAM, result.caps.totalRamMb)
            .putInt(KEY_AVAIL_RAM, result.caps.availableRamMb)
            .putInt(KEY_CORES, result.caps.cpuCores)
            .putBoolean(KEY_VULKAN, result.caps.supportsVulkan)
            .putLong(KEY_STORAGE, result.caps.availableStorageBytes)
            .putInt(KEY_API, result.caps.apiLevel)
            .putString(KEY_TIER, result.tier.name)
            .putString(KEY_MODE, result.recommendedMode.name)
            .putLong(KEY_MEASURED, result.measuredMillis ?: -1L)
            .putLong(KEY_TIMESTAMP, result.timestamp)
            .apply()
    }

    fun load(): BenchmarkResult? {
        if (!prefs.contains(KEY_TIER)) return null
        val tier = runCatching { DeviceTier.valueOf(prefs.getString(KEY_TIER, "")!!) }
            .getOrNull() ?: return null
        val mode = runCatching { RecommendedMode.valueOf(prefs.getString(KEY_MODE, "")!!) }
            .getOrNull() ?: RecommendedMode.LUMA_VISION
        val caps = DeviceCapabilities(
            deviceModel = prefs.getString(KEY_DEVICE_MODEL, "") ?: "",
            hardware = prefs.getString(KEY_HARDWARE, "") ?: "",
            totalRamMb = prefs.getInt(KEY_TOTAL_RAM, 0),
            availableRamMb = prefs.getInt(KEY_AVAIL_RAM, 0),
            cpuCores = prefs.getInt(KEY_CORES, 0),
            supportsVulkan = prefs.getBoolean(KEY_VULKAN, false),
            availableStorageBytes = prefs.getLong(KEY_STORAGE, 0L),
            apiLevel = prefs.getInt(KEY_API, 0)
        )
        val measured = prefs.getLong(KEY_MEASURED, -1L).takeIf { it >= 0 }
        return BenchmarkResult(
            caps = caps,
            tier = tier,
            recommendedMode = mode,
            measuredMillis = measured,
            timestamp = prefs.getLong(KEY_TIMESTAMP, 0L)
        )
    }

    private companion object {
        const val PREFS_NAME = "luma_device_benchmark"
        const val KEY_DEVICE_MODEL = "device_model"
        const val KEY_HARDWARE = "hardware"
        const val KEY_TOTAL_RAM = "total_ram_mb"
        const val KEY_AVAIL_RAM = "avail_ram_mb"
        const val KEY_CORES = "cpu_cores"
        const val KEY_VULKAN = "supports_vulkan"
        const val KEY_STORAGE = "storage_bytes"
        const val KEY_API = "api_level"
        const val KEY_TIER = "tier"
        const val KEY_MODE = "recommended_mode"
        const val KEY_MEASURED = "measured_millis"
        const val KEY_TIMESTAMP = "timestamp"
    }
}
