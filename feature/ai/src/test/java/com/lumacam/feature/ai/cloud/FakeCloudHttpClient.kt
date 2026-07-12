package com.lumacam.feature.ai.cloud

/**
 * Test double for [CloudHttpClient]. Returns a canned [CloudHttpResponse] or throws
 * a [CloudHttpException], so the full provider path (request → parse → normalize)
 * can be exercised in CI without any real network.
 */
class FakeCloudHttpClient(
    private val response: CloudHttpResponse? = null,
    private val error: CloudHttpException? = null
) : CloudHttpClient {

    var lastUrl: String? = null
        private set
    var lastHeaders: Map<String, String> = emptyMap()
        private set
    var lastBody: String? = null
        private set

    override suspend fun postJson(
        url: String,
        headers: Map<String, String>,
        body: String,
        timeoutMillis: Long
    ): CloudHttpResponse {
        lastUrl = url
        lastHeaders = headers
        lastBody = body
        error?.let { throw it }
        return response ?: error("FakeCloudHttpClient needs a response or error")
    }
}
