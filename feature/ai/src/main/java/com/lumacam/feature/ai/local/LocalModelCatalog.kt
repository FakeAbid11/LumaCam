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
            id = "smolvlm-256m-instruct-q8",
            name = "SmolVLM 256M Instruct",
            description = "Tiny, fast vision model — best for low-RAM devices.",
            sizeBytes = 290_000_000L,
            quantization = "Q8_0",
            minRamMb = 2048,
            fileName = "SmolVLM-256M-Instruct-Q8_0.gguf",
            downloadUrl = "https://huggingface.co/ggml-org/SmolVLM-256M-Instruct-GGUF/" +
                "resolve/main/SmolVLM-256M-Instruct-Q8_0.gguf"
        ),
        LocalModelSpec(
            id = "smolvlm-500m-instruct-q8",
            name = "SmolVLM 500M Instruct",
            description = "Small vision model with stronger scene understanding.",
            sizeBytes = 540_000_000L,
            quantization = "Q8_0",
            minRamMb = 3072,
            fileName = "SmolVLM-500M-Instruct-Q8_0.gguf",
            downloadUrl = "https://huggingface.co/ggml-org/SmolVLM-500M-Instruct-GGUF/" +
                "resolve/main/SmolVLM-500M-Instruct-Q8_0.gguf"
        )
    )

    /** Returns the spec with the given [id], or null if unknown. */
    fun findById(id: String?): LocalModelSpec? =
        models.firstOrNull { it.id == id }
}
