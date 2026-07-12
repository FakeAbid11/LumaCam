package com.lumacam.feature.ai.cloud

/**
 * Every way a Cloud AI call can fail, each carrying a calm, specific, user-facing
 * [message] (never a generic "something went wrong"). Providers surface these
 * instead of throwing, so the UI can react without the app ever crashing.
 */
sealed class CloudAiError(val message: String) {

    /** No usable network connection. */
    data object NoNetwork : CloudAiError(
        "No internet connection. Reconnect and try again, or switch to on-device analysis."
    )

    /** The request exceeded the configured timeout. */
    data object Timeout : CloudAiError(
        "The request timed out. The server may be busy — try again in a moment."
    )

    /** Auth failed (HTTP 401/403): bad or missing key. */
    data object InvalidKey : CloudAiError(
        "That API key was rejected. Double-check the key for this provider in Settings."
    )

    /** Rate limited or out of quota (HTTP 429). */
    data object RateLimited : CloudAiError(
        "Rate limit reached for this key. Wait a little while, then try again."
    )

    /** The endpoint could not be reached / resolved (bad base URL, DNS). */
    data object EndpointUnreachable : CloudAiError(
        "Couldn't reach that endpoint. Check the base URL and your connection."
    )

    /** Server-side failure (HTTP 5xx). */
    data class ServerError(val code: Int) : CloudAiError(
        "The provider had a server error (HTTP $code). This is usually temporary — try again."
    )

    /** An unexpected non-success status we don't have a specific case for. */
    data class Unexpected(val code: Int) : CloudAiError(
        "Unexpected response from the provider (HTTP $code)."
    )

    /** The response wasn't valid JSON or didn't contain a usable result. */
    data object MalformedResponse : CloudAiError(
        "The provider replied in an unexpected format. Try a different model or provider."
    )

    /** Configuration is missing/incomplete (no key, base URL, or model). */
    data object NotConfigured : CloudAiError(
        "Cloud AI isn't fully set up yet. Add your provider, key, and model in Settings."
    )

    /** A genuinely unknown failure; [detail] aids debugging without alarming users. */
    data class Unknown(val detail: String) : CloudAiError(
        "Something interrupted the analysis. Please try again."
    )
}
