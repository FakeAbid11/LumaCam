package com.lumacam.feature.ai.cloud

/**
 * Fully-resolved settings needed to call one Cloud AI provider. The [apiKey] is
 * supplied by the user at runtime (stored encrypted) and is never bundled in the
 * app or read from build config.
 *
 * @param type which provider to talk to.
 * @param apiKey the user's secret key/token.
 * @param baseUrl API root; falls back to [CloudProviderType.defaultBaseUrl] when blank.
 * @param model model id; falls back to [CloudProviderType.defaultModel] when blank.
 * @param timeoutMillis per-call ceiling so a request never hangs indefinitely.
 */
data class CloudAiConfig(
    val type: CloudProviderType,
    val apiKey: String,
    val baseUrl: String = "",
    val model: String = "",
    val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS
) {
    /** The base URL to actually use, trimmed and without a trailing slash. */
    val effectiveBaseUrl: String
        get() = baseUrl.ifBlank { type.defaultBaseUrl }.trim().trimEnd('/')

    /** The model id to actually use. */
    val effectiveModel: String
        get() = model.ifBlank { type.defaultModel }.trim()

    /** True when this config has enough information to attempt a request. */
    val isComplete: Boolean
        get() = apiKey.isNotBlank() &&
            effectiveBaseUrl.isNotBlank() &&
            effectiveModel.isNotBlank()

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 30_000L

        /** Below this, a request is considered "taking longer than expected". */
        const val SLOW_WARNING_MILLIS = 8_000L
    }
}

/**
 * A single image to analyze, as already-encoded bytes plus a MIME type. Keeping
 * this vendor-neutral (no Android [android.graphics.Bitmap]) lets the provider
 * layer stay JVM-unit-testable.
 */
data class CloudImage(
    val bytes: ByteArray,
    val mimeType: String = "image/jpeg"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CloudImage) return false
        return mimeType == other.mimeType && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = 31 * bytes.contentHashCode() + mimeType.hashCode()
}
