package com.lumacam.feature.ai.cloud

/**
 * Central translation of HTTP status codes and transport exceptions into typed
 * [CloudAiError]s. Pure logic — unit-tested — so error messaging is consistent
 * across every provider.
 */
object CloudErrorMapper {

    /** Maps a non-2xx HTTP [code] to the most specific error we can give. */
    fun fromStatus(code: Int): CloudAiError = when (code) {
        401, 403 -> CloudAiError.InvalidKey
        429 -> CloudAiError.RateLimited
        in 500..599 -> CloudAiError.ServerError(code)
        else -> CloudAiError.Unexpected(code)
    }

    /** Maps a transport-level failure to an error. */
    fun fromTransport(kind: CloudHttpException.Kind): CloudAiError = when (kind) {
        CloudHttpException.Kind.TIMEOUT -> CloudAiError.Timeout
        CloudHttpException.Kind.NO_NETWORK -> CloudAiError.NoNetwork
        CloudHttpException.Kind.UNREACHABLE -> CloudAiError.EndpointUnreachable
        CloudHttpException.Kind.UNKNOWN -> CloudAiError.Unknown("transport")
    }
}
