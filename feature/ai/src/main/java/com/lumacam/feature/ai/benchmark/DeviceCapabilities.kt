package com.lumacam.feature.ai.benchmark

/**
 * A snapshot of the phone's hardware/software specs relevant to running AI locally
 * (PRD §4 — Device AI Compatibility Benchmark). Deliberately free of any Android
 * API so the tier classification built on top of it is fully JVM-unit-testable; the
 * Android layer ([com.lumacam.app.data.DeviceCapabilityProbe]) fills these in.
 *
 * @param deviceModel human-readable model, e.g. Build.MODEL.
 * @param hardware SoC / board identifier, e.g. Build.HARDWARE / Build.SOC_MODEL.
 * @param totalRamMb total physical RAM in MB (ActivityManager.MemoryInfo.totalMem).
 * @param availableRamMb currently-available RAM in MB (MemoryInfo.availMem).
 * @param cpuCores number of CPU cores (Runtime.availableProcessors()).
 * @param supportsVulkan true when the device reports a Vulkan-capable GPU.
 * @param availableStorageBytes free bytes in app storage (StatFs).
 * @param apiLevel Android API level (Build.VERSION.SDK_INT).
 */
data class DeviceCapabilities(
    val deviceModel: String,
    val hardware: String,
    val totalRamMb: Int,
    val availableRamMb: Int,
    val cpuCores: Int,
    val supportsVulkan: Boolean,
    val availableStorageBytes: Long,
    val apiLevel: Int
)
