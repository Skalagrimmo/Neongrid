package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerStatsDao {
    @Query("SELECT * FROM player_character_stats WHERE id = 1")
    fun getPlayerStats(): Flow<PlayerStatsEntity?>

    @Query("SELECT * FROM player_character_stats WHERE id = 1")
    suspend fun getPlayerStatsSync(): PlayerStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePlayerStats(stats: PlayerStatsEntity)

    @Update
    suspend fun updatePlayerStats(stats: PlayerStatsEntity)

    @Query("DELETE FROM player_character_stats WHERE id = 1")
    suspend fun deletePlayerStats()
}

@Dao
interface InventoryItemDao {
    @Query("SELECT * FROM player_inventory_items")
    fun getAllInventoryItems(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM player_inventory_items WHERE isEquipped = 1")
    fun getEquippedItems(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM player_inventory_items WHERE itemId = :itemId")
    suspend fun getItemById(itemId: String): InventoryItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateItem(item: InventoryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateItems(items: List<InventoryItemEntity>)

    @Update
    suspend fun updateItem(item: InventoryItemEntity)

    @Query("DELETE FROM player_inventory_items WHERE itemId = :itemId")
    suspend fun deleteItem(itemId: String)

    @Delete
    suspend fun deleteItemEntity(item: InventoryItemEntity)

    @Query("DELETE FROM player_inventory_items")
    suspend fun clearInventory()
}

@Dao
interface UnlockedSkillDao {
    @Query("SELECT * FROM unlocked_skill_progress ORDER BY unlockedAt ASC")
    fun getUnlockedSkills(): Flow<List<UnlockedSkillEntity>>

    @Query("SELECT skillId FROM unlocked_skill_progress")
    fun getUnlockedSkillIds(): Flow<List<String>>

    @Query("SELECT * FROM unlocked_skill_progress WHERE skillId = :skillId")
    suspend fun getSkillById(skillId: String): UnlockedSkillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun unlockSkill(skill: UnlockedSkillEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun unlockSkills(skills: List<UnlockedSkillEntity>)

    @Update
    suspend fun updateSkill(skill: UnlockedSkillEntity)

    @Query("DELETE FROM unlocked_skill_progress WHERE skillId = :skillId")
    suspend fun lockSkill(skillId: String)

    @Delete
    suspend fun deleteSkillEntity(skill: UnlockedSkillEntity)

    @Query("DELETE FROM unlocked_skill_progress")
    suspend fun clearSkills()
}

@Dao
interface CharacterAttributesDao {
    @Query("SELECT * FROM character_attributes WHERE characterId = 1")
    fun getCharacterAttributes(): Flow<CharacterAttributesEntity?>

    @Query("SELECT * FROM character_attributes WHERE characterId = 1")
    suspend fun getCharacterAttributesSync(): CharacterAttributesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCharacterAttributes(attributes: CharacterAttributesEntity)

    @Update
    suspend fun updateCharacterAttributes(attributes: CharacterAttributesEntity)

    @Query("DELETE FROM character_attributes WHERE characterId = 1")
    suspend fun deleteCharacterAttributes()
}

@Dao
interface SkillTreeNodeDao {
    @Query("SELECT * FROM skill_tree_nodes ORDER BY tier ASC")
    fun getAllSkillTreeNodes(): Flow<List<SkillTreeNodeEntity>>

    @Query("SELECT * FROM skill_tree_nodes WHERE nodeId = :nodeId")
    suspend fun getNodeById(nodeId: String): SkillTreeNodeEntity?

    @Query("SELECT * FROM skill_tree_nodes WHERE branch = :branch ORDER BY tier ASC")
    fun getNodesByBranch(branch: String): Flow<List<SkillTreeNodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateNode(node: SkillTreeNodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateNodes(nodes: List<SkillTreeNodeEntity>)

    @Update
    suspend fun updateNode(node: SkillTreeNodeEntity)

    @Query("DELETE FROM skill_tree_nodes WHERE nodeId = :nodeId")
    suspend fun deleteNode(nodeId: String)

    @Query("DELETE FROM skill_tree_nodes")
    suspend fun clearSkillTreeNodes()
}

