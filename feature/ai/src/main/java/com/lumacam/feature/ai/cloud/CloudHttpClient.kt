package com.lumacam.feature.ai.cloud

/** A single HTTP response: status [code] and raw [body] text. */
data class CloudHttpResponse(val code: Int, val body: String)

/**
 * Raised by a [CloudHttpClient] for transport-level problems so the provider can
 * map them to a [CloudAiError]. Carries a coarse [kind] rather than leaking the
 * underlying networking library's exception types.
 */
class CloudHttpException(val kind: Kind, cause: Throwable? = null) : Exception(cause) {
    enum class Kind { TIMEOUT, NO_NETWORK, UNREACHABLE, UNKNOWN }
}

/**
 * Minimal HTTP seam used by every provider. Abstracting the transport lets the
 * concrete OkHttp client run on-device while unit tests inject a fake that returns
 * fixture responses — so the full request→parse→normalize path is verified in CI
 * without any real network.
 */
interface CloudHttpClient {
    /**
     * Performs a POST of JSON [body] to [url] with [headers], bounded by
     * [timeoutMillis]. Returns the response for any completed HTTP exchange
     * (including 4xx/5xx); throws [CloudHttpException] only for transport failures.
     */
    suspend fun postJson(
        url: String,
        headers: Map<String, String>,
        body: String,
        timeoutMillis: Long
    ): CloudHttpResponse
}
