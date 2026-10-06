package com.example.model

import com.example.engine.CivilianNpcSystem
import com.example.engine.LevelManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CivilianNpcSystemTest {

    @Test
    fun mainStreetSpawnsAmbientPopulation() {
        val civilians = CivilianNpcSystem(LevelManager(35)).spawnMainStreet()

        assertEquals(7, civilians.size)
        assertEquals(1, civilians.count { it.activity == CivilianActivity.VENDOR })
        assertEquals(6, civilians.count { it.isMoving })
    }

    @Test
    fun updateMovesWalkingCivilianAlongRoute() {
        val system = CivilianNpcSystem(LevelManager(35))
        val civilian = system.spawnMainStreet().first()
        val startX = civilian.pos.x

        system.update(listOf(civilian), 1f)

        assertTrue(civilian.pos.x != startX || civilian.pos.y != 17f)
    }

    @Test
    fun vendorRemainsStationary() {
        val vendor = CivilianNpcSystem(LevelManager(35)).spawnMainStreet().last()
        val before = vendor.pos.copy()

        CivilianNpcSystem(LevelManager(35)).update(listOf(vendor), 10f)

        assertEquals(before, vendor.pos)
        assertEquals(CivilianActivity.VENDOR, vendor.activity)
    }
}
