package com.example.ui

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameLoopControllerTest {

    @Test
    fun start_ticksUntilStopped() {
        var now = 1_000L
        var ticks = 0
        val controller = GameLoopController(timeProvider = { now })
        val scope = TestScope()
        val dispatcher = StandardTestDispatcher(scope.testScheduler)

        controller.start(
            scope = scope,
            dispatcher = dispatcher,
            targetDelayMillis = { 10L },
            onTick = {
                ticks++
                now += 16L
            }
        )

        scope.runCurrent()
        assertTrue(controller.isRunning)
        assertEquals(1, ticks)

        scope.advanceTimeBy(30L)
        scope.runCurrent()
        assertTrue(ticks >= 3)

        controller.stop()
        assertFalse(controller.isRunning)
        val ticksAfterStop = ticks
        scope.advanceTimeBy(50L)
        scope.runCurrent()
        assertEquals(ticksAfterStop, ticks)
    }
}
