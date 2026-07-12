package com.lumacam.feature.ai.cloud

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Provider for every OpenAI-compatible `/chat/completions` endpoint — OpenAI
 * itself plus Qwen VL (DashScope), Hunyuan Vision, OpenRouter, and any custom
 * self-hosted endpoint. They share the same request/response wire format and
 * differ only by base URL, default model, and (optionally) a JSON response mode.
 */
class OpenAiCompatProvider(
    config: CloudAiConfig,
    http: CloudHttpClient
) : BaseCloudProvider(config, http) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Serializable
    private data class Envelope(@SerialName("choices") val choices: List<Choice> = emptyList())

    @Serializable
    private data class Choice(@SerialName("message") val message: Message? = null)

    @Serializable
    private data class Message(@SerialName("content") val content: String? = null)

    override fun requestUrl(): String = "${config.effectiveBaseUrl}/v1/chat/completions"

    override fun requestHeaders(): Map<String, String> =
        mapOf("Authorization" to "Bearer ${config.apiKey}")

    override fun buildAnalyzeBody(prompt: String, image: CloudImage): String {
        val root = buildJsonObject {
            put("model", config.effectiveModel)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("content") {
                        addJsonObject {
                            put("type", "text")
                            put("text", prompt)
                        }
                        addJsonObject {
                            put("type", "image_url")
                            putJsonObject("image_url") {
                                put("url", dataUrl(image))
                            }
                        }
                    }
                }
            }
            put("temperature", 0.2)
            put("max_tokens", 800)
            // Only OpenAI reliably supports strict JSON mode; others may reject
            // unknown fields, so we rely on the prompt for those.
            if (config.type == CloudProviderType.OPENAI) {
                putJsonObject("response_format") { put("type", "json_object") }
            }
        }
        return root.toString()
    }

    override fun buildPingBody(): String {
        val root = buildJsonObject {
            put("model", config.effectiveModel)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    put("content", "ping")
                }
            }
            put("max_tokens", 1)
        }
        return root.toString()
    }

    override fun extractContent(responseBody: String): String? =
        runCatching { json.decodeFromString<Envelope>(responseBody) }
            .getOrNull()
            ?.choices
            ?.firstOrNull()
            ?.message
            ?.content
}
