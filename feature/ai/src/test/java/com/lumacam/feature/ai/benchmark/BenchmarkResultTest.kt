package com.lumacam.feature.ai.benchmark

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BenchmarkResultTest {

    private val caps = DeviceCapabilities(
        deviceModel = "Test",
        hardware = "soc",
        totalRamMb = 8 * 1024,
        availableRamMb = 4 * 1024,
        cpuCores = 8,
        supportsVulkan = true,
        availableStorageBytes = 4L * 1024 * 1024 * 1024,
        apiLevel = 30
    )

    @Test
    fun `estimated result reports not measured`() {
        val result = BenchmarkResult(
            caps = caps,
            tier = DeviceTier.EXCELLENT,
            recommendedMode = RecommendedMode.LOCAL_AI
        )
        assertFalse(result.isMeasured)
    }

    @Test
    fun `result with measured millis reports measured`() {
        val result = BenchmarkResult(
            caps = caps,
            tier = DeviceTier.EXCELLENT,
            recommendedMode = RecommendedMode.LOCAL_AI,
            measuredMillis = 1500L
        )
        assertTrue(result.isMeasured)
    }
}
