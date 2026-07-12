package com.lumacam.feature.ai.vision

import com.lumacam.feature.ai.CompositionResult
import com.lumacam.feature.ai.CropBounds
import com.lumacam.feature.ai.MoveDirection
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Aggregates the individual heuristics into the final [CompositionResult]. This
 * is the heart of Luma Vision and is entirely pure, so its scoring is verified by
 * JVM unit tests in CI.
 */
object CompositionScorer {

    // Weights when a subject is present (sum = 1.0).
    private const val W_THIRDS = 0.5f
    private const val W_LEVEL = 0.3f
    private const val W_BALANCE = 0.2f

    // Weights when no subject is detected (sum = 1.0).
    private const val W_LEVEL_NO_SUBJECT = 0.6f
    private const val W_BALANCE_NO_SUBJECT = 0.4f

    private const val PLACEMENT_OK = 70
    private const val CROP_SIZE = 0.8f

    /**
     * Produces a full [CompositionResult] from detector output, device tilt (deg)
     * and average [brightness] (0..1).
     */
    fun score(
        detection: DetectionOutput,
        tiltDegrees: Float,
        brightness: Float
    ): CompositionResult {
        val primary = detection.primarySubject()
        val lighting = LightingAnalyzer.assess(brightness)
        val sceneType = SceneClassifier.classify(
            labels = detection.labels,
            faceCount = detection.faceCount,
            primarySubjectArea = primary?.box?.area ?: 0f,
            brightness = brightness
        )
        val levelScore = TiltMath.levelScore(tiltDegrees)

        val compositionScore: Int
        val direction: MoveDirection
        val placement: Int
        if (primary != null) {
            val cx = primary.box.centerX
            val cy = primary.box.centerY
            placement = RuleOfThirds.scorePlacement(cx, cy)
            val balance = SymmetryHeuristic.balanceScore(cx)
            compositionScore = (
                W_THIRDS * placement + W_LEVEL * levelScore + W_BALANCE * balance
                ).roundToInt().coerceIn(0, 100)
            direction = RuleOfThirds.suggestDirection(cx, cy)
        } else {
            placement = 0
            compositionScore = (
                W_LEVEL_NO_SUBJECT * levelScore +
                    W_BALANCE_NO_SUBJECT * SymmetryHeuristic.NEUTRAL
                ).roundToInt().coerceIn(0, 100)
            direction = MoveDirection.NONE
        }

        return CompositionResult(
            tiltAngle = tiltDegrees,
            compositionScore = compositionScore,
            suggestedDirection = direction,
            sceneType = sceneType,
            lighting = lighting,
            suggestions = buildSuggestions(primary, placement, tiltDegrees, brightness),
            targetCrop = primary?.let { targetCrop(it.box, placement) }
        )
    }

    /** Ordered coaching tips, worst-offender first, capped at three. */
    internal fun buildSuggestions(
        primary: DetectedSubject?,
        placement: Int,
        tiltDegrees: Float,
        brightness: Float
    ): List<String> {
        val tips = mutableListOf<String>()
        if (abs(tiltDegrees) > CompositionResult.LEVEL_THRESHOLD_DEGREES) {
            tips += "Straighten up — the horizon looks tilted."
        }
        if (primary == null) {
            tips += "Point at your subject so LumaCam can help you frame it."
        } else if (placement < PLACEMENT_OK) {
            tips += "Nudge your subject toward a rule-of-thirds line for a stronger frame."
        }
        if (LightingAnalyzer.isLowLight(brightness)) {
            tips += "Low light — brace the phone to avoid blur."
        } else if (brightness > LightingAnalyzer.BRIGHT_MIN) {
            tips += "Very bright — lower exposure to protect the highlights."
        }
        if (tips.isEmpty()) tips += "Nicely framed — hold steady and shoot."
        return tips.take(3)
    }

    /**
     * A gentle crop that repositions the subject onto its nearest rule-of-thirds
     * intersection. Returns null when placement is already strong. The crop keeps
     * a fixed size and is clamped inside the frame.
     */
    internal fun targetCrop(box: NormalizedBox, placement: Int): CropBounds? {
        if (placement >= 90) return null
        val (ix, iy) = RuleOfThirds.nearestIntersection(box.centerX, box.centerY)
        val left = (box.centerX - ix * CROP_SIZE).coerceIn(0f, 1f - CROP_SIZE)
        val top = (box.centerY - iy * CROP_SIZE).coerceIn(0f, 1f - CROP_SIZE)
        return CropBounds(
            left = left,
            top = top,
            right = left + CROP_SIZE,
            bottom = top + CROP_SIZE
        )
    }
}
