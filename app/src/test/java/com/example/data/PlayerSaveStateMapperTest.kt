package com.example.data

import com.example.model.EquipmentItem
import com.example.model.Inventory
import com.example.model.Player
import com.example.model.Point3D
import com.example.model.Quest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerSaveStateMapperTest {

    @Test
    fun toSaveState_capturesRuntimeProgressionAndRunState() {
        val player = Player(
            pos = Point3D(7.5f, 9.25f, 2f),
            health = 64f,
            energy = 31f,
            xp = 450,
            level = 4,
            skillPoints = 3,
            credits = 275,
            equippedWeapon = EquipmentItem.DEFAULT_WEAPON,
            equippedCore = EquipmentItem.DEFAULT_CORE,
            equippedSystem = EquipmentItem.DEFAULT_SYSTEM,
            unlockedSkills = setOf("ronin_base", "ronin_speed"),
            quest = Quest(
                id = "data_heist",
                title = "Extract Black ICE",
                description = "Pull a hostile payload from the grid.",
                currentProgress = 2,
                targetCount = 3,
                isCompleted = false
            ),
            inventory = Inventory(
                ownedEquipmentIds = setOf("nano_blade", "force_shield", "targeting_chip", "quiet_soles"),
                healthPacks = 1,
                energyCells = 4
            )
        )

        val save = PlayerSaveStateMapper.toSaveState(
            player = player,
            currentZLevel = 2,
            currentScore = 900,
            exploredTiles = mapOf(2 to setOf("7,9", "8,9")),
            timestamp = 1234L
        )

        assertEquals(4, save.level)
        assertEquals(450, save.xp)
        assertEquals(3, save.skillPoints)
        assertEquals(275, save.credits)
        assertEquals("nano_blade", save.equippedWeaponId)
        assertEquals("force_shield", save.equippedCoreId)
        assertEquals("targeting_chip", save.equippedSystemId)
        assertEquals("ronin_base,ronin_speed", save.unlockedSkillIdsString)
        assertEquals(900, save.highScore)
        assertEquals(2, save.currentZLevel)
        assertEquals(7.5f, save.playerPosX, 0.0f)
        assertEquals(9.25f, save.playerPosY, 0.0f)
        assertEquals("2:7,9;8,9", save.exploredTilesString)
        assertEquals("data_heist", save.questId)
        assertEquals(2, save.questProgress)
        assertEquals("nano_blade,force_shield,targeting_chip,quiet_soles", save.inventoryEquipmentIdsString)
        assertEquals(1234L, save.timestamp)
    }

    @Test
    fun restore_rebuildsPlayerAndSkillTreeFromSaveState() {
        val save = PlayerSaveState(
            level = 6,
            xp = 1100,
            skillPoints = 5,
            credits = 720,
            equippedWeaponId = "plasma_carbine",
            equippedCoreId = "ghost_cloak",
            equippedSystemId = "quiet_soles",
            unlockedSkillIdsString = "ronin_base,ronin_speed,ghost_base",
            currentZLevel = 3,
            playerPosX = 11f,
            playerPosY = 12f,
            playerHealth = 999f,
            playerEnergy = 999f,
            exploredTilesString = "3:11,12;11,13",
            questId = "uplink_extract",
            questTitle = "Extract Uplink",
            questDescription = "Reach the receiver.",
            questProgress = 3,
            questTargetCount = 3,
            questCompleted = true,
            inventoryEquipmentIdsString = "nano_blade,plasma_carbine,ghost_cloak,quiet_soles",
            inventoryHealthPacks = 2,
            inventoryEnergyCells = 1
        )

        val restored = PlayerSaveStateMapper.restore(save)

        assertEquals(3, restored.currentZLevel)
        assertEquals(Point3D(11f, 12f, 3f), restored.player.pos)
        assertEquals(6, restored.player.level)
        assertEquals("plasma_carbine", restored.player.equippedWeapon.id)
        assertEquals("ghost_cloak", restored.player.equippedCore.id)
        assertEquals("quiet_soles", restored.player.equippedSystem.id)
        assertEquals(setOf("ronin_base", "ronin_speed", "ghost_base"), restored.player.unlockedSkills)
        assertTrue(restored.player.health <= restored.player.maxHealth)
        assertTrue(restored.player.energy <= restored.player.maxEnergy)
        assertEquals("uplink_extract", restored.player.quest.id)
        assertTrue(restored.player.quest.isCompleted)
        assertEquals(setOf("11,12", "11,13"), restored.exploredTiles[3])
        assertTrue(restored.skillNodes.first { it.id == "ronin_speed" }.isUnlocked)
        assertTrue(restored.skillNodes.first { it.id == "ghost_base" }.isUnlocked)
    }

    @Test
    fun restore_usesFallbackEquipmentAndDefaultInventoryWhenIdsAreMissing() {
        val save = PlayerSaveState(
            equippedWeaponId = "missing_weapon",
            equippedCoreId = "missing_core",
            equippedSystemId = "missing_system",
            inventoryEquipmentIdsString = ""
        )

        val restored = PlayerSaveStateMapper.restore(save)

        assertEquals(EquipmentItem.DEFAULT_WEAPON.id, restored.player.equippedWeapon.id)
        assertEquals(EquipmentItem.DEFAULT_CORE.id, restored.player.equippedCore.id)
        assertEquals(EquipmentItem.DEFAULT_SYSTEM.id, restored.player.equippedSystem.id)
        assertEquals(setOf("nano_blade", "force_shield", "targeting_chip"), restored.player.inventory.ownedEquipmentIds)
    }
}
