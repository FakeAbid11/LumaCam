package com.lumacam.feature.ai.cloud

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderErrorHandlingTest {

    private val config = CloudAiConfig(type = CloudProviderType.OPENAI, apiKey = "sk-test")
    private val image = CloudImage(byteArrayOf(1, 2, 3))

    private fun provider(
        response: CloudHttpResponse? = null,
        error: CloudHttpException? = null
    ) = OpenAiCompatProvider(config, FakeCloudHttpClient(response, error))

    @Test
    fun invalidKeyStatusMapsToInvalidKey() = runTest {
        val outcome = provider(CloudHttpResponse(401, "unauthorized")).analyze(image)
        assertEquals(CloudAiError.InvalidKey, (outcome as CloudAiOutcome.Failure).error)
    }

    @Test
    fun rateLimitStatusMapsToRateLimited() = runTest {
        val outcome = provider(CloudHttpResponse(429, "slow down")).analyze(image)
        assertEquals(CloudAiError.RateLimited, (outcome as CloudAiOutcome.Failure).error)
    }

    @Test
    fun serverErrorStatusMapsToServerError() = runTest {
        val outcome = provider(CloudHttpResponse(500, "boom")).analyze(image)
        val error = (outcome as CloudAiOutcome.Failure).error
        assertTrue(error is CloudAiError.ServerError)
        assertEquals(500, (error as CloudAiError.ServerError).code)
    }

    @Test
    fun timeoutTransportMapsToTimeout() = runTest {
        val outcome = provider(error = CloudHttpException(CloudHttpException.Kind.TIMEOUT)).analyze(image)
        assertEquals(CloudAiError.Timeout, (outcome as CloudAiOutcome.Failure).error)
    }

    @Test
    fun noNetworkTransportMapsToNoNetwork() = runTest {
        val outcome = provider(error = CloudHttpException(CloudHttpException.Kind.NO_NETWORK)).analyze(image)
        assertEquals(CloudAiError.NoNetwork, (outcome as CloudAiOutcome.Failure).error)
    }

    @Test
    fun blankKeyMapsToNotConfiguredWithoutCallingNetwork() = runTest {
        val http = FakeCloudHttpClient(CloudHttpResponse(200, "should not be used"))
        val incomplete = OpenAiCompatProvider(
            CloudAiConfig(type = CloudProviderType.OPENAI, apiKey = ""),
            http
        )
        val outcome = incomplete.analyze(image)
        assertEquals(CloudAiError.NotConfigured, (outcome as CloudAiOutcome.Failure).error)
        assertEquals(null, http.lastUrl)
    }

    @Test
    fun testConnectionSucceedsOn200() = runTest {
        val result = provider(CloudHttpResponse(200, "{}")).testConnection()
        assertTrue(result is ConnectionTestResult.Success)
    }

    @Test
    fun testConnectionFailsOnInvalidKey() = runTest {
        val result = provider(CloudHttpResponse(401, "nope")).testConnection()
        assertEquals(
            CloudAiError.InvalidKey,
            (result as ConnectionTestResult.Failure).error
        )
    }

    @Test
    fun customProviderRequiresBaseUrlAndModel() = runTest {
        val http = FakeCloudHttpClient(CloudHttpResponse(200, "{}"))
        val custom = OpenAiCompatProvider(
            CloudAiConfig(type = CloudProviderType.CUSTOM, apiKey = "k"),
            http
        )
        val outcome = custom.analyze(image)
        assertEquals(CloudAiError.NotConfigured, (outcome as CloudAiOutcome.Failure).error)
    }
}
