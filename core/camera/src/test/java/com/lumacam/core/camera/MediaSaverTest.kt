package com.lumacam.core.camera

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaSaverTest {

    @Test
    fun buildMediaFileName_usesPrefixExtensionAndTimestamp() {
        val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).parse("20260101_120000")!!
        val name = buildMediaFileName("LumaCam", "jpg", date)
        assertEquals("LumaCam_20260101_120000.jpg", name)
    }

    @Test
    fun buildMediaFileName_supportsVideoExtension() {
        val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).parse("20260202_090000")!!
        assertEquals("LumaCam_20260202_090000.mp4", buildMediaFileName("LumaCam", "mp4", date))
    }
}
