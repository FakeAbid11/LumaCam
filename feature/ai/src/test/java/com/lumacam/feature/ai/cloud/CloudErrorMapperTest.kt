package com.lumacam.feature.ai.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudErrorMapperTest {

    @Test
    fun authFailuresMapToInvalidKey() {
        assertEquals(CloudAiError.InvalidKey, CloudErrorMapper.fromStatus(401))
        assertEquals(CloudAiError.InvalidKey, CloudErrorMapper.fromStatus(403))
    }

    @Test
    fun tooManyRequestsMapsToRateLimited() {
        assertEquals(CloudAiError.RateLimited, CloudErrorMapper.fromStatus(429))
    }

    @Test
    fun serverErrorsCarryCode() {
        val e = CloudErrorMapper.fromStatus(503)
        assertTrue(e is CloudAiError.ServerError)
        assertEquals(503, (e as CloudAiError.ServerError).code)
    }

    @Test
    fun otherCodesAreUnexpected() {
        val e = CloudErrorMapper.fromStatus(418)
        assertTrue(e is CloudAiError.Unexpected)
        assertEquals(418, (e as CloudAiError.Unexpected).code)
    }

    @Test
    fun transportKindsMap() {
        assertEquals(
            CloudAiError.Timeout,
            CloudErrorMapper.fromTransport(CloudHttpException.Kind.TIMEOUT)
        )
        assertEquals(
            CloudAiError.NoNetwork,
            CloudErrorMapper.fromTransport(CloudHttpException.Kind.NO_NETWORK)
        )
        assertEquals(
            CloudAiError.EndpointUnreachable,
            CloudErrorMapper.fromTransport(CloudHttpException.Kind.UNREACHABLE)
        )
        assertTrue(
            CloudErrorMapper.fromTransport(CloudHttpException.Kind.UNKNOWN) is CloudAiError.Unknown
        )
    }

    @Test
    fun everyErrorHasAFriendlyMessage() {
        val errors = listOf(
            CloudAiError.NoNetwork,
            CloudAiError.Timeout,
            CloudAiError.InvalidKey,
            CloudAiError.RateLimited,
            CloudAiError.EndpointUnreachable,
            CloudAiError.ServerError(500),
            CloudAiError.Unexpected(400),
            CloudAiError.MalformedResponse,
            CloudAiError.NotConfigured,
            CloudAiError.Unknown("x")
        )
        errors.forEach { assertTrue(it.message.isNotBlank()) }
    }
}
