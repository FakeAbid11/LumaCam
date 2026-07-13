package com.lumacam.feature.ai.local

/**
 * Configurable test double for [LocalInferenceEngine]. Lets provider tests exercise
 * success, typed failures, and out-of-memory without any native runtime.
 */
class FakeLocalInferenceEngine(
    private val onLoad: () -> Unit = {},
    private val response: () -> String = { "{}" }
) : LocalInferenceEngine {

    var loadCount = 0
    var analyzeCount = 0
    var closeCount = 0
    var lastPrompt: String? = null

    override suspend fun load(modelPath: String, multimodal: Boolean) {
        loadCount++
        onLoad()
    }

    override suspend fun analyze(image: LocalImage, prompt: String): String {
        analyzeCount++
        lastPrompt = prompt
        return response()
    }

    override fun close() {
        closeCount++
    }
}
