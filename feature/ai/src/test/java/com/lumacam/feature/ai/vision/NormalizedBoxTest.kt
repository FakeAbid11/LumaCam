package com.lumacam.feature.ai.vision

import org.junit.Assert.assertEquals
import org.junit.Test

class NormalizedBoxTest {

    @Test
    fun fromPixelsNormalizes() {
        val box = NormalizedBox.fromPixels(0f, 0f, 50f, 50f, 100, 100)
        assertEquals(0f, box.left, 0f)
        assertEquals(0.5f, box.right, 0f)
        assertEquals(0.25f, box.centerX, 1e-4f)
        assertEquals(0.25f, box.area, 1e-4f)
    }

    @Test
    fun fromPixelsClampsOutOfBounds() {
        val box = NormalizedBox.fromPixels(-10f, -10f, 200f, 200f, 100, 100)
        assertEquals(0f, box.left, 0f)
        assertEquals(1f, box.right, 0f)
    }

    @Test
    fun invalidImageSizeIsZeroBox() {
        val box = NormalizedBox.fromPixels(0f, 0f, 50f, 50f, 0, 0)
        assertEquals(0f, box.area, 0f)
    }

    @Test
    fun dimensionsAreDerived() {
        val box = NormalizedBox(0.2f, 0.1f, 0.6f, 0.5f)
        assertEquals(0.4f, box.width, 1e-4f)
        assertEquals(0.4f, box.height, 1e-4f)
        assertEquals(0.4f, box.centerX, 1e-4f)
        assertEquals(0.3f, box.centerY, 1e-4f)
    }
}
