package com.lumacam.core.common.film

import org.junit.Assert.assertEquals
import org.junit.Test

class FilmColorMatrixTest {

    private val identity = FilmPreset.identityMatrix()

    @Test
    fun `saturation of one is identity`() {
        val m = FilmColorMatrix.saturation(1f)
        assertArrayApproxEquals(identity, m)
    }

    @Test
    fun `saturation of zero produces grayscale rows summing to one`() {
        val m = FilmColorMatrix.saturation(0f)
        // Each RGB output row should be the luminance weights (sum ~1), no offset.
        assertEquals(1f, m[0] + m[1] + m[2], 1e-4f)
        assertEquals(1f, m[5] + m[6] + m[7], 1e-4f)
        assertEquals(1f, m[10] + m[11] + m[12], 1e-4f)
        // All three rows identical (grayscale).
        assertEquals(m[0], m[5], 1e-4f)
        assertEquals(m[1], m[11], 1e-4f)
    }

    @Test
    fun `contrast of one is identity`() {
        val m = FilmColorMatrix.contrast(1f)
        assertArrayApproxEquals(identity, m)
    }

    @Test
    fun `contrast greater than one adds negative offset about mid-gray`() {
        val m = FilmColorMatrix.contrast(1.2f)
        assertEquals(1.2f, m[0], 1e-4f)
        // offset = (0.5 - 0.5*1.2) * 255 = -25.5
        assertEquals(-25.5f, m[4], 1e-3f)
    }

    @Test
    fun `concat with identity returns original`() {
        val sat = FilmColorMatrix.saturation(1.3f)
        assertArrayApproxEquals(sat, FilmColorMatrix.concat(identity, sat))
        assertArrayApproxEquals(sat, FilmColorMatrix.concat(sat, identity))
    }

    @Test
    fun `concat composes offsets correctly`() {
        val a = FilmColorMatrix.channelScale(rOffset = 10f)
        val b = FilmColorMatrix.channelScale(rOffset = 5f)
        // Applying b then a on red offset: 5 + 10 = 15 (gains are 1).
        val c = FilmColorMatrix.concat(a, b)
        assertEquals(15f, c[4], 1e-4f)
    }

    @Test
    fun `channelScale applies per-channel gain`() {
        val m = FilmColorMatrix.channelScale(rGain = 1.1f, gGain = 0.9f, bGain = 1.05f)
        assertEquals(1.1f, m[0], 1e-4f)
        assertEquals(0.9f, m[6], 1e-4f)
        assertEquals(1.05f, m[12], 1e-4f)
    }

    @Test
    fun `compose of single matrix returns it`() {
        val sat = FilmColorMatrix.saturation(0.7f)
        assertArrayApproxEquals(sat, FilmColorMatrix.compose(sat))
    }

    private fun assertArrayApproxEquals(expected: FloatArray, actual: FloatArray) {
        assertEquals(expected.size, actual.size)
        for (i in expected.indices) {
            assertEquals("index $i", expected[i], actual[i], 1e-4f)
        }
    }
}
