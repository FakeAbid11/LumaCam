package com.lumacam.feature.ai.local

/**
 * The curated, hard-coded list of local vision models LumaCam supports
 * (PRD §4 Tier 3). Kept intentionally small (two entries) — small enough to run
 * on constrained phones, sourced from Hugging Face in GGUF format.
 *
 * Model files are never bundled in the APK; users download them at runtime into
 * app-specific storage. This catalog only carries metadata + the download URL.
 */
object LocalModelCatalog {

    val models: List<LocalModelSpec> = listOf(
        LocalModelSpec(
            id = "gemma-3n-e4b-it-int4",
            name = "Gemma 3n 4B (int4)",
            description = "Multimodal vision-language model — best quality, needs ~4 GB RAM.",
            sizeBytes = 4_405_655_031L,
            quantization = "int4",
            minRamMb = 4096,
            fileName = "gemma-3n-E4B-it-int4.task",
            downloadUrl = "https://huggingface.co/google/gemma-3n-E4B-it-litert-preview/" +
                "resolve/main/gemma-3n-E4B-it-int4.task"
        ),
        LocalModelSpec(
            id = "gemma-3n-e2b-it-int4",
            name = "Gemma 3n 2B (int4)",
            description = "Lighter multimodal model — good for mid-range devices (~3 GB).",
            sizeBytes = 3_136_226_711L,
            quantization = "int4",
            minRamMb = 3072,
            fileName = "gemma-3n-E2B-it-int4.task",
            downloadUrl = "https://huggingface.co/google/gemma-3n-E2B-it-litert-preview/" +
                "resolve/main/gemma-3n-E2B-it-int4.task"
        )
    )

    /** Returns the spec with the given [id], or null if unknown. */
    fun findById(id: String?): LocalModelSpec? =
        models.firstOrNull { it.id == id }
}
