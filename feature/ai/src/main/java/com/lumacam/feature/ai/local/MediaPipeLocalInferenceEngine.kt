package com.lumacam.feature.ai.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Real on-device inference engine backed by Google's MediaPipe LLM Inference
 * task (`com.google.mediapipe:tasks-genai`). It runs a vision-language model
 * (e.g. Gemma 3n / Gemma 3 1B in MediaPipe `.task` format) entirely on-device
 * and returns the model's raw text, which [DefaultLocalAiProvider] feeds through
 * [com.lumacam.feature.ai.cloud.CompositionJsonMapper] like every other backend.
 *
 * MediaPipe ships its native runtime inside the AAR, so no NDK/CMake is required
 * in our build. The model must be a MediaPipe `.task` file (not raw GGUF) — see
 * [LocalModelCatalog] for the curated download URLs. Vision input is enabled via
 * [GraphOptions.setEnableVisionModality]; [analyze] decodes the JPEG frame to a
 * [Bitmap] and attaches it alongside the prompt.
 *
 * Heavy JNI calls (model load + generate) run on [Dispatchers.IO] so they never
 * block the UI thread.
 */
class MediaPipeLocalInferenceEngine(
    private val context: Context
) : LocalInferenceEngine {

    private var llmInference: LlmInference? = null

    override suspend fun load(modelPath: String) {
        try {
            llmInference = withContext(Dispatchers.IO) {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(1024)
                    .build()
                LlmInference.createFromOptions(context, options)
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
        val engine = llmInference
            ?: throw LocalInferenceException(LocalInferenceError.LOAD_FAILED, "Model not loaded.")

        return withContext(Dispatchers.IO) {
            val session = try {
                val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                    .setTopK(40)
                    .setTemperature(0.8f)
                    .setTopP(0.9f)
                    .setGraphOptions(
                        GraphOptions.builder().setEnableVisionModality(true).build()
                    )
                    .build()
                LlmInferenceSession.createFromOptions(engine, sessionOptions)
            } catch (e: Exception) {
                throw LocalInferenceException(
                    LocalInferenceError.LOAD_FAILED,
                    "Failed to create inference session: ${e.message}",
                    e
                )
            }

            try {
                val bitmap = BitmapFactory.decodeByteArray(image.bytes, 0, image.bytes.size)
                    ?: throw LocalInferenceException(
                        LocalInferenceError.INFERENCE_FAILED,
                        "Could not decode the captured frame."
                    )
                val mpImage: MPImage = BitmapImageBuilder(bitmap).build()
                session.addImage(mpImage)
                session.addQueryChunk(prompt)
                session.generateResponse()
            } catch (e: Exception) {
                if (e is LocalInferenceException) throw e
                throw LocalInferenceException(
                    LocalInferenceError.INFERENCE_FAILED,
                    "On-device inference failed: ${e.message}",
                    e
                )
            } finally {
                runCatching { session.close() }
            }
        }
    }

    override fun close() {
        runCatching { llmInference?.close() }
        llmInference = null
    }
}
