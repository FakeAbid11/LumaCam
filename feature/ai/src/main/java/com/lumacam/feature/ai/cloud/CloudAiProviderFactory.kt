package com.lumacam.feature.ai.cloud

/**
 * Builds the right [CloudAiProvider] for a given [CloudAiConfig]. Gemini gets its
 * dedicated client; everything else shares the OpenAI-compatible provider. Keeping
 * construction here means the app only wires one HTTP client and asks the factory
 * for a provider per request.
 */
class CloudAiProviderFactory(
    private val http: CloudHttpClient = OkHttpCloudClient()
) {
    fun create(config: CloudAiConfig): CloudAiProvider = when (config.type) {
        CloudProviderType.GEMINI -> GeminiProvider(config, http)
        else -> OpenAiCompatProvider(config, http)
    }
}
