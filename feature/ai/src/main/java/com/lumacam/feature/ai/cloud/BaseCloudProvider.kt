package com.lumacam.feature.ai.cloud

import okio.ByteString.Companion.toByteString

/**
 * Shared request/response orchestration for every provider: config validation,
 * transport-error mapping, status-code handling, and JSON normalization. Concrete
 * providers only supply the wire-format specifics (URL, headers, body, and how to
 * pull the model's text out of the response envelope).
 */
abstract class BaseCloudProvider(
    protected val config: CloudAiConfig,
    protected val http: CloudHttpClient
) : CloudAiProvider {

    override val type: CloudProviderType get() = config.type

    override suspend fun analyze(image: CloudImage, context: String?): CloudAiOutcome {
        if (!config.isComplete) return CloudAiOutcome.Failure(CloudAiError.NotConfigured)

        val prompt = CompositionPromptBuilder.build(context)
        val body = buildAnalyzeBody(prompt, image)

        val response = try {
            http.postJson(requestUrl(), requestHeaders(), body, config.timeoutMillis)
        } catch (e: CloudHttpException) {
            return CloudAiOutcome.Failure(CloudErrorMapper.fromTransport(e.kind))
        } catch (e: Exception) {
            // Cancellation must propagate so structured concurrency (e.g. a screen
            // dismissed mid-request) cancels cleanly instead of being swallowed.
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            return CloudAiOutcome.Failure(CloudAiError.Unknown(e.message ?: "analyze"))
        }

        if (response.code !in 200..299) {
            return CloudAiOutcome.Failure(CloudErrorMapper.fromStatus(response.code))
        }

        val content = extractContent(response.body)
            ?: return CloudAiOutcome.Failure(CloudAiError.MalformedResponse)
        val result = CompositionJsonMapper.parse(content)
            ?: return CloudAiOutcome.Failure(CloudAiError.MalformedResponse)
        return CloudAiOutcome.Success(result)
    }

    override suspend fun testConnection(): ConnectionTestResult {
        if (config.apiKey.isBlank() || !config.isComplete) {
            return ConnectionTestResult.Failure(CloudAiError.NotConfigured)
        }
        val response = try {
            http.postJson(requestUrl(), requestHeaders(), buildPingBody(), config.timeoutMillis)
        } catch (e: CloudHttpException) {
            return ConnectionTestResult.Failure(CloudErrorMapper.fromTransport(e.kind))
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            return ConnectionTestResult.Failure(CloudAiError.Unknown(e.message ?: "test"))
        }
        return if (response.code in 200..299) {
            ConnectionTestResult.Success
        } else {
            ConnectionTestResult.Failure(CloudErrorMapper.fromStatus(response.code))
        }
    }

    /** `data:<mime>;base64,<...>` form used by OpenAI-compatible image parts. */
    protected fun dataUrl(image: CloudImage): String =
        "data:${image.mimeType};base64,${base64(image)}"

    protected fun base64(image: CloudImage): String =
        image.bytes.toByteString().base64()

    protected abstract fun requestUrl(): String
    protected abstract fun requestHeaders(): Map<String, String>
    protected abstract fun buildAnalyzeBody(prompt: String, image: CloudImage): String
    protected abstract fun buildPingBody(): String
    protected abstract fun extractContent(responseBody: String): String?
}
