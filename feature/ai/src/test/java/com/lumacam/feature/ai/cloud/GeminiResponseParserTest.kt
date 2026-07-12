package com.lumacam.feature.ai.cloud

import com.lumacam.feature.ai.SceneType
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiResponseParserTest {

    private val config = CloudAiConfig(type = CloudProviderType.GEMINI, apiKey = "g-key")

    private fun envelope(text: String): String {
        val quoted = JsonPrimitive(text).toString()
        return """{"candidates":[{"content":{"parts":[{"text":$quoted}]}}]}"""
    }

    @Test
    fun parsesGeminiEnvelopeIntoResult() = runTest {
        val inner = """{"sceneType":"night","compositionScore":64,"suggestedDirection":"right"}"""
        val http = FakeCloudHttpClient(CloudHttpResponse(200, envelope(inner)))
        val provider = GeminiProvider(config, http)

        val outcome = provider.analyze(CloudImage(byteArrayOf(1, 2)))

        assertTrue(outcome is CloudAiOutcome.Success)
        assertEquals(SceneType.NIGHT, (outcome as CloudAiOutcome.Success).result.sceneType)
        assertEquals(64, outcome.result.compositionScore)
    }

    @Test
    fun usesApiKeyHeaderAndGenerateContentUrl() = runTest {
        val http = FakeCloudHttpClient(CloudHttpResponse(200, envelope("""{"compositionScore":10}""")))
        val provider = GeminiProvider(config, http)

        provider.analyze(CloudImage(byteArrayOf(1)))

        assertEquals("g-key", http.lastHeaders["x-goog-api-key"])
        assertTrue(http.lastUrl!!.contains("gemini-1.5-flash:generateContent"))
        assertTrue(http.lastBody!!.contains("inline_data"))
    }
}
