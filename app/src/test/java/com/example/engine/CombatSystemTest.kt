package com.example.engine

import com.example.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CombatSystemTest {

    private lateinit var levelManager: LevelManager
    private lateinit var combatSystem: CombatSystem

    @Before
    fun setUp() {
        levelManager = LevelManager(mapSize = 35)
        combatSystem = CombatSystem(levelManager)
    }

    @Test
    fun executeAttack_consumesEnergyAndDamagesEnemy() {
        val player = Player(
            pos = Point3D(5f, 5f, 1f),
            energy = 50f,
            isSneaking = false
        )
        val enemy = Enemy(
            id = "e1",
            pos = Point3D(5.5f, 5.5f, 1f),
            health = 100f,
            maxHealth = 100f
        )
        val projectiles = mutableListOf<Pair<Point3D, Point3D>>()
        val noiseRipples = mutableListOf<NoiseRipple>()

        combatSystem.executeAttack(
            player = player,
            enemies = listOf(enemy),
            currentZLevel = 1,
            lastMoveX = 1f,
            lastMoveY = 0f,
            activeProjectiles = projectiles,
            noiseRipples = noiseRipples
        )

        assertEquals(38f, player.energy, 0.01f) // 50 - 12
        assertTrue(enemy.health < 100f)
        assertEquals(AlertState.ALERTED, enemy.alertState)
        assertTrue(noiseRipples.isNotEmpty())
    }

    @Test
    fun executeAttack_insufficientEnergy_aborts() {
        val player = Player(
            pos = Point3D(5f, 5f, 1f),
            energy = 5f
        )
        val enemy = Enemy(
            id = "e1",
            pos = Point3D(5.5f, 5.5f, 1f),
            health = 100f
        )
        val projectiles = mutableListOf<Pair<Point3D, Point3D>>()
        val noiseRipples = mutableListOf<NoiseRipple>()

        combatSystem.executeAttack(
            player = player,
            enemies = listOf(enemy),
            currentZLevel = 1,
            lastMoveX = 1f,
            lastMoveY = 0f,
            activeProjectiles = projectiles,
            noiseRipples = noiseRipples
        )

        assertEquals(5f, player.energy, 0.01f)
        assertEquals(100f, enemy.health, 0.01f)
    }

    @Test
    fun detonateExplosionAt_damagesEnemiesAndPlayerInRadius() {
        val player = Player(
            pos = Point3D(10f, 10f, 1f),
            health = 100f
        )
        val enemy = Enemy(
            id = "e1",
            pos = Point3D(10.5f, 10.5f, 1f),
            health = 100f
        )

        combatSystem.detonateExplosionAt(
            pos = Point3D(10f, 10f, 1f),
            enemies = listOf(enemy),
            player = player
        )

        assertEquals(60f, player.health, 0.01f) // 100 - 40
        assertEquals(20f, enemy.health, 0.01f) // 100 - 80
        assertEquals(AlertState.ALERTED, enemy.alertState)
    }
}
