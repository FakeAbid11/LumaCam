package com.lumacam.feature.ai.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalModelCatalogTest {

    @Test
    fun `catalog is non-empty and has unique ids`() {
        val models = LocalModelCatalog.models
        assertTrue(models.isNotEmpty())
        assertEquals(models.size, models.map { it.id }.toSet().size)
    }

    @Test
    fun `every model has complete metadata`() {
        LocalModelCatalog.models.forEach { m ->
            assertTrue(m.id.isNotBlank())
            assertTrue(m.name.isNotBlank())
            assertTrue(m.fileName.isNotBlank())
            assertTrue(m.quantization.isNotBlank())
            assertTrue(m.sizeBytes > 0)
            assertTrue(m.minRamMb > 0)
            assertTrue(m.downloadUrl.startsWith("https://"))
        }
    }

    @Test
    fun `findById returns match and null for unknown`() {
        val first = LocalModelCatalog.models.first()
        assertNotNull(LocalModelCatalog.findById(first.id))
        assertEquals(first, LocalModelCatalog.findById(first.id))
        assertNull(LocalModelCatalog.findById("does-not-exist"))
        assertNull(LocalModelCatalog.findById(null))
    }

    @Test
    fun `formatBytes renders human units`() {
        assertEquals("512 B", formatBytes(512))
        assertEquals("1 KB", formatBytes(1024))
        assertEquals("1 MB", formatBytes(1024L * 1024))
        assertEquals("1.5 GB", formatBytes((1.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun `formattedMinRam renders gigabytes`() {
        val spec = LocalModelCatalog.models.first().copy(minRamMb = 2048)
        assertEquals("2 GB", spec.formattedMinRam)
        assertEquals("512 MB", spec.copy(minRamMb = 512).formattedMinRam)
    }
}
