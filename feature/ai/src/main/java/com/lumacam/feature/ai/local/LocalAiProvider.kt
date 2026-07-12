package com.lumacam.feature.ai.local

/**
 * A Local AI backend that analyzes an image with an on-device model and returns the
 * shared [com.lumacam.feature.ai.CompositionResult] (via [LocalAiOutcome]).
 * Providers never throw — all failures, including out-of-memory, are returned as a
 * typed [LocalAiError].
 *
 * PRD §4 Tier 3 / §6: this abstraction keeps the native runtime and model wiring
 * inside `:feature:ai`; the camera and app layers only see this interface, exactly
 * like [com.lumacam.feature.ai.cloud.CloudAiProvider].
 */
interface LocalAiProvider {

    /**
     * Analyzes [image] with optional freeform [context], returning a normalized
     * composition result or a typed error.
     */
    suspend fun analyze(image: LocalImage, context: String? = null): LocalAiOutcome
}

/** The currently-selected, downloaded model plus its on-disk path. */
data class ActiveLocalModel(
    val spec: LocalModelSpec,
    val filePath: String
)
