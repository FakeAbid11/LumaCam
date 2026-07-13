package com.lumacam.feature.ai.local

import com.lumacam.feature.ai.cloud.CompositionJsonMapper
import com.lumacam.feature.ai.cloud.CompositionPromptBuilder
import java.io.File

/**
 * Default [LocalAiProvider] that orchestrates the on-device path end-to-end while
 * reusing the shared [CompositionPromptBuilder] + [CompositionJsonMapper] so the
 * output normalizes to the exact same [com.lumacam.feature.ai.CompositionResult]
 * as Luma Vision and Cloud AI.
 *
 * It is deliberately defensive: it validates that a model is selected and present,
 * catches [OutOfMemoryError] and [LocalInferenceException] from the native seam,
 * and always returns a typed [LocalAiOutcome] — never throws.
 *
 * [activeModel] and [fileExists] are injected so the whole flow is JVM-unit-testable
 * with a fake [LocalInferenceEngine] and no real filesystem or native runtime.
 *
 * @param engine the (native) inference seam — [LiteRtLocalInferenceEngine]
 *   when running on a device.
 * @param activeModel supplies the currently-selected downloaded model, or null.
 * @param fileExists checks whether a model file is present on disk.
 */
class DefaultLocalAiProvider(
    private val engine: LocalInferenceEngine,
    private val activeModel: () -> ActiveLocalModel?,
    private val fileExists: (String) -> Boolean = { File(it).exists() }
) : LocalAiProvider {

    override suspend fun analyze(image: LocalImage, context: String?): LocalAiOutcome {
        val active = activeModel() ?: return LocalAiOutcome.Failure(LocalAiError.NoModelSelected)

        if (!fileExists(active.filePath)) {
            return LocalAiOutcome.Failure(LocalAiError.ModelNotDownloaded(active.spec.name))
        }

        return try {
            engine.load(active.filePath, active.spec.multimodal)
            val prompt = CompositionPromptBuilder.build(context)
            val raw = engine.analyze(image, prompt)
            val result = CompositionJsonMapper.parse(raw)
                ?: return LocalAiOutcome.Failure(
                    LocalAiError.InferenceFailed("Unparseable model output.")
                )
            LocalAiOutcome.Success(result)
        } catch (oom: OutOfMemoryError) {
            // Memory safety: never let an OOM crash the app — surface it clearly.
            LocalAiOutcome.Failure(LocalAiError.OutOfMemory)
        } catch (e: LocalInferenceException) {
            LocalAiOutcome.Failure(mapEngineError(e))
        } catch (e: Exception) {
            LocalAiOutcome.Failure(LocalAiError.Unknown(e.message ?: e.javaClass.simpleName))
        } finally {
            runCatching { engine.close() }
        }
    }

    private fun mapEngineError(e: LocalInferenceException): LocalAiError = when (e.error) {
        LocalInferenceError.RUNTIME_UNAVAILABLE -> LocalAiError.RuntimeUnavailable
        LocalInferenceError.OUT_OF_MEMORY -> LocalAiError.OutOfMemory
        LocalInferenceError.LOAD_FAILED ->
            LocalAiError.InferenceFailed(e.message ?: "Model failed to load.")
        LocalInferenceError.INFERENCE_FAILED ->
            LocalAiError.InferenceFailed(e.message ?: "Inference failed.")
    }
}
