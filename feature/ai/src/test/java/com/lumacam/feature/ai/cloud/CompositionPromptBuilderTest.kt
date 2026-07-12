package com.lumacam.feature.ai.cloud

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositionPromptBuilderTest {

    @Test
    fun promptRequestsAllResultFields() {
        val prompt = CompositionPromptBuilder.build()
        listOf(
            "sceneType",
            "compositionScore",
            "suggestedDirection",
            "tiltAngle",
            "lighting",
            "suggestions"
        ).forEach { field ->
            assertTrue("prompt should mention $field", prompt.contains(field))
        }
    }

    @Test
    fun promptAsksForJsonOnly() {
        val prompt = CompositionPromptBuilder.build().lowercase()
        assertTrue(prompt.contains("json"))
    }

    @Test
    fun includesUserContextWhenProvided() {
        val prompt = CompositionPromptBuilder.build("focus on the food")
        assertTrue(prompt.contains("focus on the food"))
    }

    @Test
    fun omitsContextSectionWhenBlank() {
        val prompt = CompositionPromptBuilder.build("   ")
        assertFalse(prompt.contains("Additional context"))
    }
}
