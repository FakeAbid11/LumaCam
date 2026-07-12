package com.lumacam.core.common.film

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FilmPresetCatalogTest {

    @Test
    fun `catalog starts with Original and includes all four film looks`() {
        val ids = FilmPresetCatalog.presets.map { it.id }
        assertEquals("original", ids.first())
        assertTrue(ids.containsAll(listOf("original", "disposable", "ccd", "minidv", "vhs")))
        assertEquals(5, FilmPresetCatalog.presets.size)
    }

    @Test
    fun `preset ids are unique`() {
        val ids = FilmPresetCatalog.presets.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `every preset has a 4x5 color matrix`() {
        FilmPresetCatalog.presets.forEach { preset ->
            assertEquals(
                "${preset.id} matrix size",
                FilmPreset.COLOR_MATRIX_SIZE,
                preset.colorMatrix.size
            )
        }
    }

    @Test
    fun `all normalized intensities are within 0 to 1`() {
        FilmPresetCatalog.presets.forEach { p ->
            listOf(
                "grain" to p.grainIntensity,
                "halation" to p.halationIntensity,
                "bloom" to p.bloomIntensity,
                "vignette" to p.vignetteIntensity,
                "chroma" to p.chromaticAberration,
                "scanline" to p.scanlineIntensity,
                "softness" to p.softness,
                "temperature" to p.temperature
            ).forEach { (label, value) ->
                assertTrue("${p.id} $label in range: $value", value in 0f..1f)
            }
        }
    }

    @Test
    fun `original is identity with no effects`() {
        val original = FilmPresetCatalog.Original
        assertTrue(original.isIdentity)
        assertEquals(0f, original.grainIntensity)
        assertEquals(0f, original.vignetteIntensity)
        assertEquals(0.5f, original.temperature)
        assertTrue(original.colorMatrix.contentEquals(FilmPreset.identityMatrix()))
    }

    @Test
    fun `only original is identity`() {
        FilmPresetCatalog.presets.filter { it.id != "original" }.forEach {
            assertFalse("${it.id} should not be identity", it.isIdentity)
        }
    }

    @Test
    fun `byId returns matching preset`() {
        assertSame(FilmPresetCatalog.Vhs, FilmPresetCatalog.byId("vhs"))
        assertSame(FilmPresetCatalog.Disposable, FilmPresetCatalog.byId("disposable"))
    }

    @Test
    fun `byId falls back to default for unknown or null`() {
        assertSame(FilmPresetCatalog.default, FilmPresetCatalog.byId("does-not-exist"))
        assertSame(FilmPresetCatalog.default, FilmPresetCatalog.byId(null))
        assertNotNull(FilmPresetCatalog.default)
    }

    @Test
    fun `disposable is warm and vhs is grainy - sanity of tuning`() {
        assertTrue(FilmPresetCatalog.Disposable.temperature > 0.5f)
        assertTrue(FilmPresetCatalog.Vhs.grainIntensity > FilmPresetCatalog.Ccd.grainIntensity)
        assertTrue(FilmPresetCatalog.Vhs.scanlineIntensity > 0f)
        assertTrue(FilmPresetCatalog.Ccd.temperature < 0.5f)
    }
}
