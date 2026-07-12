package com.lumacam.feature.ai.vision

import org.junit.Assert.assertEquals
import org.junit.Test

class LuminanceTest {

    @Test
    fun emptyArrayIsZero() {
        assertEquals(0f, Luminance.average(intArrayOf()), 0f)
    }

    @Test
    fun whiteIsFull() {
        assertEquals(1f, Luminance.average(intArrayOf(0xFFFFFFFF.toInt())), 1e-3f)
    }

    @Test
    fun blackIsZero() {
        assertEquals(0f, Luminance.average(intArrayOf(0xFF000000.toInt())), 1e-3f)
    }

    @Test
    fun midGrayIsAboutHalf() {
        assertEquals(0.502f, Luminance.average(intArrayOf(0xFF808080.toInt())), 1e-3f)
    }

    @Test
    fun averagesAcrossPixels() {
        val pixels = intArrayOf(0xFFFFFFFF.toInt(), 0xFF000000.toInt())
        assertEquals(0.5f, Luminance.average(pixels), 1e-3f)
    }
}
