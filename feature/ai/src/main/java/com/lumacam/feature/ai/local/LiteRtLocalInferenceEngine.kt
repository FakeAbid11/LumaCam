package com.lumacam.feature.ai.local

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LogSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Real on-device inference engine backed by Google's LiteRT-LM runtime
 * (`com.google.ai.edge.litertlm:litertlm-android`) — the recommended successor to
 * the now maintenance-mode MediaPipe LLM Inference API. It runs vision-language
 * models (e.g. Qwen2-VL-2B-Instruct, or any multimodal `.litertlm` from the
 * Hugging Face LiteRT Community) entirely on-device and returns the model's raw
 * text, which [DefaultLocalAiProvider] feeds through
 * [com.lumacam.feature.ai.cloud.CompositionJsonMapper] like every other backend.
 *
 * LiteRT-LM ships its native runtime inside the AAR, so no NDK/CMake is required
 * in our build. The model must be a `.litertlm` file — see [LocalModelCatalog]
 * for the curated download URLs. When [load] is told the model is multimodal, a
 * vision backend is initialized so [analyze] can attach the captured frame via
 * [Content.ImageBytes]; text-only models skip the vision backend entirely.
 *
 * Heavy JNI calls (model load + generate) run on [Dispatchers.IO] so they never
 * block the UI thread.
 */
class LiteRtLocalInferenceEngine(
    private val context: Context
) : LocalInferenceEngine {

    private var engine: Engine? = null
    private var isMultimodal: Boolean = false

    override suspend fun load(modelPath: String, multimodal: Boolean) {
        isMultimodal = multimodal
        try {
            engine = withContext(Dispatchers.IO) {
                Engine.setNativeMinLogSeverity(LogSeverity.ERROR)
                val config = EngineConfig(
                    modelPath = modelPath,
                    backend = Backend.CPU(),
                    visionBackend = if (multimodal) Backend.GPU() else null,
                    maxNumTokens = 2048,
                    maxNumImages = if (multimodal) 1 else 0,
                    cacheDir = context.cacheDir.absolutePath,
                )
                Engine(config).apply { initialize() }
            }
        } catch (e: Exception) {
            close()
            throw LocalInferenceException(
                LocalInferenceError.LOAD_FAILED,
                "Failed to load the on-device model: ${e.message}",
                e
            )
        }
    }

    override suspend fun analyze(image: LocalImage, prompt: String): String {
        val e = engine
            ?: throw LocalInferenceException(LocalInferenceError.LOAD_FAILED, "Model not loaded.")

        return withContext(Dispatchers.IO) {
            e.createConversation(ConversationConfig()).use { conversation ->
                val contents = if (isMultimodal) {
                    Contents.of(Content.ImageBytes(image.bytes), Content.Text(prompt))
                } else {
                    Contents.of(Content.Text(prompt))
                }
                try {
                    val message = conversation.sendMessage(contents)
                    val text = message.contents.contents
                        .filterIsInstance<Content.Text>()
                        .joinToString(separator = "\n") { it.text }
                    if (text.isBlank()) {
                        throw LocalInferenceException(
                            LocalInferenceError.INFERENCE_FAILED,
                            "The on-device model returned no text."
                        )
                    }
                    text
                } catch (err: LocalInferenceException) {
                    throw err
                } catch (err: Exception) {
                    throw LocalInferenceException(
                        LocalInferenceError.INFERENCE_FAILED,
                        "On-device inference failed: ${err.message}",
                        err
                    )
                }
            }
        }
    }

    override fun close() {
        runCatching { engine?.close() }
        engine = null
    }
}
