package com.lumacam.feature.ai.benchmark

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceTierClassifierTest {

    private fun caps(
        totalRamMb: Int,
        cores: Int,
        api: Int,
        vulkan: Boolean = true,
        storage: Long = 10L * 1024 * 1024 * 1024
    ) = DeviceCapabilities(
        deviceModel = "Test Device",
        hardware = "test-soc",
        totalRamMb = totalRamMb,
        availableRamMb = totalRamMb / 2,
        cpuCores = cores,
        supportsVulkan = vulkan,
        availableStorageBytes = storage,
        apiLevel = api
    )

    @Test
    fun `flagship hits EXCELLENT`() {
        val tier = DeviceTierClassifier.classify(caps(totalRamMb = 12 * 1024, cores = 8, api = 33))
        assertEquals(DeviceTier.EXCELLENT, tier)
    }

    @Test
    fun `no vulkan drops flagship out of EXCELLENT to GOOD`() {
        val tier = DeviceTierClassifier.classify(
            caps(totalRamMb = 12 * 1024, cores = 8, api = 33, vulkan = false)
        )
        assertEquals(DeviceTier.GOOD, tier)
    }

    @Test
    fun `midrange hits GOOD`() {
        val tier = DeviceTierClassifier.classify(caps(totalRamMb = 6 * 1024, cores = 6, api = 28))
        assertEquals(DeviceTier.GOOD, tier)
    }

    @Test
    fun `too few cores drops out of GOOD to LIMITED`() {
        val tier = DeviceTierClassifier.classify(caps(totalRamMb = 6 * 1024, cores = 4, api = 28))
        assertEquals(DeviceTier.LIMITED, tier)
    }

    @Test
    fun `low-end hits LIMITED`() {
        val tier = DeviceTierClassifier.classify(caps(totalRamMb = 4 * 1024, cores = 4, api = 24))
        assertEquals(DeviceTier.LIMITED, tier)
    }

    @Test
    fun `below floor is BRUTAL_TRUTH`() {
        val tier = DeviceTierClassifier.classify(caps(totalRamMb = 2 * 1024, cores = 4, api = 24))
        assertEquals(DeviceTier.BRUTAL_TRUTH, tier)
    }

    @Test
    fun `ram above limited but api too old is BRUTAL_TRUTH`() {
        val tier = DeviceTierClassifier.classify(caps(totalRamMb = 4 * 1024, cores = 4, api = 21))
        assertEquals(DeviceTier.BRUTAL_TRUTH, tier)
    }

    @Test
    fun `excellent with model recommends local ai`() {
        assertEquals(
            RecommendedMode.LOCAL_AI,
            DeviceTierClassifier.recommendedMode(DeviceTier.EXCELLENT, hasDownloadedModel = true)
        )
    }

    @Test
    fun `excellent without model recommends luma vision`() {
        assertEquals(
            RecommendedMode.LUMA_VISION,
            DeviceTierClassifier.recommendedMode(DeviceTier.EXCELLENT, hasDownloadedModel = false)
        )
    }

    @Test
    fun `brutal truth never recommends local ai even with model`() {
        assertEquals(
            RecommendedMode.LUMA_VISION,
            DeviceTierClassifier.recommendedMode(DeviceTier.BRUTAL_TRUTH, hasDownloadedModel = true)
        )
    }

    @Test
    fun `limited recommends luma vision`() {
        assertEquals(
            RecommendedMode.LUMA_VISION,
            DeviceTierClassifier.recommendedMode(DeviceTier.LIMITED, hasDownloadedModel = true)
        )
    }
}
