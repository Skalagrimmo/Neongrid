package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DataRepositoryTest {

    private lateinit var appDatabase: AppDatabase
    private lateinit var gameDatabase: GameDatabase
    private lateinit var repository: DataRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        appDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        gameDatabase = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repository = DataRepository(
            characterDao = appDatabase.characterDao(),
            inventoryDao = appDatabase.inventoryDao(),
            skillDao = appDatabase.skillDao(),
            saveStateDao = gameDatabase.saveStateDao(),
            statusEffectDao = appDatabase.statusEffectDao()
        )
    }

    @After
    fun tearDown() {
        appDatabase.close()
        gameDatabase.close()
    }

    @Test
    fun applyAndQueryStatusEffects() = runBlocking {
        val hackedDebuff = CharacterStatusEffectEntity(
            effectId = "hacked_1",
            name = "System Hacked",
            effectType = "HACKED",
            durationSeconds = 10f,
            remainingSeconds = 10f,
            isBuff = false,
            statModifierMultiplier = 0.5f
        )
        val stealthBuff = CharacterStatusEffectEntity(
            effectId = "stealth_1",
            name = "Active Camouflage",
            effectType = "STEALTH_BUFF",
            durationSeconds = 15f,
            remainingSeconds = 15f,
            isBuff = true,
            statModifierMultiplier = 1.8f
        )
        val overheatedDebuff = CharacterStatusEffectEntity(
            effectId = "overheated_1",
            name = "Thermal Overload",
            effectType = "OVERHEATED",
            durationSeconds = 8f,
            remainingSeconds = 8f,
            isBuff = false,
            statModifierMultiplier = 0.7f
        )

        repository.applyStatusEffects(listOf(hackedDebuff, stealthBuff, overheatedDebuff))

        val allEffects = repository.statusEffects.first()
        assertEquals(3, allEffects.size)

        val buffs = repository.activeBuffs.first()
        assertEquals(1, buffs.size)
        assertEquals("stealth_1", buffs[0].effectId)

        val debuffs = repository.activeDebuffs.first()
        assertEquals(2, debuffs.size)

        val retrievedHacked = repository.getStatusEffectById("hacked_1")
        assertNotNull(retrievedHacked)
        assertEquals("System Hacked", retrievedHacked?.name)

        repository.removeStatusEffect("hacked_1")
        val remainingDebuffs = repository.activeDebuffs.first()
        assertEquals(1, remainingDebuffs.size)
        assertEquals("overheated_1", remainingDebuffs[0].effectId)

        repository.clearStatusEffects()
        assertTrue(repository.statusEffects.first().isEmpty())
    }

    @Test
    fun saveAndGetCharacterStats() = runBlocking {
        val stats = PlayerStatsEntity(
            id = 1,
            level = 5,
            xp = 1200,
            skillPoints = 3,
            credits = 500,
            health = 90f,
            maxHealth = 100f
        )

        repository.saveCharacterStats(stats)
        val loadedStats = repository.characterStats.first()

        assertNotNull(loadedStats)
        assertEquals(5, loadedStats?.level)
        assertEquals(1200, loadedStats?.xp)
        assertEquals(500, loadedStats?.credits)
    }

    @Test
    fun deleteCharacterStats() = runBlocking {
        val stats = PlayerStatsEntity(id = 1, level = 3)
        repository.saveCharacterStats(stats)

        repository.deleteCharacterStats()
        val loadedStats = repository.characterStats.first()

        assertNull(loadedStats)
    }

    @Test
    fun saveAndQueryInventoryItems() = runBlocking {
        val item1 = InventoryItemEntity(
            itemId = "item_1",
            name = "Plasma Rifle",
            equipmentType = "WEAPON",
            description = "High energy rifle",
            quantity = 1,
            isEquipped = true
        )
        val item2 = InventoryItemEntity(
            itemId = "item_2",
            name = "Health Pack",
            equipmentType = "CONSUMABLE",
            description = "Restores 50 HP",
            quantity = 3,
            isEquipped = false
        )

        repository.saveInventoryItems(listOf(item1, item2))

        val allItems = repository.inventoryItems.first()
        assertEquals(2, allItems.size)

        val equippedItems = repository.equippedItems.first()
        assertEquals(1, equippedItems.size)
        assertEquals("item_1", equippedItems[0].itemId)
    }

    @Test
    fun deleteAndClearInventoryItems() = runBlocking {
        val item1 = InventoryItemEntity(itemId = "item_1", name = "Item 1", equipmentType = "WEAPON", description = "Desc 1")
        val item2 = InventoryItemEntity(itemId = "item_2", name = "Item 2", equipmentType = "CORE", description = "Desc 2")

        repository.saveInventoryItems(listOf(item1, item2))
        repository.deleteInventoryItem("item_1")

        var items = repository.inventoryItems.first()
        assertEquals(1, items.size)
        assertEquals("item_2", items[0].itemId)

        repository.clearInventory()
        items = repository.inventoryItems.first()
        assertTrue(items.isEmpty())
    }

    @Test
    fun unlockAndQuerySkills() = runBlocking {
        val skill1 = UnlockedSkillEntity(skillId = "stealth_1", skillName = "Ghost Protocol", characterClass = "CYBER_NINJA")
        val skill2 = UnlockedSkillEntity(skillId = "hack_1", skillName = "Overclock", characterClass = "NETRUNNER")

        repository.unlockSkills(listOf(skill1, skill2))

        val skills = repository.unlockedSkills.first()
        val skillIds = repository.unlockedSkillIds.first()

        assertEquals(2, skills.size)
        assertTrue(skillIds.contains("stealth_1"))
        assertTrue(skillIds.contains("hack_1"))

        repository.lockSkill("stealth_1")
        val updatedSkillIds = repository.unlockedSkillIds.first()
        assertEquals(1, updatedSkillIds.size)
        assertEquals("hack_1", updatedSkillIds[0])

        repository.clearSkills()
        val clearedSkills = repository.unlockedSkills.first()
        assertTrue(clearedSkills.isEmpty())
    }

    @Test
    fun saveAndGetPlayerSaveState() = runBlocking {
        val saveState = PlayerSaveState(
            id = 1,
            level = 10,
            xp = 4500,
            credits = 2500,
            questTitle = "Cyber Core Crisis"
        )

        repository.saveGameState(saveState)
        val loadedSaveState = repository.playerSaveState.first()

        assertNotNull(loadedSaveState)
        assertEquals(10, loadedSaveState?.level)
        assertEquals("Cyber Core Crisis", loadedSaveState?.questTitle)

        repository.deleteSaveState()
        val clearedSaveState = repository.playerSaveState.first()
        assertNull(clearedSaveState)
    }

    @Test
    fun syncFullPlayerProfile() = runBlocking {
        val stats = PlayerStatsEntity(id = 1, level = 8, xp = 3200)
        val items = listOf(
            InventoryItemEntity(itemId = "blade_v2", name = "Nano Blade V2", equipmentType = "WEAPON", description = "Sharper blade", isEquipped = true)
        )
        val skills = listOf(
            UnlockedSkillEntity(skillId = "blade_mastery", skillName = "Blade Mastery", characterClass = "SAMURAI")
        )

        repository.syncFullPlayerProfile(stats, items, skills)

        val currentStats = repository.characterStats.first()
        val currentItems = repository.inventoryItems.first()
        val currentSkills = repository.unlockedSkills.first()

        assertEquals(8, currentStats?.level)
        assertEquals(1, currentItems.size)
        assertEquals("blade_v2", currentItems[0].itemId)
        assertEquals(1, currentSkills.size)
        assertEquals("blade_mastery", currentSkills[0].skillId)
    }

    @Test
    fun resetAllPlayerProgress() = runBlocking {
        val stats = PlayerStatsEntity(id = 1, level = 4)
        val items = listOf(InventoryItemEntity(itemId = "item_1", name = "Test Item", equipmentType = "WEAPON", description = "Desc"))
        val skills = listOf(UnlockedSkillEntity(skillId = "skill_1", skillName = "Test Skill", characterClass = "SOLDIER"))

        repository.syncFullPlayerProfile(stats, items, skills)

        repository.resetAllPlayerProgress()

        assertNull(repository.characterStats.first())
        assertTrue(repository.inventoryItems.first().isEmpty())
        assertTrue(repository.unlockedSkills.first().isEmpty())
    }
}
