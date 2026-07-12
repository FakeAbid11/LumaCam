package com.lumacam.feature.ai.cloud

import com.lumacam.feature.ai.MoveDirection
import com.lumacam.feature.ai.SceneType
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenAiResponseParserTest {

    private val config = CloudAiConfig(type = CloudProviderType.OPENAI, apiKey = "sk-test")

    private fun envelope(content: String): String {
        val quoted = JsonPrimitive(content).toString()
        return """{"choices":[{"message":{"role":"assistant","content":$quoted}}]}"""
    }

    @Test
    fun parsesOpenAiEnvelopeIntoResult() = runTest {
        val inner = """
            {"sceneType":"food","compositionScore":88,"suggestedDirection":"none",
             "tiltAngle":0.0,"lighting":{"label":"Soft","description":"Even light"},
             "suggestions":["Fill the frame"]}
        """.trimIndent()
        val http = FakeCloudHttpClient(CloudHttpResponse(200, envelope(inner)))
        val provider = OpenAiCompatProvider(config, http)

        val outcome = provider.analyze(CloudImage(byteArrayOf(1, 2, 3)))

        assertTrue(outcome is CloudAiOutcome.Success)
        val result = (outcome as CloudAiOutcome.Success).result
        assertEquals(SceneType.FOOD, result.sceneType)
        assertEquals(88, result.compositionScore)
        assertEquals(MoveDirection.NONE, result.suggestedDirection)
    }

    @Test
    fun sendsBearerAuthAndImagePayload() = runTest {
        val http = FakeCloudHttpClient(CloudHttpResponse(200, envelope("""{"compositionScore":50}""")))
        val provider = OpenAiCompatProvider(config, http)

        provider.analyze(CloudImage(byteArrayOf(9, 9, 9)))

        assertEquals("Bearer sk-test", http.lastHeaders["Authorization"])
        assertTrue(http.lastUrl!!.endsWith("/v1/chat/completions"))
        assertTrue(http.lastBody!!.contains("data:image/jpeg;base64,"))
        assertTrue(http.lastBody!!.contains("gpt-4o-mini"))
    }

    @Test
    fun malformedContentBecomesMalformedResponse() = runTest {
        val http = FakeCloudHttpClient(CloudHttpResponse(200, envelope("not json at all")))
        val provider = OpenAiCompatProvider(config, http)

        val outcome = provider.analyze(CloudImage(byteArrayOf(1)))

        assertTrue(outcome is CloudAiOutcome.Failure)
        assertEquals(CloudAiError.MalformedResponse, (outcome as CloudAiOutcome.Failure).error)
    }

    @Test
    fun emptyChoicesBecomesMalformedResponse() = runTest {
        val http = FakeCloudHttpClient(CloudHttpResponse(200, """{"choices":[]}"""))
        val provider = OpenAiCompatProvider(config, http)

        val outcome = provider.analyze(CloudImage(byteArrayOf(1)))

        assertEquals(
            CloudAiError.MalformedResponse,
            (outcome as CloudAiOutcome.Failure).error
        )
    }
}
