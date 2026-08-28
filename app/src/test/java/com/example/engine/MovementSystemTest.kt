package com.example.engine

import com.example.model.NoiseRipple
import com.example.model.Player
import com.example.model.Point3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MovementSystemTest {

    private lateinit var levelManager: LevelManager
    private lateinit var movementSystem: MovementSystem

    @Before
    fun setUp() {
        levelManager = LevelManager(mapSize = 35)
        movementSystem = MovementSystem(levelManager)
    }

    @Test
    fun checkCollisionAt_outOfBounds_returnsTrue() {
        assertTrue(movementSystem.checkCollisionAt(-1f, 5f, 1))
        assertTrue(movementSystem.checkCollisionAt(5f, -1f, 1))
        assertTrue(movementSystem.checkCollisionAt(36f, 5f, 1))
        assertTrue(movementSystem.checkCollisionAt(5f, 36f, 1))
    }

    @Test
    fun checkCollisionAt_walkableFloor_returnsFalse() {
        // Z=1 (17, 17) is GRID_ROAD
        assertFalse(movementSystem.checkCollisionAt(17.5f, 17.5f, 1))
    }

    @Test
    fun processPlayerMove_updatesPlayerCoordinatesAndDirection() {
        val player = Player(pos = Point3D(17.0f, 17.0f, 1f))
        val ripples = mutableListOf<NoiseRipple>()

        val result = movementSystem.processPlayerMove(
            player = player,
            currentZLevel = 1,
            dx = 0.5f,
            dy = 0.0f,
            noiseRipples = ripples
        )

        assertTrue(result.moved)
        assertEquals(1.0f, result.lastMoveX, 0.01f)
        assertEquals(0.0f, result.lastMoveY, 0.01f)
        assertTrue(player.pos.x > 17.0f)
    }
}
