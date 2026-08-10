package com.example.engine

import com.example.model.AlertState
import com.example.model.Enemy
import com.example.model.Point3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EnemyAiStateMachineTest {

    private lateinit var sampleEnemy: Enemy
    private lateinit var stateMachine: EnemyAiStateMachine

    @Before
    fun setUp() {
        sampleEnemy = Enemy(
            id = "enemy_1",
            name = "Syntrob Scout",
            pos = Point3D(0f, 0f, 1f),
            alertState = AlertState.PATROLLING,
            alertMeter = 0f
        )
        stateMachine = EnemyAiStateMachine(initialState = IdleState(maxIdleTime = 2f))
    }

    @Test
    fun initialState_isIdle() {
        assertTrue(stateMachine.currentState is IdleState)
        assertEquals("IDLE", stateMachine.currentState.name)
        assertEquals(AlertState.PATROLLING, stateMachine.currentState.alertState)
    }

    @Test
    fun idleState_transitionsToPatrollingWhenTimerExpires() {
        val waypoints = listOf(Point3D(0f, 0f, 1f), Point3D(10f, 0f, 1f))
        val context = EnemyAiContext(
            enemy = sampleEnemy,
            playerPos = Point3D(50f, 50f, 1f), // Far away
            playerStealthLevel = 1.0f,
            hasLineOfSight = false,
            isPlayerInCover = true,
            patrolWaypoints = waypoints
        )

        // Advance 2.5 seconds (idle max time is 2f)
        stateMachine.tick(context, dt = 2.5f)

        assertTrue(stateMachine.currentState is PatrollingState)
        assertEquals("PATROLLING", stateMachine.currentState.name)
    }

    @Test
    fun lineOfSight_triggersSuspiciousStateWhenMeterFills() {
        val context = EnemyAiContext(
            enemy = sampleEnemy,
            playerPos = Point3D(3f, 0f, 1f), // Close to enemy
            playerStealthLevel = 0.2f, // Low stealth
            hasLineOfSight = true,
            isPlayerInCover = false
        )

        // Tick to increase alert meter
        stateMachine.tick(context, dt = 1.0f)

        assertTrue(stateMachine.currentState is SuspiciousState || stateMachine.currentState is AlertedState)
        assertTrue(sampleEnemy.alertMeter > 0f)
    }

    @Test
    fun lineOfSight_triggersAlertedStateWhenMeterHits100() {
        val context = EnemyAiContext(
            enemy = sampleEnemy,
            playerPos = Point3D(1f, 0f, 1f), // Very close
            playerStealthLevel = 0.0f, // Completely exposed
            hasLineOfSight = true,
            isPlayerInCover = false
        )

        // Force tick until alert meter maxes out
        for (i in 0..10) {
            stateMachine.tick(context, dt = 0.5f)
        }

        assertTrue(stateMachine.currentState is AlertedState)
        assertEquals(AlertState.ALERTED, sampleEnemy.alertState)
        assertEquals(100f, sampleEnemy.alertMeter)
    }

    @Test
    fun soundDisturbance_transitionsToSuspiciousState() {
        val noisePos = Point3D(5f, 5f, 1f)
        val context = EnemyAiContext(
            enemy = sampleEnemy,
            playerPos = Point3D(20f, 20f, 1f),
            playerStealthLevel = 0.8f,
            hasLineOfSight = false,
            isPlayerInCover = true,
            noiseLocation = noisePos
        )

        stateMachine.tick(context, dt = 0.1f)

        assertTrue(stateMachine.currentState is SuspiciousState)
        val suspiciousState = stateMachine.currentState as SuspiciousState
        assertEquals(noisePos, suspiciousState.investigationTarget)
    }

    @Test
    fun alertedState_transitionsToSearchingWhenTargetLost() {
        val alertedState = AlertedState(lastKnownPlayerPos = Point3D(10f, 10f, 1f))
        stateMachine = EnemyAiStateMachine(initialState = alertedState)

        val context = EnemyAiContext(
            enemy = sampleEnemy,
            playerPos = Point3D(50f, 50f, 1f),
            playerStealthLevel = 1.0f,
            hasLineOfSight = false, // Target lost!
            isPlayerInCover = true
        )

        // Tick for > 4.0s lost target duration
        stateMachine.tick(context, dt = 4.5f)

        assertTrue(stateMachine.currentState is SearchingState)
        assertEquals(AlertState.SUSPICIOUS, sampleEnemy.alertState)
    }
}
