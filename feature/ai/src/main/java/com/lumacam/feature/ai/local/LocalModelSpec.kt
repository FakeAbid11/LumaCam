package com.lumacam.feature.ai.local

/**
 * Curated metadata describing a single downloadable on-device vision model
 * (PRD §4 Tier 3 — Local AI Model). This is pure data with no Android or native
 * dependency, so it is fully JVM-unit-testable.
 *
 * @param id stable identifier used for storage keys and selection.
 * @param name human-readable label shown in Settings.
 * @param description one-line summary of the model.
 * @param sizeBytes advisory download size; the real size comes from the server's
 *   `Content-Length` at download time and is only used for storage pre-checks/UI.
 * @param quantization quantization format (e.g. "Q8_0", "Q4_K_M").
 * @param minRamMb recommended minimum device RAM to run the model comfortably.
 * @param fileName the on-disk file name once downloaded.
 * @param downloadUrl direct URL to the model file (e.g. Hugging Face `resolve`).
 */
data class LocalModelSpec(
    val id: String,
    val name: String,
    val description: String,
    val sizeBytes: Long,
    val quantization: String,
    val minRamMb: Int,
    val fileName: String,
    val downloadUrl: String
) {
    /** Human-friendly download size, e.g. "531 MB". */
    val formattedSize: String get() = formatBytes(sizeBytes)

    /** Human-friendly RAM recommendation, e.g. "2 GB". */
    val formattedMinRam: String
        get() = if (minRamMb >= 1024 && minRamMb % 1024 == 0) {
            "${minRamMb / 1024} GB"
        } else if (minRamMb >= 1024) {
            String.format("%.1f GB", minRamMb / 1024.0)
        } else {
            "$minRamMb MB"
        }
}

/** Formats a byte count into a compact, human-readable string (KB/MB/GB). */
fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "—"
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.0f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.0f MB", mb)
    val gb = mb / 1024.0
    return String.format("%.1f GB", gb)
}
