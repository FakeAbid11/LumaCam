package com.lumacam.feature.ai.cloud

/**
 * Builds the instruction sent to a vision model, asking it to return strict JSON
 * that maps 1:1 onto [com.lumacam.feature.ai.CompositionResult]. Keeping the
 * prompt in one place (and unit-testing it) means every provider requests the same
 * structured shape, which the [CompositionJsonMapper] then parses.
 */
object CompositionPromptBuilder {

    /** Exact JSON schema we ask the model to fill in. */
    val jsonSchemaHint: String = """
        {
          "sceneType": "one of: portrait, landscape, food, night, macro, architecture, unknown",
          "compositionScore": "integer 0-100, overall composition quality",
          "suggestedDirection": "one of: up, down, left, right, none",
          "tiltAngle": "number, horizon tilt in degrees, negative = counter-clockwise",
          "lighting": { "label": "short label", "description": "one-line lighting note" },
          "suggestions": ["short actionable coaching tip", "..."]
        }
    """.trimIndent()

    /**
     * Full prompt text. [userContext] is optional extra guidance from the caller
     * (e.g. "this is a food photo") appended when present.
     */
    fun build(userContext: String? = null): String {
        val context = userContext?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { "\n\nAdditional context from the user: $it" } ?: ""
        return """
            You are LumaCam's photography composition coach. Analyze the attached
            photo and assess its composition, framing, scene, and lighting.

            Respond with ONLY a single minified JSON object — no markdown, no code
            fences, no commentary — matching exactly this schema:

            $jsonSchemaHint

            Rules:
            - compositionScore is an integer from 0 to 100.
            - suggestedDirection is the way the photographer should nudge the frame.
            - Keep each suggestion short, kind, and actionable (max 3 suggestions).
            - If unsure of the scene, use "unknown".$context
        """.trimIndent()
    }
}
