package com.lumacam.feature.ai.cloud

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Provider for Google Gemini's `generateContent` API (PRD §4 Tier 4). Uses the
 * `x-goog-api-key` header (keeping the key out of the URL) and requests JSON via
 * `responseMimeType`.
 */
class GeminiProvider(
    config: CloudAiConfig,
    http: CloudHttpClient
) : BaseCloudProvider(config, http) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Serializable
    private data class Envelope(val candidates: List<Candidate> = emptyList())

    @Serializable
    private data class Candidate(val content: Content? = null)

    @Serializable
    private data class Content(val parts: List<Part> = emptyList())

    @Serializable
    private data class Part(val text: String? = null)

    override fun requestUrl(): String =
        "${config.effectiveBaseUrl}/v1beta/models/${config.effectiveModel}:generateContent"

    override fun requestHeaders(): Map<String, String> =
        mapOf("x-goog-api-key" to config.apiKey)

    override fun buildAnalyzeBody(prompt: String, image: CloudImage): String {
        val root = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    putJsonArray("parts") {
                        addJsonObject { put("text", prompt) }
                        addJsonObject {
                            putJsonObject("inline_data") {
                                put("mime_type", image.mimeType)
                                put("data", base64(image))
                            }
                        }
                    }
                }
            }
            putJsonObject("generationConfig") {
                put("temperature", 0.2)
                put("responseMimeType", "application/json")
            }
        }
        return root.toString()
    }

    override fun buildPingBody(): String {
        val root = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    putJsonArray("parts") {
                        addJsonObject { put("text", "ping") }
                    }
                }
            }
            putJsonObject("generationConfig") { put("maxOutputTokens", 1) }
        }
        return root.toString()
    }

    override fun extractContent(responseBody: String): String? =
        runCatching { json.decodeFromString<Envelope>(responseBody) }
            .getOrNull()
            ?.candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull { !it.text.isNullOrBlank() }
            ?.text
}
