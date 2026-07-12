package com.lumacam.feature.ai.vision

import com.lumacam.feature.ai.MoveDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class RuleOfThirdsTest {

    @Test
    fun onIntersectionScoresPerfect() {
        assertEquals(100, RuleOfThirds.scorePlacement(1f / 3f, 1f / 3f))
    }

    @Test
    fun deadCenterScoresAboutHalf() {
        assertEquals(50, RuleOfThirds.scorePlacement(0.5f, 0.5f))
    }

    @Test
    fun distanceToNearestIsZeroOnIntersection() {
        assertEquals(0f, RuleOfThirds.distanceToNearest(2f / 3f, 2f / 3f), 1e-4f)
    }

    @Test
    fun onTargetSuggestsNoMove() {
        assertEquals(MoveDirection.NONE, RuleOfThirds.suggestDirection(1f / 3f + 0.01f, 1f / 3f))
    }

    @Test
    fun subjectTooFarLeftIsNudgedRight() {
        assertEquals(MoveDirection.RIGHT, RuleOfThirds.suggestDirection(0.1f, 1f / 3f))
    }

    @Test
    fun subjectTooFarRightIsNudgedLeft() {
        assertEquals(MoveDirection.LEFT, RuleOfThirds.suggestDirection(0.9f, 1f / 3f))
    }

    @Test
    fun subjectTooHighIsNudgedDown() {
        assertEquals(MoveDirection.DOWN, RuleOfThirds.suggestDirection(1f / 3f, 0.1f))
    }

    @Test
    fun subjectTooLowIsNudgedUp() {
        assertEquals(MoveDirection.UP, RuleOfThirds.suggestDirection(1f / 3f, 0.9f))
    }
}
