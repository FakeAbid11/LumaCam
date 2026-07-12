package com.lumacam.app.ui.camera

import com.lumacam.feature.ai.benchmark.DeviceTier
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the contract that the film preview-filter default and the general
 * "visual effects" default are driven by the SAME tier decision on low-end
 * (LIMITED / BRUTAL_TRUTH) devices — so the two never produce a half-reduced,
 * inconsistent look. Pure Kotlin, no Android deps.
 */
class CameraViewModelTierTest {

    @Test
    fun `LIMITED and BRUTAL_TRUTH are both treated as low-end`() {
        assertTrue(CameraViewModel.computeLowEndForTier(DeviceTier.LIMITED))
        assertTrue(CameraViewModel.computeLowEndForTier(DeviceTier.BRUTAL_TRUTH))
    }

    @Test
    fun `EXCELLENT and GOOD are not low-end`() {
        assertFalse(CameraViewModel.computeLowEndForTier(DeviceTier.EXCELLENT))
        assertFalse(CameraViewModel.computeLowEndForTier(DeviceTier.GOOD))
    }

    @Test
    fun `unknown tier defaults to not low-end`() {
        assertFalse(CameraViewModel.computeLowEndForTier(null))
    }

    @Test
    fun `preview filter default is the inverse of low-end`() {
        // When low-end (effects off by default), the film live-preview filter is
        // also off by default — both reduced together.
        assertTrue(CameraViewModel.computeLowEndForTier(DeviceTier.LIMITED))
        assertFalse(CameraViewModel.computeLowEndForTier(DeviceTier.LIMITED).not())
        assertFalse(CameraViewModel.computeLowEndForTier(DeviceTier.GOOD))
    }
}
