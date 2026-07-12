package com.lumacam.feature.ai.vision

import com.lumacam.feature.ai.MoveDirection
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Rule-of-thirds scoring and recomposition hints. All coordinates are normalized
 * (0..1). Pure logic — no Android — so it is unit-testable in CI.
 */
object RuleOfThirds {

    /** The four rule-of-thirds intersection points. */
    val intersections: List<Pair<Float, Float>> = listOf(
        1f / 3f to 1f / 3f,
        2f / 3f to 1f / 3f,
        1f / 3f to 2f / 3f,
        2f / 3f to 2f / 3f
    )

    /** Worst-case distance: a frame corner to its nearest intersection. */
    private val MAX_DISTANCE = sqrt(2f) / 3f

    /** Subjects within this distance of an intersection are "on target". */
    const val DEAD_ZONE = 0.05f

    /** The intersection closest to ([x], [y]). */
    fun nearestIntersection(x: Float, y: Float): Pair<Float, Float> =
        intersections.minByOrNull { (ix, iy) -> hypot(x - ix, y - iy) }!!

    /** Distance from ([x], [y]) to the nearest intersection. */
    fun distanceToNearest(x: Float, y: Float): Float {
        val (ix, iy) = nearestIntersection(x, y)
        return hypot(x - ix, y - iy)
    }

    /** Placement quality 0..100 (100 = exactly on an intersection). */
    fun scorePlacement(x: Float, y: Float): Int {
        val d = distanceToNearest(x, y)
        val normalized = (1f - d / MAX_DISTANCE).coerceIn(0f, 1f)
        return (normalized * 100f).toInt()
    }

    /**
     * Direction to recompose the subject at ([x], [y]) so it moves toward the
     * nearest intersection. Returns [MoveDirection.NONE] when already on target.
     * The dominant axis wins; the arrow points where the subject should go.
     */
    fun suggestDirection(x: Float, y: Float): MoveDirection {
        val (ix, iy) = nearestIntersection(x, y)
        val dx = ix - x
        val dy = iy - y
        if (hypot(dx, dy) < DEAD_ZONE) return MoveDirection.NONE
        return if (abs(dx) >= abs(dy)) {
            if (dx > 0f) MoveDirection.RIGHT else MoveDirection.LEFT
        } else {
            if (dy > 0f) MoveDirection.DOWN else MoveDirection.UP
        }
    }
}
