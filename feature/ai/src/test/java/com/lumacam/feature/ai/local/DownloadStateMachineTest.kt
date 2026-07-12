package com.lumacam.feature.ai.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadStateMachineTest {

    @Test
    fun `start from idle checks space`() {
        assertEquals(
            DownloadState.CheckingSpace,
            DownloadStateMachine.reduce(DownloadState.Idle, DownloadEvent.Start)
        )
    }

    @Test
    fun `space confirmed begins downloading`() {
        val next = DownloadStateMachine.reduce(DownloadState.CheckingSpace, DownloadEvent.SpaceConfirmed)
        assertTrue(next is DownloadState.Downloading)
    }

    @Test
    fun `progress updates bytes`() {
        val start = DownloadState.Downloading(0, 100)
        val next = DownloadStateMachine.reduce(start, DownloadEvent.Progress(50, 100))
        assertEquals(DownloadState.Downloading(50, 100), next)
    }

    @Test
    fun `finish from downloading completes`() {
        val next = DownloadStateMachine.reduce(DownloadState.Downloading(100, 100), DownloadEvent.Finish)
        assertEquals(DownloadState.Completed, next)
    }

    @Test
    fun `fail from downloading marks failed`() {
        val next = DownloadStateMachine.reduce(DownloadState.Downloading(10, 100), DownloadEvent.Fail("net"))
        assertEquals(DownloadState.Failed("net"), next)
    }

    @Test
    fun `resume from failed rechecks space`() {
        val next = DownloadStateMachine.reduce(DownloadState.Failed("net"), DownloadEvent.Resume)
        assertEquals(DownloadState.CheckingSpace, next)
    }

    @Test
    fun `cancel always returns to idle`() {
        assertEquals(DownloadState.Idle, DownloadStateMachine.reduce(DownloadState.Downloading(5, 9), DownloadEvent.Cancel))
        assertEquals(DownloadState.Idle, DownloadStateMachine.reduce(DownloadState.CheckingSpace, DownloadEvent.Cancel))
    }

    @Test
    fun `invalid transitions are no-ops`() {
        // Finish while idle is meaningless — stay idle.
        assertEquals(DownloadState.Idle, DownloadStateMachine.reduce(DownloadState.Idle, DownloadEvent.Finish))
        // Progress while completed is ignored.
        assertEquals(DownloadState.Completed, DownloadStateMachine.reduce(DownloadState.Completed, DownloadEvent.Progress(1, 2)))
    }

    @Test
    fun `downloading fraction and percent`() {
        val d = DownloadState.Downloading(25, 100)
        assertEquals(0.25f, d.fraction)
        assertEquals(25, d.percent)
    }

    @Test
    fun `unknown total size yields null fraction`() {
        val d = DownloadState.Downloading(25, 0)
        assertNull(d.fraction)
        assertNull(d.percent)
    }
}
