package com.lumacam.feature.ai.local

/**
 * The single, clean Kotlin seam behind which ALL native inference lives. The rest
 * of the app (providers, view models, UI) only ever touches this interface — never
 * JNI, NDK, or a specific runtime.
 *
 * The real engine is [LiteRtLocalInferenceEngine], which runs on-device models
 * through Google's LiteRT-LM runtime (`com.google.ai.edge.litertlm:litertlm-android`)
 * — the recommended successor to the now maintenance-mode MediaPipe LLM Inference
 * API. No NDK/CMake is needed: the native libraries ship inside the AAR, so the
 * runtime is pulled in purely as an Android dependency and only exercised on a
 * physical device (it cannot run on the JVM/CI).
 *
 * Implementations must translate native failures into [LocalInferenceException]
 * (or let [OutOfMemoryError] propagate) so the provider can map them to a typed
 * [LocalAiError] without the app crashing.
 */
interface LocalInferenceEngine {

    /**
     * Loads the model at [modelPath] into memory, ready for [analyze].
     * [multimodal] tells the engine whether to initialize a vision backend so an
     * image can be attached during [analyze] (ignored by text-only models).
     */
    suspend fun load(modelPath: String, multimodal: Boolean = false)

    /**
     * Runs the loaded model on [image] with [prompt], returning the model's raw
     * textual reply (expected to contain the composition JSON).
     */
    suspend fun analyze(image: LocalImage, prompt: String): String

    /** Releases native resources / unloads the model. Safe to call repeatedly. */
    fun close()
}

/** Categories of engine failure, mapped by the provider to a [LocalAiError]. */
enum class LocalInferenceError {
    RUNTIME_UNAVAILABLE,
    OUT_OF_MEMORY,
    LOAD_FAILED,
    INFERENCE_FAILED
}

/** Thrown by a [LocalInferenceEngine] to signal a typed, recoverable failure. */
class LocalInferenceException(
    val error: LocalInferenceError,
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Stand-in engine used until a real native runtime is bundled. It never loads or
 * runs anything — every call reports [LocalInferenceError.RUNTIME_UNAVAILABLE], so
 * "Local AI Model" mode fails gracefully with a clear message instead of crashing
 * or silently doing nothing.
 */
class PlaceholderLocalInferenceEngine : LocalInferenceEngine {

    override suspend fun load(modelPath: String, multimodal: Boolean) {
        throw LocalInferenceException(
            LocalInferenceError.RUNTIME_UNAVAILABLE,
            "On-device inference runtime is not bundled in this build."
        )
    }

    override suspend fun analyze(image: LocalImage, prompt: String): String {
        throw LocalInferenceException(
            LocalInferenceError.RUNTIME_UNAVAILABLE,
            "On-device inference runtime is not bundled in this build."
        )
    }

    override fun close() = Unit
}
