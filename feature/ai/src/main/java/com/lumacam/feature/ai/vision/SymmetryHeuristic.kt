package com.lumacam.feature.ai.vision

import kotlin.math.abs

/**
 * Lightweight horizontal-balance heuristic. A well-balanced frame either has its
 * visual mass centered (symmetry) or the subject cleanly on one side; a subject
 * drifting toward — but not reaching — an edge reads as unbalanced. Pure logic.
 */
object SymmetryHeuristic {

    /**
     * Balance score 0..100 for a subject centered at [centerX] (0..1). Perfectly
     * centered (0.5) or on a rule-of-thirds line (~0.33 / ~0.66) scores high;
     * awkward in-between positions score lower.
     */
    fun balanceScore(centerX: Float): Int {
        val sweetSpots = floatArrayOf(1f / 3f, 0.5f, 2f / 3f)
        val nearest = sweetSpots.minOf { abs(centerX - it) }
        // 0.5 is the max distance from any sweet spot to the far edge region.
        val normalized = (1f - nearest / 0.5f).coerceIn(0f, 1f)
        return (normalized * 100f).toInt()
    }

    /** Balance for the whole frame, using the primary subject if present. */
    fun balanceScore(subjects: List<DetectedSubject>): Int {
        val primary = DetectionOutput(subjects).primarySubject() ?: return NEUTRAL
        return balanceScore(primary.box.centerX)
    }

    /** Neutral score used when there is no subject to reason about. */
    const val NEUTRAL = 60
}
