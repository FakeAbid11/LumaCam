package com.lumacam.feature.ai.local

/**
 * The curated, hard-coded list of local models LumaCam supports (PRD §4 Tier 3).
 * Kept intentionally small — small enough to run on constrained phones, sourced
 * from the Hugging Face LiteRT Community in LiteRT-LM `.litertlm` format.
 *
 * Every entry here is run by [com.lumacam.feature.ai.local.LiteRtLocalInferenceEngine]
 * (Google's LiteRT-LM runtime). Models flagged [LocalModelSpec.multimodal] = true
 * accept a captured frame; the rest are text-only.
 *
 * Model files are never bundled in the APK; users download them at runtime into
 * app-specific storage. This catalog only carries metadata + the download URL.
 */
object LocalModelCatalog {

    val models: List<LocalModelSpec> = listOf(
        LocalModelSpec(
            id = "qwen2-vl-2b-instruct",
            name = "Qwen2-VL 2B (int4)",
            description = "Multimodal vision-language model — on-device scene analysis, needs ~3 GB RAM.",
            sizeBytes = 2_400_000_000L, // ~2.4 GB LiteRT int4 (confirm exact size; model may be gated)
            quantization = "int4",
            minRamMb = 3072,
            multimodal = true,
            fileName = "Qwen2-VL-2B.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/Qwen2-VL-2B/" +
                "resolve/main/Qwen2-VL-2B.litertlm"
        ),
        LocalModelSpec(
            id = "minicpm5-1b",
            name = "MiniCPM5 1B (int8)",
            description = "Compact text model from the MiniCPM family — fast on-device chat, no vision.",
            sizeBytes = 1_200_000_000L, // ~1.2 GB LiteRT int8 (estimate)
            quantization = "int8",
            minRamMb = 2048,
            multimodal = false,
            fileName = "minicpm_dynamic_wi8_afp32_gpu_opt.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/MiniCPM5-1B/" +
                "resolve/main/minicpm_dynamic_wi8_afp32_gpu_opt.litertlm"
        )
    )

    /** Returns the spec with the given [id], or null if unknown. */
    fun findById(id: String?): LocalModelSpec? =
        models.firstOrNull { it.id == id }
}
