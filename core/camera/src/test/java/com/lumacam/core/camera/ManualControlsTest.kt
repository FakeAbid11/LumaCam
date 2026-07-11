package com.lumacam.core.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualControlsTest {

    @Test
    fun singleLensIsWide() {
        assertEquals(listOf(LensType.WIDE), classifyLenses(listOf(4.5f)))
    }

    @Test
    fun emptyLensListIsEmpty() {
        assertTrue(classifyLenses(emptyList()).isEmpty())
    }

    @Test
    fun tripleLensClassifiesUltraWideMainTele() {
        val result = classifyLenses(listOf(2.0f, 4.0f, 8.0f))
        assertEquals(
            listOf(LensType.ULTRA_WIDE, LensType.WIDE, LensType.TELEPHOTO),
            result
        )
    }

    @Test
    fun classificationIsOrderIndependentPerFocal() {
        // Same focal set, shuffled order -> each focal keeps its type.
        val result = classifyLenses(listOf(8.0f, 2.0f, 4.0f))
        assertEquals(
            listOf(LensType.TELEPHOTO, LensType.ULTRA_WIDE, LensType.WIDE),
            result
        )
    }

    @Test
    fun isoClampsToRange() {
        assertEquals(100, coerceIso(50, 100..3200))
        assertEquals(3200, coerceIso(9999, 100..3200))
        assertEquals(800, coerceIso(800, 100..3200))
    }

    @Test
    fun isoWithNullRangePassesThrough() {
        assertEquals(1234, coerceIso(1234, null))
    }

    @Test
    fun exposureTimeClampsToRange() {
        val range = 1_000L..100_000_000L
        assertEquals(1_000L, coerceExposureTimeNanos(10L, range))
        assertEquals(100_000_000L, coerceExposureTimeNanos(999_000_000L, range))
    }

    @Test
    fun exposureCompensationClamps() {
        assertEquals(-4, coerceExposureCompensation(-10, -4..4))
        assertEquals(4, coerceExposureCompensation(10, -4..4))
        assertEquals(0, coerceExposureCompensation(0, -4..4))
    }

    @Test
    fun focusDistanceClampsBetweenZeroAndMinDistance() {
        assertEquals(0f, coerceFocusDistance(-1f, 10f))
        assertEquals(10f, coerceFocusDistance(50f, 10f))
        assertEquals(5f, coerceFocusDistance(5f, 10f))
    }

    @Test
    fun focusDistanceWithNoManualSupportStaysAtInfinity() {
        assertEquals(0f, coerceFocusDistance(5f, 0f))
    }

    @Test
    fun defaultsAreClampedToRange() {
        assertEquals(200, defaultIso(200..1600))
        assertEquals(50_000_000L, defaultExposureTimeNanos(50_000_000L..100_000_000L))
    }

    @Test
    fun whiteBalanceRoundTripsThroughAwbMode() {
        WhiteBalanceMode.entries.forEach { mode ->
            assertEquals(mode, WhiteBalanceMode.fromAwbMode(mode.awbMode))
        }
    }

    @Test
    fun manualStateFlagsReflectOverrides() {
        val auto = ManualCameraState()
        assertTrue(!auto.isManualExposure && !auto.isManualFocus)
        assertTrue(ManualCameraState(isoValue = 400).isManualExposure)
        assertTrue(ManualCameraState(exposureTimeNanos = 1_000L).isManualExposure)
        assertTrue(ManualCameraState(focusDistance = 2f).isManualFocus)
    }
}
