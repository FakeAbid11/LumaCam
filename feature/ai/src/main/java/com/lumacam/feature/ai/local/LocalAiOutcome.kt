package com.lumacam.feature.ai.local

import com.lumacam.feature.ai.CompositionResult

/**
 * Result of a Local AI (on-device model) analysis: either a normalized
 * [CompositionResult] — the same type Luma Vision and Cloud AI produce — or a
 * typed [LocalAiError]. Providers always return this and never throw.
 */
sealed interface LocalAiOutcome {
    data class Success(val result: CompositionResult) : LocalAiOutcome
    data class Failure(val error: LocalAiError) : LocalAiOutcome
}

/**
 * A single image to analyze on-device, as already-encoded bytes plus a MIME type.
 * Vendor-neutral (no Android [android.graphics.Bitmap]) so the provider layer stays
 * JVM-unit-testable.
 */
class LocalImage(
    val bytes: ByteArray,
    val mimeType: String = "image/jpeg"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LocalImage) return false
        return mimeType == other.mimeType && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int = 31 * bytes.contentHashCode() + mimeType.hashCode()
}
