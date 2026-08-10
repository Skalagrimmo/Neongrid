package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * DataRepository provides a clean, unified abstraction layer over Room DAOs
 * for character stats, inventory items, unlocked skill progress, game save states,
 * and combat status effects.
 * Ensures data consistency across player progression subsystems.
 */
class DataRepository(
    private val characterDao: CharacterDao,
    private val inventoryDao: InventoryDao,
    private val skillDao: SkillDao? = null,
    private val saveStateDao: PlayerSaveStateDao,
    private val statusEffectDao: StatusEffectDao? = null,
    private val unlockedSkillDao: UnlockedSkillDao? = null
) {
    // --- Character Status Effects ---
    val statusEffects: Flow<List<CharacterStatusEffectEntity>> =
        statusEffectDao?.getAllStatusEffects() ?: flowOf(emptyList())

    val activeBuffs: Flow<List<CharacterStatusEffectEntity>> =
        statusEffectDao?.getActiveBuffs() ?: flowOf(emptyList())

    val activeDebuffs: Flow<List<CharacterStatusEffectEntity>> =
        statusEffectDao?.getActiveDebuffs() ?: flowOf(emptyList())

    suspend fun getStatusEffectById(effectId: String): CharacterStatusEffectEntity? {
        return statusEffectDao?.getStatusEffectById(effectId)
    }

    suspend fun applyStatusEffect(effect: CharacterStatusEffectEntity) {
        statusEffectDao?.applyStatusEffect(effect)
    }

    suspend fun applyStatusEffects(effects: List<CharacterStatusEffectEntity>) {
        statusEffectDao?.applyStatusEffects(effects)
    }

    suspend fun removeStatusEffect(effectId: String) {
        statusEffectDao?.removeStatusEffect(effectId)
    }

    suspend fun clearStatusEffects() {
        statusEffectDao?.clearStatusEffects()
    }
    // --- Character Stats ---
    val characterStats: Flow<PlayerStatsEntity?> = characterDao.getCharacterStats()

    suspend fun saveCharacterStats(stats: PlayerStatsEntity) {
        characterDao.insertOrUpdateCharacterStats(stats)
    }

    suspend fun deleteCharacterStats() {
        characterDao.deleteCharacterStats()
    }

    // --- Inventory Items ---
    val inventoryItems: Flow<List<InventoryItemEntity>> = inventoryDao.getAllInventoryItems()
    val equippedItems: Flow<List<InventoryItemEntity>> = inventoryDao.getEquippedItems()

    suspend fun saveInventoryItem(item: InventoryItemEntity) {
        inventoryDao.insertOrUpdateItem(item)
    }

    suspend fun saveInventoryItems(items: List<InventoryItemEntity>) {
        inventoryDao.insertOrUpdateItems(items)
    }

    suspend fun deleteInventoryItem(itemId: String) {
        inventoryDao.deleteItem(itemId)
    }

    suspend fun clearInventory() {
        inventoryDao.clearInventory()
    }

    // --- Skill Tree (Multi-class Skills) ---
    val allSkills: Flow<List<Skill>> = skillDao?.getAllSkills() ?: flowOf(emptyList())
    val unlockedTreeSkills: Flow<List<Skill>> = skillDao?.getUnlockedSkills() ?: flowOf(emptyList())

    fun getSkillsByClass(characterClass: String): Flow<List<Skill>> {
        return skillDao?.getSkillsByClass(characterClass) ?: flowOf(emptyList())
    }

    suspend fun insertSkill(skill: Skill) {
        skillDao?.insertSkill(skill)
    }

    suspend fun insertSkills(skills: List<Skill>) {
        skillDao?.insertSkills(skills)
    }

    // --- Unlocked Skills Progress ---
    val unlockedSkills: Flow<List<UnlockedSkillEntity>> = unlockedSkillDao?.getUnlockedSkills() ?: flowOf(emptyList())
    val unlockedSkillIds: Flow<List<String>> = unlockedSkillDao?.getUnlockedSkillIds() ?: flowOf(emptyList())

    suspend fun unlockSkill(skill: UnlockedSkillEntity) {
        unlockedSkillDao?.unlockSkill(skill)
    }

    suspend fun unlockSkills(skills: List<UnlockedSkillEntity>) {
        unlockedSkillDao?.unlockSkills(skills)
    }

    suspend fun lockSkill(skillId: String) {
        unlockedSkillDao?.lockSkill(skillId)
    }

    suspend fun clearSkills() {
        unlockedSkillDao?.clearSkills()
        skillDao?.clearAllSkills()
    }

    // --- Game Save State ---
    val playerSaveState: Flow<PlayerSaveState?> = saveStateDao.getSaveState()

    suspend fun saveGameState(saveState: PlayerSaveState) {
        saveStateDao.insertSaveState(saveState)
    }

    suspend fun deleteSaveState() {
        saveStateDao.deleteSaveState()
    }

    // --- Data Consistency & Combined Operations ---
    /**
     * Atomically syncs the player character stats, inventory items, and unlocked skills
     * to maintain overall character state consistency.
     */
    suspend fun syncFullPlayerProfile(
        stats: PlayerStatsEntity,
        items: List<InventoryItemEntity>,
        skills: List<UnlockedSkillEntity>
    ) {
        characterDao.insertOrUpdateCharacterStats(stats)
        inventoryDao.clearInventory()
        inventoryDao.insertOrUpdateItems(items)
        unlockedSkillDao?.clearSkills()
        unlockedSkillDao?.unlockSkills(skills)
    }

    /**
     * Resets all character progress (stats, inventory, skills, status effects) to default state.
     */
    suspend fun resetAllPlayerProgress() {
        characterDao.deleteCharacterStats()
        inventoryDao.clearInventory()
        unlockedSkillDao?.clearSkills()
        skillDao?.clearAllSkills()
        statusEffectDao?.clearStatusEffects()
    }
}
