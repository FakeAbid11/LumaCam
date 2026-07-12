package com.lumacam.feature.ai.cloud

import com.lumacam.feature.ai.CompositionResult
import com.lumacam.feature.ai.CropBounds
import com.lumacam.feature.ai.LightingAssessment
import com.lumacam.feature.ai.MoveDirection
import com.lumacam.feature.ai.SceneType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Parses a vision model's textual reply into a normalized [CompositionResult].
 *
 * This is the heart of "every provider normalizes to the same shape" and is fully
 * JVM-unit-tested. It is defensive by design: it extracts the JSON object even when
 * wrapped in prose or ```json fences, tolerates missing/extra fields, clamps and
 * canonicalizes values, and NEVER throws — malformed input yields null so the
 * provider can surface [CloudAiError.MalformedResponse].
 */
object CompositionJsonMapper {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Serializable
    private data class Dto(
        @SerialName("sceneType") val sceneType: String? = null,
        @SerialName("compositionScore") val compositionScore: Double? = null,
        @SerialName("suggestedDirection") val suggestedDirection: String? = null,
        @SerialName("tiltAngle") val tiltAngle: Double? = null,
        @SerialName("lighting") val lighting: LightingDto? = null,
        @SerialName("suggestions") val suggestions: List<String>? = null,
        @SerialName("targetCrop") val targetCrop: CropDto? = null
    )

    @Serializable
    private data class LightingDto(
        @SerialName("label") val label: String? = null,
        @SerialName("description") val description: String? = null
    )

    @Serializable
    private data class CropDto(
        @SerialName("left") val left: Float? = null,
        @SerialName("top") val top: Float? = null,
        @SerialName("right") val right: Float? = null,
        @SerialName("bottom") val bottom: Float? = null
    )

    /** Parses [rawContent]; returns null if no usable JSON could be recovered. */
    fun parse(rawContent: String?): CompositionResult? {
        val jsonText = extractJsonObject(rawContent) ?: return null
        val dto = runCatching { json.decodeFromString<Dto>(jsonText) }.getOrNull() ?: return null
        return CompositionResult(
            tiltAngle = dto.tiltAngle?.toFloat() ?: 0f,
            compositionScore = normalizeScore(dto.compositionScore),
            suggestedDirection = normalizeDirection(dto.suggestedDirection),
            sceneType = normalizeScene(dto.sceneType),
            lighting = normalizeLighting(dto.lighting),
            suggestions = normalizeSuggestions(dto.suggestions),
            targetCrop = normalizeCrop(dto.targetCrop)
        )
    }

    /**
     * Recovers the first balanced JSON object from arbitrary model output: strips
     * code fences, then scans for the outermost `{ ... }` pair. Returns null when
     * no plausible object is present.
     */
    internal fun extractJsonObject(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val start = raw.indexOf('{')
        if (start < 0) return null
        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until raw.length) {
            val c = raw[i]
            when {
                escaped -> escaped = false
                c == '\\' && inString -> escaped = true
                c == '"' -> inString = !inString
                !inString && c == '{' -> depth++
                !inString && c == '}' -> {
                    depth--
                    if (depth == 0) return raw.substring(start, i + 1)
                }
            }
        }
        return null
    }

    private fun normalizeScore(raw: Double?): Int =
        (raw ?: 0.0).toInt().coerceIn(0, 100)

    internal fun normalizeDirection(raw: String?): MoveDirection =
        when (raw?.trim()?.lowercase()) {
            "up", "top", "upward" -> MoveDirection.UP
            "down", "bottom", "downward" -> MoveDirection.DOWN
            "left" -> MoveDirection.LEFT
            "right" -> MoveDirection.RIGHT
            else -> MoveDirection.NONE
        }

    internal fun normalizeScene(raw: String?): SceneType =
        when (raw?.trim()?.lowercase()) {
            "portrait", "person", "people", "face" -> SceneType.PORTRAIT
            "landscape", "nature", "scenery", "outdoor" -> SceneType.LANDSCAPE
            "food", "meal", "dish" -> SceneType.FOOD
            "night", "lowlight", "low-light", "dark" -> SceneType.NIGHT
            "macro", "closeup", "close-up" -> SceneType.MACRO
            "architecture", "building", "urban", "city" -> SceneType.ARCHITECTURE
            else -> SceneType.UNKNOWN
        }

    private fun normalizeLighting(dto: LightingDto?): LightingAssessment {
        val label = dto?.label?.trim().orEmpty().ifBlank { "Lighting" }
        val description = dto?.description?.trim().orEmpty()
            .ifBlank { "No specific lighting notes." }
        return LightingAssessment(label = label, description = description)
    }

    private fun normalizeSuggestions(raw: List<String>?): List<String> =
        raw.orEmpty()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(3)

    private fun normalizeCrop(dto: CropDto?): CropBounds? {
        if (dto?.left == null || dto.top == null || dto.right == null || dto.bottom == null) {
            return null
        }
        val left = dto.left.coerceIn(0f, 1f)
        val top = dto.top.coerceIn(0f, 1f)
        val right = dto.right.coerceIn(0f, 1f)
        val bottom = dto.bottom.coerceIn(0f, 1f)
        if (right <= left || bottom <= top) return null
        return CropBounds(left = left, top = top, right = right, bottom = bottom)
    }
}
