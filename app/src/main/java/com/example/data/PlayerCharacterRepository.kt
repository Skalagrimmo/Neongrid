package com.example.data

import kotlinx.coroutines.flow.Flow

class PlayerCharacterRepository(
    private val statsDao: PlayerStatsDao,
    private val inventoryDao: InventoryItemDao,
    private val skillDao: UnlockedSkillDao
) {
    val playerStats: Flow<PlayerStatsEntity?> = statsDao.getPlayerStats()
    val inventoryItems: Flow<List<InventoryItemEntity>> = inventoryDao.getAllInventoryItems()
    val equippedItems: Flow<List<InventoryItemEntity>> = inventoryDao.getEquippedItems()
    val unlockedSkills: Flow<List<UnlockedSkillEntity>> = skillDao.getUnlockedSkills()
    val unlockedSkillIds: Flow<List<String>> = skillDao.getUnlockedSkillIds()

    suspend fun savePlayerStats(stats: PlayerStatsEntity) {
        statsDao.savePlayerStats(stats)
    }

    suspend fun updatePlayerStats(stats: PlayerStatsEntity) {
        statsDao.updatePlayerStats(stats)
    }

    suspend fun getPlayerStatsSync(): PlayerStatsEntity? {
        return statsDao.getPlayerStatsSync()
    }

    suspend fun deletePlayerStats() {
        statsDao.deletePlayerStats()
    }

    suspend fun getItemById(itemId: String): InventoryItemEntity? {
        return inventoryDao.getItemById(itemId)
    }

    suspend fun saveInventoryItem(item: InventoryItemEntity) {
        inventoryDao.insertOrUpdateItem(item)
    }

    suspend fun updateInventoryItem(item: InventoryItemEntity) {
        inventoryDao.updateItem(item)
    }

    suspend fun setEquippedStatus(itemId: String, isEquipped: Boolean) {
        val existing = inventoryDao.getItemById(itemId)
        if (existing != null) {
            inventoryDao.updateItem(existing.copy(isEquipped = isEquipped))
        }
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

    suspend fun getSkillById(skillId: String): UnlockedSkillEntity? {
        return skillDao.getSkillById(skillId)
    }

    suspend fun unlockSkill(skill: UnlockedSkillEntity) {
        skillDao.unlockSkill(skill)
    }

    suspend fun unlockSkills(skills: List<UnlockedSkillEntity>) {
        skillDao.unlockSkills(skills)
    }

    suspend fun lockSkill(skillId: String) {
        skillDao.lockSkill(skillId)
    }

    suspend fun clearUnlockedSkills() {
        skillDao.clearSkills()
    }

    suspend fun resetAllProgress() {
        statsDao.deletePlayerStats()
        inventoryDao.clearInventory()
        skillDao.clearSkills()
    }
}
