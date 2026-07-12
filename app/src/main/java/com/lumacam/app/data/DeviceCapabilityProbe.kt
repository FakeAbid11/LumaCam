package com.lumacam.app.data

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.StatFs
import com.lumacam.feature.ai.benchmark.DeviceCapabilities

/**
 * Reads real device specs into a [DeviceCapabilities] snapshot for the Device AI
 * Compatibility Benchmark (PRD §4). This is near-instant, spec-reading only — not a
 * model inference benchmark.
 *
 * Android-only (Build / ActivityManager / PackageManager / StatFs), so it is not
 * JVM-unit-tested; the tier logic that consumes it lives in `:feature:ai` and is
 * fully tested there.
 */
class DeviceCapabilityProbe(private val context: Context) {

    fun probe(): DeviceCapabilities {
        val app = context.applicationContext

        val am = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }

        return DeviceCapabilities(
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
            hardware = socIdentifier(),
            totalRamMb = (memInfo.totalMem / BYTES_PER_MB).toInt(),
            availableRamMb = (memInfo.availMem / BYTES_PER_MB).toInt(),
            cpuCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1),
            supportsVulkan = detectVulkan(app.packageManager),
            availableStorageBytes = availableStorage(app),
            apiLevel = Build.VERSION.SDK_INT
        )
    }

    private fun socIdentifier(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val soc = "${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}".trim()
            if (soc.isNotBlank() && !soc.equals("unknown unknown", ignoreCase = true)) return soc
        }
        return Build.HARDWARE.ifBlank { Build.BOARD }
    }

    private fun detectVulkan(pm: PackageManager): Boolean =
        pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION) ||
            pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)

    private fun availableStorage(context: Context): Long = runCatching {
        val path = context.getExternalFilesDir(null) ?: context.filesDir
        StatFs(path.absolutePath).availableBytes
    }.getOrDefault(0L)

    private companion object {
        const val BYTES_PER_MB = 1024L * 1024L
    }
}
