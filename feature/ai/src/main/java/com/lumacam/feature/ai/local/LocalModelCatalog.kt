package com.lumacam.feature.ai.local

/**
 * The curated, hard-coded list of local vision models LumaCam supports
 * (PRD §4 Tier 3). Kept intentionally small — small enough to run on
 * constrained phones, sourced from the Hugging Face LiteRT Community in
 * MediaPipe/LiteRT `.litertlm` format.
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
            fileName = "Qwen2-VL-2B.litertlm",
            downloadUrl = "https://huggingface.co/litert-community/Qwen2-VL-2B/" +
                "resolve/main/Qwen2-VL-2B.litertlm"
        )
    )

    /** Returns the spec with the given [id], or null if unknown. */
    fun findById(id: String?): LocalModelSpec? =
        models.firstOrNull { it.id == id }
}
