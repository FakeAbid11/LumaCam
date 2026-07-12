package com.lumacam.feature.ai.cloud

/**
 * A Cloud AI backend that analyzes an image and returns the shared
 * [com.lumacam.feature.ai.CompositionResult] (via [CloudAiOutcome]). Providers
 * never throw — all failures are returned as [CloudAiError].
 *
 * PRD §4 Tier 4 / §6: this abstraction keeps concrete vendor deps inside
 * `:feature:ai`; the camera and app layers only see this interface.
 */
interface CloudAiProvider {

    /** Which backend this instance talks to. */
    val type: CloudProviderType

    /**
     * Analyzes [image] with optional freeform [context], returning a normalized
     * composition result or a typed error.
     */
    suspend fun analyze(image: CloudImage, context: String? = null): CloudAiOutcome

    /**
     * Makes a minimal request to validate the key/endpoint/model without a full
     * image analysis. Used by the Settings "Test Connection" button.
     */
    suspend fun testConnection(): ConnectionTestResult
}
