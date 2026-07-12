package com.lumacam.feature.ai.vision

import org.junit.Assert.assertEquals
import org.junit.Test

class SymmetryHeuristicTest {

    @Test
    fun centeredScoresFull() {
        assertEquals(100, SymmetryHeuristic.balanceScore(0.5f))
    }

    @Test
    fun onThirdScoresFull() {
        assertEquals(100, SymmetryHeuristic.balanceScore(1f / 3f))
    }

    @Test
    fun edgeScoresLow() {
        assertEquals(33, SymmetryHeuristic.balanceScore(0f))
    }

    @Test
    fun noSubjectIsNeutral() {
        assertEquals(SymmetryHeuristic.NEUTRAL, SymmetryHeuristic.balanceScore(emptyList()))
    }
}
