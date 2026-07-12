package com.lumacam.feature.ai.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LightingAnalyzerTest {

    @Test
    fun darkFrameIsLowLight() {
        assertEquals("Low light", LightingAnalyzer.assess(0.1f).label)
        assertTrue(LightingAnalyzer.isLowLight(0.1f))
    }

    @Test
    fun midFrameIsGoodLight() {
        assertEquals("Good light", LightingAnalyzer.assess(0.5f).label)
        assertFalse(LightingAnalyzer.isLowLight(0.5f))
    }

    @Test
    fun brightFrameIsVeryBright() {
        assertEquals("Very bright", LightingAnalyzer.assess(0.95f).label)
    }
}
