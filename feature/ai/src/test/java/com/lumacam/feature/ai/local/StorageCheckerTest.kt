package com.lumacam.feature.ai.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageCheckerTest {

    private val mb = 1024L * 1024

    @Test
    fun `enough space when available exceeds size plus headroom`() {
        val result = StorageChecker.check(
            modelSizeBytes = 300 * mb,
            availableBytes = 800 * mb,
            headroomBytes = 200 * mb
        )
        assertTrue(result.hasEnoughSpace)
        assertEquals(500 * mb, result.requiredBytes)
        assertEquals(0L, result.shortfallBytes)
    }

    @Test
    fun `insufficient when available below required`() {
        val result = StorageChecker.check(
            modelSizeBytes = 300 * mb,
            availableBytes = 400 * mb,
            headroomBytes = 200 * mb
        )
        assertFalse(result.hasEnoughSpace)
        assertEquals(100 * mb, result.shortfallBytes)
    }

    @Test
    fun `exact required amount is enough`() {
        val result = StorageChecker.check(
            modelSizeBytes = 100 * mb,
            availableBytes = 300 * mb,
            headroomBytes = 200 * mb
        )
        assertTrue(result.hasEnoughSpace)
    }

    @Test
    fun `default headroom is applied`() {
        val result = StorageChecker.check(modelSizeBytes = 0, availableBytes = 0)
        assertEquals(StorageChecker.HEADROOM_BYTES, result.requiredBytes)
        assertFalse(result.hasEnoughSpace)
    }
}
