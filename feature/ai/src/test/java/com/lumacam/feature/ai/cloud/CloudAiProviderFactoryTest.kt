package com.lumacam.feature.ai.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudAiProviderFactoryTest {

    private val factory = CloudAiProviderFactory(FakeCloudHttpClient(CloudHttpResponse(200, "{}")))

    @Test
    fun geminiTypeCreatesGeminiProvider() {
        val provider = factory.create(CloudAiConfig(CloudProviderType.GEMINI, "k"))
        assertTrue(provider is GeminiProvider)
        assertEquals(CloudProviderType.GEMINI, provider.type)
    }

    @Test
    fun openAiCompatibleTypesCreateOpenAiProvider() {
        listOf(
            CloudProviderType.OPENAI,
            CloudProviderType.QWEN,
            CloudProviderType.HUNYUAN,
            CloudProviderType.OPENROUTER,
            CloudProviderType.CUSTOM
        ).forEach { type ->
            val provider = factory.create(CloudAiConfig(type, "k"))
            assertTrue("$type should use OpenAiCompatProvider", provider is OpenAiCompatProvider)
        }
    }
}
