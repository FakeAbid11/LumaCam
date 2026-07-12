package com.lumacam.feature.ai.vision

import org.junit.Assert.assertEquals
import org.junit.Test

class TiltMathTest {

    @Test
    fun uprightPortraitIsZeroDegrees() {
        assertEquals(0f, TiltMath.rollDegrees(0f, -9.8f), 1e-3f)
    }

    @Test
    fun clockwiseNinetyDegrees() {
        assertEquals(90f, TiltMath.rollDegrees(9.8f, 0f), 1e-3f)
    }

    @Test
    fun counterClockwiseNinetyDegrees() {
        assertEquals(-90f, TiltMath.rollDegrees(-9.8f, 0f), 1e-3f)
    }

    @Test
    fun zeroVectorIsZero() {
        assertEquals(0f, TiltMath.rollDegrees(0f, 0f), 1e-3f)
    }

    @Test
    fun levelFrameScoresFull() {
        assertEquals(100, TiltMath.levelScore(0f))
    }

    @Test
    fun maxTiltScoresZero() {
        assertEquals(0, TiltMath.levelScore(10f))
        assertEquals(0, TiltMath.levelScore(25f))
    }

    @Test
    fun halfTiltScoresHalf() {
        assertEquals(50, TiltMath.levelScore(5f))
    }

    @Test
    fun tiltSignIsIgnored() {
        assertEquals(TiltMath.levelScore(4f), TiltMath.levelScore(-4f))
    }
}
