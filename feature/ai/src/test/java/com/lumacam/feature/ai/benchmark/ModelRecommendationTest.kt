package com.lumacam.feature.ai.benchmark

import com.lumacam.feature.ai.local.LocalModelSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelRecommendationTest {

    private val gb = 1024L * 1024 * 1024

    private val model = LocalModelSpec(
        id = "test-model",
        name = "Test Model",
        description = "desc",
        sizeBytes = 300L * 1024 * 1024,
        quantization = "Q8_0",
        minRamMb = 3072,
        fileName = "test.gguf",
        downloadUrl = "https://example.com/test.gguf"
    )

    private fun caps(totalRamMb: Int, storage: Long) = DeviceCapabilities(
        deviceModel = "Test",
        hardware = "soc",
        totalRamMb = totalRamMb,
        availableRamMb = totalRamMb / 2,
        cpuCores = 8,
        supportsVulkan = true,
        availableStorageBytes = storage,
        apiLevel = 30
    )

    @Test
    fun `recommended when ram and storage sufficient on capable tier`() {
        val caps = caps(totalRamMb = 8 * 1024, storage = 4 * gb)
        val result = ModelSuitability.evaluate(caps, DeviceTier.EXCELLENT, model)
        assertEquals(ModelRecommendation.Recommended, result)
    }

    @Test
    fun `not recommended when ram below model minimum`() {
        val caps = caps(totalRamMb = 2 * 1024, storage = 4 * gb)
        val result = ModelSuitability.evaluate(caps, DeviceTier.LIMITED, model)
        assertTrue(result is ModelRecommendation.NotRecommended)
        assertTrue((result as ModelRecommendation.NotRecommended).reason.contains("RAM"))
    }

    @Test
    fun `not recommended when storage insufficient`() {
        val caps = caps(totalRamMb = 8 * 1024, storage = 100L * 1024 * 1024)
        val result = ModelSuitability.evaluate(caps, DeviceTier.EXCELLENT, model)
        assertTrue(result is ModelRecommendation.NotRecommended)
        assertTrue((result as ModelRecommendation.NotRecommended).reason.contains("free"))
    }

    @Test
    fun `not recommended on brutal truth even when it fits`() {
        val caps = caps(totalRamMb = 4 * 1024, storage = 4 * gb)
        val result = ModelSuitability.evaluate(caps, DeviceTier.BRUTAL_TRUTH, model)
        assertTrue(result is ModelRecommendation.NotRecommended)
        assertTrue((result as ModelRecommendation.NotRecommended).reason.contains("slow"))
    }

    @Test
    fun `ram gate takes priority over storage gate`() {
        val caps = caps(totalRamMb = 1024, storage = 10L * 1024 * 1024)
        val result = ModelSuitability.evaluate(caps, DeviceTier.BRUTAL_TRUTH, model)
        assertTrue(result is ModelRecommendation.NotRecommended)
        assertTrue((result as ModelRecommendation.NotRecommended).reason.contains("RAM"))
    }
}
